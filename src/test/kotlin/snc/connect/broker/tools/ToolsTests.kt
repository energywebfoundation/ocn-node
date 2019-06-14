package snc.connect.broker.tools

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

}