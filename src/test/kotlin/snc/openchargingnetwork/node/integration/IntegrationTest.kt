package snc.openchargingnetwork.node.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.boot.builder.SpringApplicationBuilder
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import org.web3j.tx.ClientTransactionManager
import org.web3j.tx.gas.StaticGasProvider
import snc.openchargingnetwork.contracts.Registry
import snc.openchargingnetwork.node.Application

class IntegrationTest {

    private val provider = "http://localhost:8544"

    @BeforeAll
    fun bootStrap() {
        // important: must run ganache first [e.g. ./gradlew ganache]
        val web3 = Web3j.build(HttpService(provider))
        val txManager = ClientTransactionManager(web3, "0x627306090abaB3A6e1400e9345bC60c78a8BEf57")
        val gasProvider = StaticGasProvider(0.toBigInteger(), 10000000.toBigInteger())
        val registry = Registry.deploy(web3, txManager, gasProvider).sendAsync().get()

        val node1 = SpringApplicationBuilder(Application::class.java)
                .properties("server.port=8080")
                .addCommandLineProperties(true)
        val node2 = SpringApplicationBuilder(Application::class.java)
                .properties("server.port=8081")
                .addCommandLineProperties(true)

        node1.run(
                "--ocn.node.web3.provider=$provider",
                "--ocn.node.web3.contracts.registry=${registry.contractAddress}")
        node2.run(
                "--ocn.node.url=http://localhost:8081",
                "--ocn.node.privatekey=0x0dbbe8e4ae425a6d2687f1a7e3ba17bc98c673636790f1b8ad91193c05875ef1",
                "--ocn.node.web3.provider=$provider",
                "--ocn.node.web3.contracts.registry=${registry.contractAddress}")
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
        val response1 = khttp.get("http://localhost:8080/ocn/registry/node/de/cpo")
        val response2 = khttp.get("http://localhost:8081/ocn/registry/node/de/cpo")

        assertThat(response1.statusCode).isEqualTo(200)
        assertThat(response2.statusCode).isEqualTo(200)

        assertThat(response1.text).isEqualTo("Party not registered on OCN")
        assertThat(response2.text).isEqualTo("Party not registered on OCN")
    }

}