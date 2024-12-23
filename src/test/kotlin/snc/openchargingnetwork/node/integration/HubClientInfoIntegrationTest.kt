package snc.openchargingnetwork.node.integration

import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.*
import org.web3j.crypto.Credentials
import snc.openchargingnetwork.node.integration.parties.PartyServer
import snc.openchargingnetwork.node.integration.utils.*
import snc.openchargingnetwork.node.models.OcnRulesListType
import snc.openchargingnetwork.node.models.ocpi.*
import java.lang.Thread.sleep
import java.util.concurrent.TimeUnit


class HubClientInfoIntegrationTest {

    private lateinit var networkComponents: NetworkComponents
    private lateinit var cpo1: TestCpo
    private lateinit var cpo2: TestCpo
    private lateinit var msp: TestMsp

    private val hubClientInfoParams = HubClientInfoParams(plannedPartySearch = Scheduler(true))

    @BeforeEach
    fun bootStrap() {
        networkComponents = setupNetwork(hubClientInfoParams)
        val cpos = networkComponents.cpos
        cpo1 = cpos[0]
        cpo2 = cpos[1]
        msp = networkComponents.msps.first()
    }

    @AfterEach
    fun stopTestParties() {
        stopPartyServers(networkComponents)
    }

    /**
     * Tests that new parties and deleted parties trigger hubClientInfo notifications
     */
    @Test
    fun hubClientInfo_partyRegisteredNotification() {
        val newCpoDefinition = PartyDefinition(
                nodeNumber = 1,
                port = 8301,
                party = BasicRole("CPC", "CH"),
                credentials = Credentials.create("0x82d052c865f5763aad42add438569276c00d3d88a2d062d36b2bae914d58b8c8"))
        val newCpo = setUpCpo(
                newCpoDefinition,
                networkComponents.nodes.first { d -> d.definition.nodeNumber == newCpoDefinition.nodeNumber }.definition,
                networkComponents.contracts)

        // The assumption is that cpo1 is also connected to node1
        await().atMost(2, TimeUnit.SECONDS).until{cpo1.server.hubClientInfoStatuses.containsKey(newCpoDefinition.party)}
        assertThat(cpo1.server.hubClientInfoStatuses[newCpoDefinition.party]).isEqualTo(ConnectionStatus.CONNECTED)

        newCpo.server.deleteCredentials()
        await().atMost(2, TimeUnit.SECONDS).until{cpo1.server.hubClientInfoStatuses[newCpoDefinition.party] == ConnectionStatus.SUSPENDED}
    }

    /**
     * Tests that HubClientInfo functionality can be disabled
     * (MSP server is stopped and no OFFLINE update is broadcast)
     */
    @Test
    fun hubClientInfo_stillAliveCanBeDisabled() {
        fun mspIsConnected(): Boolean {
            val cpo1notified = cpo1.server.hubClientInfoStatuses[msp.party] == ConnectionStatus.CONNECTED
            val cpo2notified = cpo2.server.hubClientInfoStatuses[msp.party] == ConnectionStatus.CONNECTED
            return cpo1notified && cpo2notified
        }

        await().atMost(2, TimeUnit.SECONDS).until { mspIsConnected() }
        msp.server.stopServer()
        sleep(hubClientInfoParams.stillAlive.rate * 2)
        await().atMost(2, TimeUnit.SECONDS).until { mspIsConnected() } // should still be connected as still alive disabled
    }

    /**
     * Tests that a party's client info is updated when sending or receiving a request.
     * StillAliveCheck should be disabled for this test or updated time may change unexpectedly
     */
    @Test
    fun hubClientInfo_clientInfoUpdatedByRequest() {
        val clientInfoBeforeRequest = getClientInfo()

        // make arbitrary request in order to update clientInfo.lastUpdated
        msp.server.getLocation(cpo1.party)

        val clientInfoAfterRequest = getClientInfo()

        val clientInfoFilter = { party: BasicRole ->
            { ci: ClientInfo -> BasicRole(ci.partyID, ci.countryCode) == party }
        }
        val mspClientInfoBeforeRequest = clientInfoBeforeRequest.first { ci -> clientInfoFilter(msp.party)(ci) }
        val cpo1ClientInfoBeforeRequest = clientInfoBeforeRequest.first { ci -> clientInfoFilter(cpo1.party)(ci) }
        val cpo2ClientInfoBeforeRequest = clientInfoBeforeRequest.first { ci -> clientInfoFilter(cpo2.party)(ci) }

        val mspClientInfoAfterRequest = clientInfoAfterRequest.first { ci -> clientInfoFilter(msp.party)(ci) }
        val cpo1ClientInfoAfterRequest = clientInfoAfterRequest.first { ci -> clientInfoFilter(cpo1.party)(ci) }
        val cpo2ClientInfoAfterRequest = clientInfoAfterRequest.first { ci -> clientInfoFilter(cpo2.party)(ci) }

        // the MSP and CPO1 should be updated as they were involved in the request
        assertThat(mspClientInfoBeforeRequest.lastUpdated).isLessThan(mspClientInfoAfterRequest.lastUpdated)
        assertThat(cpo1ClientInfoBeforeRequest.lastUpdated).isLessThan(cpo1ClientInfoAfterRequest.lastUpdated)
        assertThat(cpo2ClientInfoBeforeRequest.lastUpdated).isEqualTo(cpo2ClientInfoAfterRequest.lastUpdated)
    }

    @Test
    fun hubClientInfo_returnsPlannedParties() {
        val newParty = PartyServer(
                config = PartyDefinition(
                        nodeNumber = 0,
                        port = 8302,
                        party = BasicRole("TON", "GB"),
                        credentials = Credentials.create("0x49b2e2b48cfc25fda1d1cbdb2197b83902142c6da502dcf1871c628ea524f11b")),
                deployedContracts = networkComponents.contracts)
        val nodeAddress = networkComponents.nodes[newParty.config.nodeNumber].definition.credentials.address
        newParty.setPartyInRegistry(nodeAddress, Role.EMSP, "name", "url")

        sleep(hubClientInfoParams.plannedPartySearch.rate * 2)

        val clientInfo = getClientInfo()
        val expectedPlannedRole = clientInfo.find { it.countryCode == "GB" && it.partyID == "TON" }
        assertThat(expectedPlannedRole).isNotNull
        assertThat(expectedPlannedRole!!.role).isEqualTo(Role.EMSP)
        assertThat(expectedPlannedRole.status).isEqualTo(ConnectionStatus.PLANNED)
    }

    @Test
    fun hubClientInfo_GetListFilteredByWhitelist() {
        val clientInfoBefore = getClientInfo().map { BasicRole(id = it.partyID, country = it.countryCode) }

        assertThat(clientInfoBefore.find { it == cpo1.party }).isNotNull
        assertThat(clientInfoBefore.find { it == cpo2.party }).isNotNull

        msp.server.addToList(OcnRulesListType.BLACKLIST, cpo1.party)

        val clientInfoAfter = getClientInfo().map { BasicRole(id = it.partyID, country = it.countryCode) }

        assertThat(clientInfoAfter.find { it == cpo1.party }).isNull()
        assertThat(clientInfoAfter.find { it == cpo2.party }).isNotNull
    }

    private fun getClientInfo(): Array<ClientInfo> {
        val nodeRole = BasicRole(id = "OCN", country = "DE")
        val response = msp.server.getHubClientInfoList(nodeRole)
        assertThat(response.statusCode).isEqualTo(200)

        val json = response.jsonObject
        assertThat(json.getInt("status_code")).isEqualTo(1000)

        return objectMapper.readValue(json.getJSONArray("data").toString())
    }
}