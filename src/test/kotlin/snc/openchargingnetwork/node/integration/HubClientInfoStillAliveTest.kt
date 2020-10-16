package snc.openchargingnetwork.node.integration

import org.awaitility.Awaitility.*
import org.junit.jupiter.api.*
import snc.openchargingnetwork.node.integration.utils.*
import snc.openchargingnetwork.node.models.OcnRulesListType
import snc.openchargingnetwork.node.models.ocpi.*
import java.util.concurrent.TimeUnit


class HubClientInfoStillAliveTest {

    private lateinit var networkComponents: NetworkComponents
    private lateinit var cpo1: TestCpo
    private lateinit var cpo2: TestCpo
    private lateinit var emsp: TestMsp

    private val rate = 1000L
    private val hubClientInfoParams = HubClientInfoParams(stillAlive = Scheduler(true, rate))

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
        fun mspIs(expectedStatus: ConnectionStatus): Boolean {
            // ensure both cpo1 and cpo2 have seen the status update (tests both local and remote node connections)
            val cpo1matches = cpo1.server.hubClientInfoStatuses[emsp.party] == expectedStatus
            val cpo2matches = cpo2.server.hubClientInfoStatuses[emsp.party] == expectedStatus
            return cpo1matches && cpo2matches
        }

        // need to wait for initial emsp status to be broadcast to cpo2
        await().atMost(rate, TimeUnit.SECONDS).until { mspIs(ConnectionStatus.CONNECTED) }

        emsp.server.stopServer()

        // TODO: ensure that test reliably works without factor of n to stillAliveRate
        //   - note: this is caused when the task fetches the disconnected party's Versions endpoint and must wait
        //           for a "Connection Refused" response, adding ~2 seconds to the test.
        await().atMost(rate * 4, TimeUnit.MILLISECONDS).until {
            mspIs(ConnectionStatus.OFFLINE)
        }
    }

    /**
     * Tests that a party does not receive updates by non-whitelisted parties
     */
    @Test
    fun hubClientInfo_stillAliveFilteredByWhitelist() {
        fun cpo1Has(emspStatus: ConnectionStatus, cpo2Status: ConnectionStatus): Boolean {
            val emspSeen = cpo1.server.hubClientInfoStatuses[emsp.party] == emspStatus
            val cpo2Seen = cpo1.server.hubClientInfoStatuses[cpo2.party] == cpo2Status
            return emspSeen && cpo2Seen
        }

        await().atMost(rate, TimeUnit.SECONDS).until {
            cpo1Has(emspStatus = ConnectionStatus.CONNECTED, cpo2Status = ConnectionStatus.CONNECTED)
        }

        cpo1.server.addToList(OcnRulesListType.WHITELIST, emsp.party)

        cpo2.server.stopServer()
        emsp.server.stopServer()

        // see test above for timeout reasoning
        await().atMost(rate * 4, TimeUnit.SECONDS).until {
            cpo1Has(emspStatus = ConnectionStatus.OFFLINE, cpo2Status = ConnectionStatus.CONNECTED)
        }
    }

}