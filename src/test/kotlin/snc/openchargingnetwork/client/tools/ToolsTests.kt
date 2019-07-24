package snc.openchargingnetwork.client.tools

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ToolsTests {

    @Test
    fun urlJoin() {
        assertThat(urlJoin("http://localhost:3000", "/endpoint")).isEqualTo("http://localhost:3000/endpoint")
        assertThat(urlJoin("http://localhost:3000/", "/endpoint")).isEqualTo("http://localhost:3000/endpoint")
        assertThat(urlJoin("http://localhost:3000/", "endpoint")).isEqualTo("http://localhost:3000/endpoint")
        assertThat(urlJoin("http://localhost:3000", "/endpoint/")).isEqualTo("http://localhost:3000/endpoint")
    }

    @Test
    fun generatePrivKey() {
        val pkey1 = generatePrivateKey()
        val pkey2 = generatePrivateKey()
        assertThat(pkey1).isNotEqualTo(pkey2)
    }

}