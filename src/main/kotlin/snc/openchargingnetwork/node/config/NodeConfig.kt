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

import com.olisystems.ocnregistryv2_0.OcnRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.config.IntervalTask
import org.springframework.web.client.RestTemplate
import org.web3j.protocol.Web3j
import org.web3j.tx.ClientTransactionManager
import org.web3j.tx.TransactionManager
import org.web3j.tx.gas.StaticGasProvider
import snc.openchargingnetwork.node.repositories.*
import snc.openchargingnetwork.node.scheduledTasks.HubClientInfoStillAliveCheck
import snc.openchargingnetwork.node.scheduledTasks.PlannedPartySearch
import org.web3j.protocol.http.HttpService as Web3jHttpService
import snc.openchargingnetwork.node.services.HttpService as OcnHttpService


@Configuration
class NodeConfig(private val properties: NodeProperties) {

    private val web3: Web3j = Web3j.build(Web3jHttpService(properties.web3.provider))
    private val txManager: TransactionManager = ClientTransactionManager(web3, null)
    private val gasProvider = StaticGasProvider(0.toBigInteger(), 0.toBigInteger())

    @Bean
    fun databaseInitializer(platformRepo: PlatformRepository,
                            roleRepo: RoleRepository,
                            endpointRepo: EndpointRepository,
                            proxyResourceRepository: ProxyResourceRepository) = ApplicationRunner {}



    @Bean
    fun ocnRegistry(): OcnRegistry {
        return OcnRegistry.load(
            properties.web3.contracts.ocnRegistry,
            web3,
            txManager,
            gasProvider)
    }

    @Bean
    fun coroutineScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    @Bean
    fun newScheduledTasks(registry: OcnRegistry,
                          httpService: OcnHttpService,
                          platformRepo: PlatformRepository,
                          roleRepo: RoleRepository,
                          networkClientInfoRepo: NetworkClientInfoRepository): List<IntervalTask> {

        val taskList = mutableListOf<IntervalTask>()
        val hasPrivateKey = properties.privateKey !== null

        if (properties.stillAliveEnabled && hasPrivateKey) {
            val stillAliveTask = HubClientInfoStillAliveCheck(httpService, platformRepo, properties)
            taskList.add(IntervalTask(stillAliveTask, properties.stillAliveRate.toLong()))
        }

        //
        if (properties.plannedPartySearchEnabled && hasPrivateKey) {
            val plannedPartyTask = PlannedPartySearch(registry, roleRepo, networkClientInfoRepo, properties)
            taskList.add(IntervalTask(plannedPartyTask, properties.plannedPartySearchRate.toLong()))
        }
        return taskList.toList()
    }



//    // modify the default task executor (runs async tasks, not to be confused with scheduled tasks)
//    @Bean
//    fun taskExecutor(): TaskExecutor {
//        val taskExecutor = ThreadPoolTaskExecutor()
//        taskExecutor.corePoolSize = 100
//        taskExecutor.setThreadNamePrefix("worker-")
//        return taskExecutor
//    }

}

