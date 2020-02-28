package snc.openchargingnetwork.node.integration

import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.web3j.crypto.Credentials
import snc.openchargingnetwork.node.data.exampleLocation1
import snc.openchargingnetwork.node.integration.parties.CpoServer
import snc.openchargingnetwork.node.integration.parties.MspServer
import snc.openchargingnetwork.node.models.ocpi.BasicRole
import snc.openchargingnetwork.node.models.ocpi.Location

class IntegrationTest {

    private lateinit var mspServer: MspServer

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
        val cpoServer1 = CpoServer(BasicRole("CPA", "CH"), 8100)
        cpoServer1.setPartyInRegistry(registry.contractAddress, cpo1, node1.address)
        cpoServer1.registerCredentials()

        // CPO 2
        val cpo2 = Credentials.create("0x388c684f0ba1ef5017716adb5d21a053ea8e90277d0868337519f97bede61418")
        val cpoServer2 = CpoServer(BasicRole("CPB", "CH"), 8101)
        cpoServer2.setPartyInRegistry(registry.contractAddress, cpo2, node2.address)
        cpoServer2.registerCredentials()

        // MSP
        val msp = Credentials.create("0x659cbb0e2411a44db63778987b1e22153c086a95eb6b18bdf89de078917abc63")
        mspServer = MspServer(BasicRole("MSP", "DE"), 8200)
        mspServer.setPartyInRegistry(registry.contractAddress, msp, node1.address)
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

    @Test
    fun get_location_local() {
        val to = BasicRole("CPA", "CH")
        val response = mspServer.getLocation(to)
        val json = response.jsonObject

        assertThat(response.statusCode).isEqualTo(200)
        assertThat(json.getInt("status_code")).isEqualTo(1000)
        assertThat(objectMapper.readValue<Location>(json.getJSONObject("data").toString())).isEqualTo(exampleLocation1)
    }

    @Test
    fun get_location_remote() {
        val to = BasicRole("CPB", "CH")
        val response = mspServer.getLocation(to)
        val json = response.jsonObject

        assertThat(response.statusCode).isEqualTo(200)
        assertThat(json.getInt("status_code")).isEqualTo(1000)
        assertThat(objectMapper.readValue<Location>(json.getJSONObject("data").toString())).isEqualTo(exampleLocation1)
    }


}