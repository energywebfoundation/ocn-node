package snc.openchargingnetwork.node.components.listeners

import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import snc.openchargingnetwork.node.models.events.AppRecipientFoundEvent
import snc.openchargingnetwork.node.components.OcpiRequestHandlerBuilder

@Component
class AppEventsListener(private val requestHandlerBuilder: OcpiRequestHandlerBuilder) {

    companion object {
        private val logger = LoggerFactory.getLogger(AppEventsListener::class.java)
    }

    @Async
    @EventListener
    fun handleAppRecipientFoundEvent(event: AppRecipientFoundEvent) {
        logger.info("Got AppRecipientFoundEvent!")
        // create a copy of the request with the new receiver headers (the app provider)
        val modifiedRequest = event.request.copy(headers = event.request.headers.copy(receiver = event.recipient))
        // update the signature: use existing sig in notary, then stash and re-sign
        // find recipient location (LOCAL/REMOTE)
        // send!
        val requestHandler = requestHandlerBuilder.build<Any>(modifiedRequest)

    }

}