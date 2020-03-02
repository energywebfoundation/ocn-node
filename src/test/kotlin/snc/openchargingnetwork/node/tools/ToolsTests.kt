package snc.openchargingnetwork.node.tools

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.web3j.crypto.Keys

class ToolsTests {

    @Test
    fun urlJoin() {
        assertThat(urlJoin("http://localhost:3000", "/endpoint")).isEqualTo("http://localhost:3000/endpoint")
        assertThat(urlJoin("http://localhost:3000/", "/endpoint")).isEqualTo("http://localhost:3000/endpoint")
        assertThat(urlJoin("http://localhost:3000/", "endpoint")).isEqualTo("http://localhost:3000/endpoint")
        assertThat(urlJoin("http://localhost:3000", "/endpoint/")).isEqualTo("http://localhost:3000/endpoint")
    }

}