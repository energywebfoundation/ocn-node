/*
    Copyright 2019 Share&Charge Foundation

    This file is part of Open Charging Network Node.

    Open Charging Network Node is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Open Charging Network Node is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Open Charging Network Node.  If not, see <https://www.gnu.org/licenses/>.
*/

package snc.openchargingnetwork.node.config

import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import org.web3j.tx.ClientTransactionManager
import org.web3j.tx.TransactionManager
import org.web3j.tx.gas.StaticGasProvider
import snc.openchargingnetwork.node.repositories.*
import snc.openchargingnetwork.contracts.RegistryFacade


@Configuration
open class NodeConfig(private val properties: NodeProperties) {

    private val web3: Web3j = Web3j.build(HttpService(properties.web3.provider))
    private val txManager: TransactionManager = ClientTransactionManager(web3, null)
    private val gasProvider = StaticGasProvider(0.toBigInteger(), 0.toBigInteger())

    @Bean
    fun databaseInitializer(platformRepo: PlatformRepository,
                            roleRepo: RoleRepository,
                            endpointRepo: EndpointRepository,
                            proxyResourceRepository: ProxyResourceRepository,
                            walletRepo: WalletRepository) = ApplicationRunner {}

    @Bean
    fun registryFacade(): RegistryFacade {
        return RegistryFacade.load(
                properties.web3.contracts.registry,
                web3,
                txManager,
                gasProvider
        )
    }

}