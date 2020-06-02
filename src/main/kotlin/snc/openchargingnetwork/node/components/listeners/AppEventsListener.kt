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

package snc.openchargingnetwork.node.components.listeners

import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import snc.openchargingnetwork.node.models.events.AppRecipientFoundEvent

@Component
class AppEventsListener {

    companion object {
        private val logger = LoggerFactory.getLogger(AppEventsListener::class.java)
    }

    @Async
    @EventListener
    fun handleAppRecipientFoundEvent(event: AppRecipientFoundEvent) {
        logger.info("Got AppRecipientFoundEvent!")
        // create a copy of the request with the new receiver headers (the app provider)
        // update the signature: use existing sig in notary, then stash and re-sign
        // find recipient location (LOCAL/REMOTE)
        // send!
        event.requestHandler.forwardAgain(event.appRecipient)
    }

}