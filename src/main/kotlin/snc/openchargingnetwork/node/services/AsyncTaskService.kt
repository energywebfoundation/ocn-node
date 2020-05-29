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

import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.AbstractAggregateRoot
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import snc.openchargingnetwork.node.models.events.AppRecipientFoundEvent
import snc.openchargingnetwork.node.models.ocpi.OcpiRequestVariables

@Service
class AsyncTaskService(private val registryService: RegistryService,
                       private val applicationEventPublisher: ApplicationEventPublisher) {

    companion object {
        private val logger = LoggerFactory.getLogger(AsyncTaskService::class.java)
    }

    /**
     * Forward request to linked apps
     * TODO: rename - e.g. findLinkedApps
     */
    @Async
    fun forwardToLinkedApps(request: OcpiRequestVariables) {
        registryService.getAgreementsByInterface(request.headers.sender, request.module, request.interfaceRole)
                .forEach {
                    logger.info("publishing forward request to ${it.provider}")
                    applicationEventPublisher.publishEvent(AppRecipientFoundEvent(it.provider, request))
                }
    }

}