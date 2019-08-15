/*
    Copyright 2019 Energy Web Foundation

    This file is part of Open Charging Network Client.

    Open Charging Network Client is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Open Charging Network Client is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Open Charging Network Client.  If not, see <https://www.gnu.org/licenses/>.
*/

package snc.openchargingnetwork.client.services

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.web3j.crypto.Credentials
import org.web3j.crypto.Keys
import org.web3j.crypto.Sign
import org.web3j.utils.Numeric
import snc.openchargingnetwork.client.models.entities.WalletEntity
import snc.openchargingnetwork.client.models.exceptions.OcpiHubConnectionProblemException
import snc.openchargingnetwork.client.models.ocpi.BasicRole
import snc.openchargingnetwork.client.repositories.WalletRepository
import snc.openchargingnetwork.client.tools.generatePrivateKey
import snc.openchargingnetwork.contracts.RegistryFacade
import java.nio.charset.StandardCharsets

@Service
class CredentialsService(walletRepo: WalletRepository,
                         private val registry: RegistryFacade) {

    val credentials: Credentials = try {
        val wallet = walletRepo.findByIdOrNull(1L) ?: throw IllegalStateException("No wallet found")
        Credentials.create(wallet.privateKey)
    } catch (e: IllegalStateException) {
        val privateKey = generatePrivateKey()
        val walletEntity = WalletEntity(privateKey)
        walletRepo.save(walletEntity)
        Credentials.create(privateKey)
    }

    fun signRequest(body: String): String {
        val dataToSign = body.toByteArray(StandardCharsets.UTF_8)
        val signature = Sign.signPrefixedMessage(dataToSign, credentials.ecKeyPair)
        return Numeric.cleanHexPrefix(Numeric.toHexString(signature.r)) +
                Numeric.cleanHexPrefix(Numeric.toHexString(signature.s)) +
                Numeric.cleanHexPrefix(Numeric.toHexString(signature.v))
    }

    fun verifyRequest(body: String, signature: String, sender: BasicRole) {
        val signedRequest = body.toByteArray(StandardCharsets.UTF_8)
        val cleanSignature = Numeric.cleanHexPrefix(signature)
        val r = cleanSignature.substring(0, 64)
        val s = cleanSignature.substring(64, 128)
        val v = cleanSignature.substring(128, 130)
        val signingKey = Sign.signedPrefixedMessageToKey(signedRequest, Sign.SignatureData(
                Numeric.hexStringToByteArray(v),
                Numeric.hexStringToByteArray(r),
                Numeric.hexStringToByteArray(s)))
        val signingAddress = "0x${Keys.getAddress(signingKey)}"
        val registeredClientAddress = registry.clientAddressOf(
                sender.country.toByteArray(),
                sender.id.toByteArray()).sendAsync().get()
        if (signingAddress != registeredClientAddress) {
            throw OcpiHubConnectionProblemException("Could not verify OCN-Signature of request")
        }
    }

}