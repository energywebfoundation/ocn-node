package snc.openchargingnetwork.node.integration

import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.web3j.crypto.Credentials
import shareandcharge.openchargingnetwork.notary.Notary
import shareandcharge.openchargingnetwork.notary.SignableHeaders
import shareandcharge.openchargingnetwork.notary.ValuesToSign
import snc.openchargingnetwork.node.data.exampleLocation1
import snc.openchargingnetwork.node.integration.parties.CpoServer
import snc.openchargingnetwork.node.integration.parties.MspServer
import snc.openchargingnetwork.node.models.ocpi.BasicRole
import snc.openchargingnetwork.node.models.ocpi.Location
import snc.openchargingnetwork.node.tools.extractNextLink
import java.util.regex.Pattern


class IntegrationTest {

    private lateinit var mspServer: MspServer

    private val recipients = listOf(
            CpoTestCase(
                    party = BasicRole("CPA", "CH"),
                    address = "0x821aEa9a577a9b44299B9c15c88cf3087F3b5544"),
            CpoTestCase(
                    party = BasicRole("CPB", "CH"),
                    address = "0x0d1d4e623D10F9FBA5Db95830F7d3839406C6AF2"))

    @BeforeAll
    fun bootStrap() {
        // REGISTRY CONTRACT
        val registry = deployRegistry()

        // NODE 1 = http://localhost:8080
        val node1 = Credentials.create("0xae6ae8e5ccbfb04590405997ee2d52d2b330726137b875053c36d94e974d162f")
        setUpNode(registry.contractAddress, node1, 8080)

        // NODE 2 = http://localhost:8081
        val node2 = Credentials.create("0x0dbbe8e4ae425a6d2687f1a7e3ba17bc98c673636790f1b8ad91193c05875ef1")
        setUpNode(registry.contractAddress, node2, 8081)

        // CPO 1
        val cpo1 = Credentials.create("0xc88b703fb08cbea894b6aeff5a544fb92e78a18e19814cd85da83b71f772aa6c")
        val cpoServer1 = CpoServer(cpo1, BasicRole("CPA", "CH"), 8100)
        cpoServer1.setPartyInRegistry(registry.contractAddress, node1.address)
        cpoServer1.registerCredentials()

        // CPO 2
        val cpo2 = Credentials.create("0x388c684f0ba1ef5017716adb5d21a053ea8e90277d0868337519f97bede61418")
        val cpoServer2 = CpoServer(cpo2, BasicRole("CPB", "CH"), 8101)
        cpoServer2.setPartyInRegistry(registry.contractAddress, node2.address)
        cpoServer2.registerCredentials()

        // MSP
        val msp = Credentials.create("0x659cbb0e2411a44db63778987b1e22153c086a95eb6b18bdf89de078917abc63")
        mspServer = MspServer(msp, BasicRole("MSP", "DE"), 8200)
        mspServer.setPartyInRegistry(registry.contractAddress, node1.address)
        mspServer.registerCredentials()
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

    // TODO: proper parameterized tests
    @Test
    fun basic_request() {
        for (recipient in recipients) {
            val response = mspServer.getLocation(recipient.party)
            val json = response.jsonObject

            assertThat(response.statusCode).isEqualTo(200)
            assertThat(json.getInt("status_code")).isEqualTo(1000)

            val data: Location = objectMapper.readValue(json.getJSONObject("data").toString())
            assertThat(data).isEqualTo(exampleLocation1)

            val notary = Notary.deserialize(json.getString("ocn_signature"))
            val body: Map<String, Any?> = objectMapper.readValue(json.toString())
            val signedValues = ValuesToSign(body = body)
            val verifyResult = notary.verify(signedValues)
            assertThat(verifyResult.isValid).isEqualTo(true)
            assertThat(notary.signatory.checksum()).isEqualTo(recipient.address)
        }
    }

    @Test
    fun paginated_local() {
        for (recipient in recipients) {
            val response = mspServer.getLocationList(recipient.party)

            assertThat(response.headers["x-limit"]).isEqualTo("5")
            assertThat(response.headers["x-total-count"]).isEqualTo("50")

            val pattern = "<http://localhost:8080/ocpi/sender/2.2/locations/page/\\d+>; rel=\"next\"".toPattern()
            assertThat(response.headers["link"]).containsPattern(pattern)

            val json = response.jsonObject
            assertThat(json.getInt("status_code")).isEqualTo(1000)
            assertThat(json.getJSONArray("data").length()).isEqualTo(4)

            val valuesToSign = ValuesToSign(
                    headers = SignableHeaders(
                            limit = response.headers["x-limit"],
                            totalCount = response.headers["x-total-count"],
                            link = response.headers["link"]),
                    body = objectMapper.readValue<Map<String, Any?>>(json.toString()))

            val notary = Notary.deserialize(json.getString("ocn_signature"))

            val verifyResult = notary.verify(valuesToSign)
            assertThat(verifyResult.isValid).isEqualTo(true)

            assertThat(notary.signatory.checksum()).isEqualTo("0xf17f52151EbEF6C7334FAD080c5704D77216b732")
            assertThat(notary.rewrites.size).isEqualTo(1)
            assertThat(notary.rewrites[0].signatory.checksum()).isEqualTo(recipient.address)

            val next = response.headers.getValue("link").extractNextLink()

            val nextResponse = mspServer.getNextLink(recipient.party, next!!)
            val nextJson = nextResponse.jsonObject

            assertThat(nextJson.getInt("status_code")).isEqualTo(1000)
            assertThat(nextJson.getJSONArray("data").length()).isEqualTo(5)
        }
    }


    // TODO:
    // -> send cdr to MSP (check location header)
    // -> send command (check async command result)
    // -> sending when whitelisted/blacklisted

    // TODO:
    // -> proxied resources not being deleted
    // -> async

}