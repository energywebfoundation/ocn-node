package snc.openchargingnetwork.node.tools

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ExtensionsTests {

    @Test
    fun extractToken() {
        val auth = "Token 1234567890"
        assertThat(auth.extractToken()).isEqualTo("1234567890")
    }

    @Test
    fun extractNextLink() {
        val link = "<http://cpo.com/ocpi/cpo/2.2/locations?limit=10&offset=90>; rel=\"last\", <http://cpo.com/ocpi/cpo/2.2/locations?limit=10&offset=110>; rel=\"next\""
        assertThat(link.extractNextLink()).isEqualTo("http://cpo.com/ocpi/cpo/2.2/locations?limit=10&offset=110")

        val badLink = "http://malformed.link/blahblah; rel=\"next\""
        assertThat(badLink.extractNextLink()).isNull()
    }

}