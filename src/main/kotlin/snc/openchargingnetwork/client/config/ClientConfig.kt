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

package snc.openchargingnetwork.client.config

import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.filter.CommonsRequestLoggingFilter
import org.web3j.crypto.Credentials
import org.web3j.crypto.WalletUtils
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import org.web3j.tx.gas.StaticGasProvider
import snc.openchargingnetwork.client.repositories.RoleRepository
import snc.openchargingnetwork.client.repositories.EndpointRepository
import snc.openchargingnetwork.client.repositories.PlatformRepository
import snc.openchargingnetwork.contracts.RegistryFacade
import java.io.FileNotFoundException

@Configuration
class Configuration(private val properties: Properties) {

    private val web3: Web3j = Web3j.build(HttpService(properties.web3.provider))

    private val credentials: Credentials = try {
        WalletUtils.loadCredentials(
                properties.web3.wallet.password,
                properties.web3.wallet.filepath)
    } catch (e: FileNotFoundException) {
        Credentials.create("0x6e91b60850c1846a1319a36e2300bd409cf22efffa6df1b8a999eb26e71baff2")
    }

    private val gasProvider = StaticGasProvider(0.toBigInteger(), 100000.toBigInteger())


    @Bean
    fun databaseInitializer(platformRepo: PlatformRepository,
                            roleRepo: RoleRepository,
                            endpointRepo: EndpointRepository) = ApplicationRunner {}

    @Bean
    fun requestLoggingFilter(): CommonsRequestLoggingFilter {
        val filter = CommonsRequestLoggingFilter()
        filter.setIncludeHeaders(true)
        filter.setIncludePayload(true)
        filter.setIncludePayload(true)
        filter.setMaxPayloadLength(10000)
        return filter
    }

    @Bean
    fun registryFacade(): RegistryFacade {
        return RegistryFacade.load(
                properties.web3.contracts.registry,
                web3,
                credentials,
                gasProvider
        )
    }

    init {
        //TODO: use proper logger
        println("Admin APIKEY = ${properties.apikey}")
        if (credentials.address == "0xe91f246ea100b7320e2d3c3ed0634ed72357f549") {
            println("WARNING: Using unsafe private key")
        }
        println("Client Ethereum address: ${credentials.address}")
    }

}
