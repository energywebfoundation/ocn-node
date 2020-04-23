package snc.openchargingnetwork.node.integration

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.*
import org.junit.jupiter.api.*
import snc.openchargingnetwork.node.integration.utils.*
import snc.openchargingnetwork.node.models.OcnRulesListType
import snc.openchargingnetwork.node.models.ocpi.*
import java.lang.Thread.sleep
import java.util.concurrent.TimeUnit


class HubClientInfoStillAliveTest {

    private lateinit var networkComponents: NetworkComponents
    private lateinit var cpo1: TestCpo
    private lateinit var cpo2: TestCpo
    private lateinit var emsp: TestMsp

    private val hubClientInfoParams = HubClientInfoParams(stillAlive = Scheduler(true))

    @BeforeEach
    fun bootStrap() {
        networkComponents = setupNetwork(hubClientInfoParams)
        cpo1 = networkComponents.cpos[0]
        cpo2 = networkComponents.cpos[1]
        emsp = networkComponents.msps[0]
    }

    @AfterEach
    fun stopTestParties() {
        stopPartyServers(networkComponents)
    }

    /**
     * Tests that a party which is unreachable will be marked as offline by the StillAliveCheck
     */
    @Test
    fun hubClientInfo_stillAlivePutsOffline() {
        assertThat(cpo1.server.hubClientInfoStatuses[emsp.party]).isEqualTo(ConnectionStatus.CONNECTED)
        assertThat(cpo2.server.hubClientInfoStatuses[emsp.party]).isEqualTo(ConnectionStatus.CONNECTED)

        emsp.server.stopServer()

        //TODO: ensure that test reliably works without factor of 2 to stillAliveRate
        await().atMost(hubClientInfoParams.stillAlive.rate * 2, TimeUnit.MILLISECONDS).until{
            val seenByCpo1 = cpo1.server.hubClientInfoStatuses[emsp.party] == ConnectionStatus.OFFLINE
            val seenByCpo2 = cpo2.server.hubClientInfoStatuses[emsp.party] == ConnectionStatus.OFFLINE
            seenByCpo1 && seenByCpo2
        }
    }

    /**
     * Tests that a party does not receive updates by non-whitelisted parties
     */
    @Test
    fun hubClientInfo_stillAliveFilteredByWhitelist() {
        assertThat(cpo1.server.hubClientInfoStatuses[emsp.party]).isEqualTo(ConnectionStatus.CONNECTED)
        assertThat(cpo1.server.hubClientInfoStatuses[cpo2.party]).isEqualTo(ConnectionStatus.CONNECTED)

        cpo1.server.addToList(OcnRulesListType.WHITELIST, emsp.party)

        cpo2.server.stopServer()
        emsp.server.stopServer()

        sleep(hubClientInfoParams.stillAlive.rate * 2)

        assertThat(cpo1.server.hubClientInfoStatuses[emsp.party]).isEqualTo(ConnectionStatus.OFFLINE)
        assertThat(cpo1.server.hubClientInfoStatuses[cpo2.party]).isEqualTo(ConnectionStatus.CONNECTED) // has not received the updated status as not in whitelist
    }

}