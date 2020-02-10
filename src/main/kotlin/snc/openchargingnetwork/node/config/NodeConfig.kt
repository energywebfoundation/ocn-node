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