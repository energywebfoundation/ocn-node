package snc.openchargingnetwork.client.services

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.web3j.crypto.Credentials
import snc.openchargingnetwork.client.models.entities.WalletEntity
import snc.openchargingnetwork.client.repositories.WalletRepository
import snc.openchargingnetwork.client.tools.generatePrivateKey

@Service
class CredentialsService(walletRepo: WalletRepository) {

    val credentials: Credentials = try {
        val wallet = walletRepo.findByIdOrNull(1L) ?: throw IllegalStateException("No wallet found")
        Credentials.create(wallet.privateKey)
    } catch (e: IllegalStateException) {
        val privateKey = generatePrivateKey()
        val walletEntity = WalletEntity(privateKey)
        walletRepo.save(walletEntity)
        Credentials.create(privateKey)
    }

}