package snc.openchargingnetwork.node.integration

import org.awaitility.kotlin.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import snc.openchargingnetwork.node.integration.utils.*
import snc.openchargingnetwork.node.models.OcnAppPermission
import snc.openchargingnetwork.node.models.ocpi.InterfaceRole
import snc.openchargingnetwork.node.models.ocpi.ModuleID
import java.util.concurrent.TimeUnit

class AppInterfaceTest {

    // TODO: integration test setup could be in an inheritable class
    private lateinit var networkComponents: NetworkComponents
    private lateinit var cpo1: TestCpo
    private lateinit var cpo2: TestCpo
    private lateinit var msp: TestMsp


    @BeforeEach
    fun bootStrap() {
        networkComponents = setupNetwork(HubClientInfoParams())
        cpo1 = networkComponents.cpos[0]
        cpo2 = networkComponents.cpos[1]
        msp = networkComponents.msps.first()
    }

    @AfterEach
    fun stopTestParties() {
        stopPartyServers(networkComponents)
    }

    private fun seenByBothCpos(): Boolean {
        val message = ReceivedMessage(
                module = ModuleID.LOCATIONS,
                interfaceRole = InterfaceRole.SENDER,
                method = HttpMethod.GET,
                sender = msp.party)
        val cpo1Seen = cpo1.server.messageStore.contains(message)
        val cpo2Seen = cpo2.server.messageStore.contains(message)
        return cpo1Seen && cpo2Seen
    }

    @Test
    fun fowardsRequestToApp_Local() {
        // CPO1 is the App Provider
        cpo1.server.setAppPermissions(listOf(OcnAppPermission.FORWARD_ALL))
        // MSP is the App User
        msp.server.agreeToAppPermissions(cpo1.address)
        // MSP sends request to CPO2 which should also be forwarded to CPO1
        msp.server.getLocation(cpo2.party)

        await.atMost(2L, TimeUnit.SECONDS).until { seenByBothCpos() }

    }

    @Test
    fun fowardsRequestToApp_Remote() {
        // CPO2 is the App Provider
        cpo2.server.setAppPermissions(listOf(OcnAppPermission.FORWARD_ALL))
        // MSP is the App User
        msp.server.agreeToAppPermissions(cpo2.address)
        // MSP sends request to CPO1 which should also be forwarded to CPO2
        msp.server.getLocation(cpo1.party)

        await.atMost(2L, TimeUnit.SECONDS).until { seenByBothCpos() }
    }

}