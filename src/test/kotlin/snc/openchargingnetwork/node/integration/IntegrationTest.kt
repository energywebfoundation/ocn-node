package snc.openchargingnetwork.node.integration

import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import shareandcharge.openchargingnetwork.notary.Notary
import shareandcharge.openchargingnetwork.notary.SignableHeaders
import shareandcharge.openchargingnetwork.notary.ValuesToSign
import snc.openchargingnetwork.node.data.exampleCDR
import snc.openchargingnetwork.node.data.exampleLocation1
import snc.openchargingnetwork.node.integration.parties.MspServer
import snc.openchargingnetwork.node.integration.utils.*
import snc.openchargingnetwork.node.models.ocpi.*
import snc.openchargingnetwork.node.tools.checksum
import snc.openchargingnetwork.node.tools.extractNextLink
import java.lang.Thread.sleep
import java.util.stream.Stream


class IntegrationTest {

    private lateinit var networkComponents: NetworkComponents
    private lateinit var mspServer: MspServer
    private lateinit var msp: TestMsp
    private lateinit var cpos: List<TestCpo>

    private val hubClientInfoParams = HubClientInfoParams()

    private fun cpoSources(): Stream<Arguments> {
        return Stream.of(
                Arguments.of(cpos[0]),
                Arguments.of(cpos[1]))
    }

    @BeforeAll
    fun bootStrap() {
        networkComponents = setupNetwork(hubClientInfoParams)
        msp = networkComponents.msps.first()
        mspServer = msp.server
        cpos = networkComponents.cpos
    }

    @AfterAll
    fun stopTestParties() {
        stopPartyServers(networkComponents)
    }

    @Test
    fun healthy() {
        val response1 = khttp.get("http://localhost:8080/health")
        val response2 = khttp.get("http://localhost:8081/health")

        assertThat(response1.statusCode).isEqualTo(200)
        assertThat(response1.text).isEqualTo("OK")
        assertThat(response2.statusCode).isEqualTo(200)
        assertThat(response2.text).isEqualTo("OK")
    }

    @Test
    fun registry() {
        val response1 = khttp.get("http://localhost:8080/ocn/registry/node/de/msp")
        val response2 = khttp.get("http://localhost:8081/ocn/registry/node/de/msp")

        assertThat(response1.statusCode).isEqualTo(200)
        assertThat(response2.statusCode).isEqualTo(200)

        val expectedUrl = "http://localhost:8080"
        val expectedAddress = "0xf17f52151ebef6c7334fad080c5704d77216b732".checksum()

        assertThat(response1.jsonObject.getString("url")).isEqualTo(expectedUrl)
        assertThat(response1.jsonObject.getString("address").checksum()).isEqualTo(expectedAddress)
        assertThat(response2.jsonObject.getString("url")).isEqualTo(expectedUrl)
        assertThat(response2.jsonObject.getString("address").checksum()).isEqualTo(expectedAddress)
    }

    @ParameterizedTest
    @MethodSource("cpoSources")
    fun basic_request(cpo: TestCpo) {
        // send request to cpo
        val response = mspServer.getLocation(cpo.party)
        assertThat(response.statusCode).isEqualTo(200)

        // check response data
        val json = response.jsonObject
        assertThat(json.getInt("status_code")).isEqualTo(1000)

        val data: Location = objectMapper.readValue(json.getJSONObject("data").toString())
        assertThat(data).isEqualTo(exampleLocation1)

        // check signature
        val notary = Notary.deserialize(json.getString("ocn_signature"))
        val body: Map<String, Any?> = objectMapper.readValue(json.toString())
        val signedValues = ValuesToSign(body = body)
        val verifyResult = notary.verify(signedValues)

        assertThat(verifyResult.isValid).isEqualTo(true)
        assertThat(notary.signatory.checksum()).isEqualTo(cpo.address.checksum())
    }

