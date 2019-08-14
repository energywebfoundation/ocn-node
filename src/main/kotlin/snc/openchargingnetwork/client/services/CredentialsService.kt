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