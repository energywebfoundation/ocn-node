/*
    Copyright 2019-2020 eMobilify GmbH

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/

package snc.openchargingnetwork.node.services

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.web3j.crypto.Credentials
import org.web3j.crypto.Keys
import org.web3j.crypto.Sign
import org.web3j.utils.Numeric
import snc.openchargingnetwork.node.models.entities.WalletEntity
import snc.openchargingnetwork.node.models.exceptions.OcpiHubConnectionProblemException
import snc.openchargingnetwork.node.models.ocpi.BasicRole
import snc.openchargingnetwork.node.repositories.WalletRepository
import snc.openchargingnetwork.node.tools.generatePrivateKey
import snc.openchargingnetwork.contracts.RegistryFacade
import java.nio.charset.StandardCharsets

@Service
class WalletService(walletRepo: WalletRepository,
                    private val registry: RegistryFacade) {


    /**
     * Load credentials (public-private keypair) using private key stored in DB or by generating new private key
     *
     * The OCN Node's credentials are used to sign forwarded requests to other OCN Nodes.
     */
    final val credentials: Credentials

    final val address: String

    init {
        credentials = try {
            val wallet = walletRepo.findByIdOrNull(1L) ?: throw NoSuchElementException("No wallet found")
            Credentials.create(wallet.privateKey)
        } catch (e: NoSuchElementException) {
            val privateKey = generatePrivateKey()
            val walletEntity = WalletEntity(privateKey)
            walletRepo.save(walletEntity)
            Credentials.create(privateKey)
        }
        address = credentials.address
    }

    val privateKey: String get() = credentials.ecKeyPair.privateKey.toString(16)

    /**
     * Take a component of a signature (r,s,v) and convert it to a string to include as an OCN-Signature header
     * in network requests
     */
    private fun toHexStringNoPrefix(bytes: ByteArray): String {
        return Numeric.cleanHexPrefix(Numeric.toHexString(bytes))
    }


    /**
     * Reverse an OCN-Signature string to get the original r,s,v values as byte arrays
     */
    fun toByteArray(signature: String): Triple<ByteArray, ByteArray, ByteArray> {
        val cleanSignature = Numeric.cleanHexPrefix(signature)
        val r = Numeric.hexStringToByteArray(cleanSignature.substring(0, 64))
        val s = Numeric.hexStringToByteArray(cleanSignature.substring(64, 128))
        val v = Numeric.hexStringToByteArray(cleanSignature.substring(128, 130))
        return Triple(r, s, v)
    }


    /**
     * Sign an arbitrary string (used to sign the JSON body of a message sent over the network)
     */
    fun sign(request: String): String {
        val dataToSign = request.toByteArray(StandardCharsets.UTF_8)
        val signature = Sign.signPrefixedMessage(dataToSign, credentials.ecKeyPair)
        val r = toHexStringNoPrefix(signature.r)
        val s = toHexStringNoPrefix(signature.s)
        val v = toHexStringNoPrefix(signature.v)
        return r + s + v
    }


    /**
     * Verify that a request (as JSON string) was signed by the sender using the provided OCN-Signature
     */
    fun verify(request: String, signature: String, sender: BasicRole) {
        val dataToVerify = request.toByteArray(StandardCharsets.UTF_8)
        val (r, s, v) = toByteArray(signature)
        val signingKey = Sign.signedPrefixedMessageToKey(dataToVerify, Sign.SignatureData(v, r, s))
        val signingAddress = "0x${Keys.getAddress(signingKey)}"
        val registeredNodeAddress = registry.nodeAddressOf(sender.country.toByteArray(), sender.id.toByteArray()).sendAsync().get()
        if (signingAddress.toLowerCase() != registeredNodeAddress.toLowerCase()) {
            throw OcpiHubConnectionProblemException("Could not verify OCN-Signature of request")
        }
    }

}