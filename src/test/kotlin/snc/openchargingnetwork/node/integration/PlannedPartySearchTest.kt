package snc.openchargingnetwork.node.integration

import org.awaitility.Awaitility.await
import org.junit.jupiter.api.*
import org.web3j.crypto.Credentials
import snc.openchargingnetwork.node.integration.parties.PartyServer
import snc.openchargingnetwork.node.integration.utils.*
import snc.openchargingnetwork.node.models.ocpi.BasicRole
import snc.openchargingnetwork.node.models.ocpi.ConnectionStatus
import snc.openchargingnetwork.node.models.ocpi.Role
import java.util.concurrent.TimeUnit


class PlannedPartySearchTest {

    private lateinit var networkComponents: NetworkComponents
    private lateinit var cpo1: TestCpo
    private lateinit var cpo2: TestCpo
    private lateinit var emsp: TestMsp


    private val hubClientInfoParams = HubClientInfoParams(plannedPartySearch = Scheduler(true))

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
     * Tests that planned parties are considered in PUSH hubclientinfo requests
     */
    @Test
    fun hubClientInfo_getsPlannedParties() {
        val newParty = PartyServer(
                config = PartyDefinition(
                        nodeNumber = 0,
                        port = 8300,
                        party = BasicRole("SEV", "GB"),
                        credentials = Credentials.create("0x49b2e2b48cfc25fda1d1cbdb2197b83902142c6da502dcf1871c628ea524f11b")),
                deployedContracts = networkComponents.contracts)

        val operator = networkComponents.nodes[newParty.config.nodeNumber].definition.credentials.address
        newParty.setPartyInRegistry(operator, role = Role.EMSP, name = "SEV", url = "http://localhost:8300")

        await().atMost(hubClientInfoParams.plannedPartySearch.rate * 2, TimeUnit.MILLISECONDS).until {
            val expectedRole = BasicRole("SEV", "GB")
            val seenByCpo1 = cpo1.server.hubClientInfoStatuses[expectedRole] == ConnectionStatus.PLANNED
            val seenByCpo2 = cpo2.server.hubClientInfoStatuses[expectedRole] == ConnectionStatus.PLANNED
            val seenByEmsp = emsp.server.hubClientInfoStatuses[expectedRole] == ConnectionStatus.PLANNED
            seenByCpo1 && seenByCpo2 && seenByEmsp
        }

    }

}