package snc.openchargingnetwork.node.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import snc.openchargingnetwork.node.integration.utils.*
import snc.openchargingnetwork.node.models.OcnRulesListType
import java.util.stream.Stream

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

    private fun testMessagePostWithBody(cpo: TestCpo, requestBody: String) {
        val response = msp.server.postCustomModuleRequestWithBody(cpo.party, requestBody)
        val json = response.jsonObject
        assertThat(response.statusCode).isEqualTo(200)
        assertThat(json.get("status_code")).isEqualTo(1000)

        // Expecting that request body is echoed back
        assertThat(json.get("data")).isEqualTo(requestBody)
    }

    private fun testMessagePostWithoutBody(cpo: TestCpo) {
        val response = msp.server.postCustomModuleRequestWithoutBody(cpo.party)
        val json = response.jsonObject
        assertThat(response.statusCode).isEqualTo(200)
        assertThat(json.get("status_code")).isEqualTo(1000)
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

    private fun requestBodies(): Stream<String> {
        return Stream.of(
                "simple string",
                """[{"hello":"world"},{"beautiful":"day"}]""",
                """{"key1":"value1"}""",
                """{"key1":["value1","value2"]}""")
    }

    @Test
    fun `can send and receive custom module messages locally`() {
        testMessageReceived(cpo1)
    }

    @Test
    fun `can send and receive custom module messages remotely`() {
        testMessageReceived(cpo2)
    }

    @ParameterizedTest
    @MethodSource("requestBodies")
    fun `can post custom module messages locally`(body: String) {
        testMessagePostWithBody(cpo1, body)
        testMessagePostWithoutBody(cpo1)
    }

    @ParameterizedTest
    @MethodSource("requestBodies")
    fun `can post custom module messages remotely`(body: String) {
        testMessagePostWithBody(cpo2, body)
        testMessagePostWithoutBody(cpo2)
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