    @ParameterizedTest
    @MethodSource("cpoSources")
    fun paginated_request(cpo: TestCpo) {
        // send original request to CPO
        val response = mspServer.getLocationList(cpo.party)
        assertThat(response.statusCode).isEqualTo(200)

        // check paginated headers
        assertThat(response.headers["x-limit"]).isEqualTo("5")
        assertThat(response.headers["x-total-count"]).isEqualTo("50")

        // check link header has been modified by sender's node
        val pattern = "<http://localhost:8080/ocpi/sender/2.2/locations/page/\\d+>; rel=\"next\"".toPattern()
        assertThat(response.headers["link"]).containsPattern(pattern)

        // check response data
        val json = response.jsonObject
        assertThat(json.getInt("status_code")).isEqualTo(1000)
        assertThat(json.getJSONArray("data").length()).isEqualTo(4)

        // re-create signed values for signature verification
        val valuesToSign = ValuesToSign(
                headers = SignableHeaders(
                        limit = response.headers["x-limit"],
                        totalCount = response.headers["x-total-count"],
                        link = response.headers["link"]),
                body = objectMapper.readValue<Map<String, Any?>>(json.toString()))

        val notary = Notary.deserialize(json.getString("ocn_signature"))

        // check signature is valid (has not been modified in transit)
        val verifyResult = notary.verify(valuesToSign)
        assertThat(verifyResult.isValid).isEqualTo(true)

        // check main signatory is the sender's node (rewrote the link header)
        assertThat(notary.signatory.checksum()).isEqualTo("0xf17f52151EbEF6C7334FAD080c5704D77216b732")

        // check rewrite contains original signatory (cpo)
        assertThat(notary.rewrites.size).isEqualTo(1)
        assertThat(notary.rewrites[0].signatory.checksum()).isEqualTo(cpo.address.checksum())

        // send request to next link
        val next = response.headers.getValue("link").extractNextLink()

        val nextResponse = mspServer.getNextLink(cpo.party, next!!)
        val nextJson = nextResponse.jsonObject

        // check response data
        assertThat(nextJson.getInt("status_code")).isEqualTo(1000)
        assertThat(nextJson.getJSONArray("data").length()).isEqualTo(5)
    }

    @ParameterizedTest
    @MethodSource("cpoSources")
    fun restful_post_request(cpo: TestCpo) {
        // send cdr to msp
        val response = cpo.server.sendCdr(BasicRole("MSP", "DE"))
        assertThat(response.statusCode).isEqualTo(200)

        // check location header
        val pattern = "${cpo.server.node}/ocpi/receiver/2.2/cdrs/\\d".toPattern()
        assertThat(response.headers["location"]).containsPattern(pattern)

        // check response body
        val json = response.jsonObject
        assertThat(json.getInt("status_code")).isEqualTo(1000)

        val sig = Notary.deserialize(json.getString("ocn_signature"))

        val signedValues = ValuesToSign(
                headers = SignableHeaders(location = response.headers["location"]),
                body = objectMapper.readValue<Map<String, Any?>>(json.toString()))

        // check signed by correct entity (sender's node is the modifier of the location header)
        assertThat(sig.signatory.checksum()).isEqualTo(cpo.operator.checksum())
        assertThat(sig.rewrites.size).isEqualTo(1)
        assertThat(sig.rewrites[0].signatory.checksum()).isEqualTo(msp.address.checksum())

        // verify signature matches response
        val verifyResult = sig.verify(signedValues)
        assertThat(verifyResult.isValid).isEqualTo(true)

        // verify cdr retrieved successfully
        val getResponse = cpo.server.getCdr(BasicRole("MSP", "DE"), response.headers.getValue("location"))
        assertThat(getResponse.statusCode).isEqualTo(200)

        val getJson = getResponse.jsonObject
        assertThat(getJson.getInt("status_code")).isEqualTo(1000)

        val getData: CDR = objectMapper.readValue(getJson.getJSONObject("data").toString())
        assertThat(getData).isEqualTo(exampleCDR)
    }

    @ParameterizedTest
    @MethodSource("cpoSources")
    fun async_request(cpo: TestCpo) {
        // send request to cpo
        val response = mspServer.sendStartSession(cpo.party)
        assertThat(response.statusCode).isEqualTo(200)

        // check body
        val json = response.jsonObject
        assertThat(json.getInt("status_code")).isEqualTo(1000)
        assertThat(json.getJSONObject("data").getString("result")).isEqualTo("ACCEPTED")
        assertThat(json.getJSONObject("data").getInt("timeout")).isEqualTo(10)

        // check signature
        val sig = Notary.deserialize(json.getString("ocn_signature"))
        val signedValues = ValuesToSign(body = objectMapper.readValue<Map<String, Any?>>(json.toString()))
        val verifyResult = sig.verify(signedValues)

        assertThat(verifyResult.isValid).isEqualTo(true)
        assertThat(sig.signatory.checksum()).isEqualTo(cpo.address.checksum())

        // TODO: use awaitility
        // check async result
        val timeout = json.getJSONObject("data").getInt("timeout")
        for (i in 0..timeout) {

            if (i == timeout) {
                throw Exception("Failed to get async result inside timeout window")
            }

            if (mspServer.asyncCommandsResponse != null) {
                assertThat(mspServer.asyncCommandsResponse).isEqualTo(CommandResult(result = CommandResultType.ACCEPTED))
                break
            }

            sleep(1000)
        }
    }

}