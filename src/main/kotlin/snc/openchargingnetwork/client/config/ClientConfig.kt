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
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import org.web3j.tx.ClientTransactionManager
import org.web3j.tx.TransactionManager
import org.web3j.tx.gas.StaticGasProvider
import snc.openchargingnetwork.client.repositories.RoleRepository
import snc.openchargingnetwork.client.repositories.EndpointRepository
import snc.openchargingnetwork.client.repositories.PlatformRepository
import snc.openchargingnetwork.client.tools.generateUUIDv4Token
import snc.openchargingnetwork.contracts.RegistryFacade

@Configuration
class Configuration(private val properties: Properties) {

    private val web3: Web3j = Web3j.build(HttpService(properties.web3.provider))
    private val txManager: TransactionManager = ClientTransactionManager(web3, null)
    private val gasProvider = StaticGasProvider(0.toBigInteger(), 0.toBigInteger())

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
                txManager,
                gasProvider
        )
    }

    init {
        if (properties.apikey.isNullOrEmpty()) {
            properties.apikey = generateUUIDv4Token()
        }
        println("\n===================================================\n" +
                "ADMIN_APIKEY = ${properties.apikey}\n" +
                "URL = ${properties.url}" +
                "\n===================================================\n")
    }

}
