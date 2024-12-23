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

import com.fasterxml.jackson.module.kotlin.readValue
import com.olisystems.ocnregistryv2_0.OcnRegistry
import org.springframework.stereotype.Service
import org.web3j.crypto.Credentials
import org.web3j.crypto.Keys
import org.web3j.crypto.Sign
import org.web3j.utils.Numeric
import snc.openchargingnetwork.node.config.NodeProperties
import snc.openchargingnetwork.node.models.exceptions.InvalidOcnSignatureException
import snc.openchargingnetwork.node.models.exceptions.OcpiHubConnectionProblemException
import snc.openchargingnetwork.node.models.ocpi.BasicRole
import snc.openchargingnetwork.node.models.ocpi.ClientInfo
import snc.openchargingnetwork.node.tools.checksum
import java.nio.charset.StandardCharsets

/**
 * Provides methods for the node's wallet; sign and verify messages sent between nodes
 */
@Service
class WalletService(private val properties: NodeProperties,
                    private val registry: OcnRegistry,
                    private val httpService: HttpService) {

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
    fun signatureStringToByteArray(signature: String): Triple<ByteArray, ByteArray, ByteArray> {
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
        val credentials = Credentials.create(properties.privateKey)
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
        val (r, s, v) = signatureStringToByteArray(signature)
        val signingKey = Sign.signedPrefixedMessageToKey(dataToVerify, Sign.SignatureData(v, r, s))
        val signingAddress = "0x${Keys.getAddress(signingKey)}"
        val (operator, _) = registry.getOperatorByOcpi(sender.country.toByteArray(), sender.id.toByteArray()).sendAsync().get()
        if (signingAddress.toLowerCase() != operator.toLowerCase()) {
            throw OcpiHubConnectionProblemException("Could not verify OCN-Signature of request")
        }
    }

    /**
     * Verify that a ClientInfo update belongs to the correct node of the party
     */
    fun verifyClientInfo(clientInfoString: String, signature: String): ClientInfo {
        val dataToVerify = clientInfoString.toByteArray(StandardCharsets.UTF_8)
        val (r, s, v) = signatureStringToByteArray(signature)
        val signingKey = Sign.signedPrefixedMessageToKey(dataToVerify, Sign.SignatureData(v, r, s))
        val signingAddress = "0x${Keys.getAddress(signingKey)}"

        val clientInfo: ClientInfo = httpService.mapper.readValue(clientInfoString)

        // validate party registered with signer
        val countryCode = clientInfo.countryCode.toByteArray()
        val partyID = clientInfo.partyID.toByteArray()

        val operator = registry.getPartyDetailsByOcpi(countryCode, partyID).sendAsync().get().component6()

        if (operator.checksum() != signingAddress.checksum()) {
            throw InvalidOcnSignatureException("Invalid OCN-Signature header. Client registered with operator $operator but update signed by $signingAddress.")
        }
        return clientInfo
    }

}