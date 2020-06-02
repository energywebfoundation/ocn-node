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
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import snc.openchargingnetwork.node.components.OcpiRequestHandler
import snc.openchargingnetwork.node.models.events.AppRecipientFoundEvent

@Service
class AsyncTaskService(private val registryService: RegistryService,
                       private val applicationEventPublisher: ApplicationEventPublisher) {

    companion object {
        private val logger = LoggerFactory.getLogger(AsyncTaskService::class.java)
    }

    /**
     * Finds all apps, linked to a sender, with permissions that grant them access to a given request type.
     * Once apps have been found, triggers an AppRecipientFoundEvent.
     */
    @Async
    fun findLinkedApps(requestHandler: OcpiRequestHandler<*>) {
        val request = requestHandler.request
        registryService.getAgreementsByInterface(request.headers.sender, request.module, request.interfaceRole)
                .forEach {
                    logger.info("publishing forward request to ${it.provider}")
                    applicationEventPublisher.publishEvent(AppRecipientFoundEvent(requestHandler, it.provider))
                }
    }

}