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

import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import snc.openchargingnetwork.node.tools.urlJoin
import java.net.ConnectException
import java.net.InetAddress
import java.net.URL
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import org.slf4j.LoggerFactory

@Component
class Verification(private val properties: NodeProperties) {

	companion object {
		private val logger = LoggerFactory.getLogger(Verification::class.java)
    }
    @EventListener(ApplicationReadyEvent::class)
    fun testRegistry() {
        if (properties.privateKey == null) {
            if (properties.dev) {
                properties.privateKey = "0x1c3e5453c0f9aa74a8eb0216310b2b013f017813a648fce364bf41dbc0b37647"
            } else {
                throw IllegalStateException("No private key set. Unable to verify registry configuration.")
            }
        }
    }

    @EventListener(ApplicationReadyEvent::class)
    fun testPublicURL() {
        val url = URL(this.properties.url)

        val inetAddress = try {
            InetAddress.getByName(url.host)
        } catch (e: UnknownHostException) {
            throw IllegalArgumentException("Provided host \"${url.host}\" unknown.")
        }

        if (!this.properties.dev) {
            if (url.protocol != "https") {
                throw IllegalArgumentException("Must use https in prod mode. Provided url has protocol \"${url.protocol}\".")
            }
            if (inetAddress.isAnyLocalAddress || inetAddress.isLoopbackAddress) {
                throw IllegalArgumentException("Must use publicly accessible url in prod mode. Provided url has local/loopback host address \"${inetAddress.hostAddress}\".")
            }
        }

        this.testHealth()
    }

    private fun testHealth() {
        val healthURL = urlJoin(this.properties.url, "/health")

        try {
            val response = khttp.get(healthURL)
            if (response.statusCode != 200) {
				logger.warn("Received status code ${response.statusCode} from $healthURL application may not be healthy.")
            }
        } catch (e: ConnectException) {
            throw IllegalArgumentException("Unable to connect. Ensure $healthURL is reachable.")
        } catch (e: SSLException) {
            throw IllegalArgumentException("Experienced SSL exception. Ensure $healthURL has correct certificates.")
        }
    }

}
