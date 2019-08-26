package snc.openchargingnetwork.client.tools

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ExtensionsTests {

    @Test
    fun extractToken() {
        val auth = "Token 1234567890"
        assertThat(auth.extractToken()).isEqualTo("1234567890")
    }

}