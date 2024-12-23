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
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import snc.openchargingnetwork.node.components.OcpiRequestHandler
import snc.openchargingnetwork.node.config.NodeProperties

@Service
class AsyncTaskService(private val registryService: RegistryService, private val properties: NodeProperties) {

    companion object {
        private val logger = LoggerFactory.getLogger(AsyncTaskService::class.java)
    }

    /**
     * Finds all services, linked to a sender, with permissions that grant them access to a given request type.
     * Once services have been found, sends via provided request handler.
     * TODO new ocn: forward request o all parties of a given oposite role
     */
    @Async
    fun forwardOcpiRequestToLinkedServices(requestHandler: OcpiRequestHandler<*>, fromLocalPlatform: Boolean = true) {
        // we only want to forward to services if the module is one of the default OCPI modules,
        // and only if the sender is a local platform (to avoid repeat forwarding on the recipient node)
        // and also forward if service interface option is enabled
//        val isDefaultModule = requestHandler.request.module != ModuleID.CUSTOM
//
//        if (isDefaultModule && fromLocalPlatform && properties.serviceInterfaceEnabled) {
//            val request = requestHandler.request
//            registryService.getAgreementsByInterface(request.headers.sender, request.module, request.interfaceRole)
//                    .forEach {
//                        try {
//                            requestHandler.forwardAgain(it.provider)
//                        } catch (e: Exception) {
//                            // fire and forget
//                            logger.warn("Error forwarding request to service ${it.provider}: ${e.message}")
//                        }
//                    }
//        }
    }

}