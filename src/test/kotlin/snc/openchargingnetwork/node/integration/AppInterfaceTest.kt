package snc.openchargingnetwork.node.integration

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import snc.openchargingnetwork.node.integration.utils.*
import snc.openchargingnetwork.node.models.OcnAppPermission

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

    @Test
    fun fowardsRequestToApp() {
        cpo2.server.setAppPermissions(listOf(OcnAppPermission.FORWARD_ALL))
        msp.server.agreeToAppPermissions(cpo2.address)
        msp.server.getLocation(cpo1.party)
    }

}