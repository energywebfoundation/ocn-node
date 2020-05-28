package snc.openchargingnetwork.node.services

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import snc.openchargingnetwork.node.models.ocpi.OcpiRequestVariables

@Service
class AsyncTaskService(private val registryService: RegistryService) {

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
                    logger.info("forwarding request to ${it.provider}")
                    // TODO: send to app using requestHandlerBuilder (circular import - could use an event instead)
                    // e.g. forEach
                    //          -> fire event containing original request and new recipient
                    //          -> listener picks up event
                    //          -> uses builder to create request handler
                    //          -> forwards modifiable request to new sender (must modify recipient headers and re-sign)
                }
    }

}