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

@Component
class Verification(private val properties: NodeProperties) {

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
                throw ConnectException("${response.statusCode} ${response.text}")
            }
        } catch (e: ConnectException) {
            throw IllegalArgumentException("Unable to connect. Ensure $healthURL is reachable.")
        } catch (e: SSLException) {
            throw IllegalArgumentException("Experienced SSL exception. Ensure $healthURL has correct certificates.")
        }
    }

}