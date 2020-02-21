package snc.openchargingnetwork.node.config

import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.web3j.crypto.Credentials
import snc.openchargingnetwork.contracts.Registry
import snc.openchargingnetwork.node.tools.urlJoin
import java.net.ConnectException
import java.net.InetAddress
import java.net.URL
import java.net.UnknownHostException
import java.util.concurrent.ExecutionException
import javax.net.ssl.SSLException

@Component
class Verification(private val properties: NodeProperties,
                   private val registry: Registry) {

    @EventListener(ApplicationReadyEvent::class)
    fun testRegistry() {
        if (properties.dev) {
            testDevRegistry()
        } else {
            testProdRegistry()
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

    private fun testDevRegistry() {
        if (properties.privateKey == null) {
            properties.privateKey = "0x1c3e5453c0f9aa74a8eb0216310b2b013f017813a648fce364bf41dbc0b37647"
        }
        val credentials = Credentials.create(properties.privateKey)
        val domain = registry.getNode(credentials.address).sendAsync().get()
        if (domain != properties.url) {
            registry.setNode(properties.url).sendAsync()
        }
    }

    private fun testProdRegistry() {
        if (properties.privateKey == null) {
            throw IllegalStateException("No private key set. Unable to verify registry configuration.")
        }
        val credentials = Credentials.create(properties.privateKey)
        val domain = registry.getNode(credentials.address).sendAsync().get()
        if (domain != properties.url) {
            throw IllegalStateException("Expected registry listing with domain \"${properties.url}\", got \"$domain\"")
        }
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