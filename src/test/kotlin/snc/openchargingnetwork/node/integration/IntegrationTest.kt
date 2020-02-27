package snc.openchargingnetwork.node.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.boot.builder.SpringApplicationBuilder
import snc.openchargingnetwork.node.Application

class IntegrationTest {

    @BeforeAll
    fun bootStrap() {
        val node1 = SpringApplicationBuilder(Application::class.java)
                .properties("server.port=8080")
        val node2 = SpringApplicationBuilder(Application::class.java)
                .properties("server.port=8081")
                .addCommandLineProperties(true)

        node1.run()
        node2.run(
                "--ocn.node.url=http://localhost:8081",
                "--ocn.node.privatekey=0x0dbbe8e4ae425a6d2687f1a7e3ba17bc98c673636790f1b8ad91193c05875ef1")
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

}