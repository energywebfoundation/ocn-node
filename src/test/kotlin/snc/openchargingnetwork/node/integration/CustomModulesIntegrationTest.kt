package snc.openchargingnetwork.node.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import snc.openchargingnetwork.node.integration.utils.*
import snc.openchargingnetwork.node.models.OcnRulesListType

class CustomModulesIntegrationTest {

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

    private fun testMessageReceived(cpo: TestCpo) {
        val response = msp.server.sendCustomModuleRequest(cpo.party)
        val json = response.jsonObject
        val data = json.getJSONArray("data").getJSONObject(0)

        assertThat(response.statusCode).isEqualTo(200)
        assertThat(json.get("status_code")).isEqualTo(1000)
        assertThat(data.get("id")).isEqualTo("999")
        assertThat(data.get("overall_free")).isEqualTo(true)
        assertThat(data.getJSONObject("evses").get("free")).isEqualTo(1)
        assertThat(data.getJSONObject("evses").get("total")).isEqualTo(2)
        assertThat(data.getJSONArray("coordinates").get(0)).isEqualTo("50.387")
        assertThat(data.getJSONArray("coordinates").get(1)).isEqualTo("1.651")
    }

    private fun testBlocked(cpo: TestCpo) {
        cpo.server.addToList(OcnRulesListType.BLACKLIST, msp.party, listOf("lite-locations"))

        val response = msp.server.sendCustomModuleRequest(cpo.party)
        val json = response.jsonObject

        assertThat(response.statusCode).isEqualTo(400)
        assertThat(json.get("status_code")).isEqualTo(2000)
        assertThat(json.get("status_message")).isEqualTo("Sender not whitelisted to request lite-locations from receiver.")
    }

    @Test
    fun `can send and receive custom module messages locally`() {
        testMessageReceived(cpo1)
    }

    @Test
    fun `can send and receive custom module messages remotely`() {
        testMessageReceived(cpo2)
    }

    @Test
    fun `can be blocked locally`() {
        testBlocked(cpo1)
    }

   @Test
   fun `can be blocked remotely`() {
       testBlocked(cpo2)
   }

}