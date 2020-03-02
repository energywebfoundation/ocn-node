package snc.openchargingnetwork.node.services

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import khttp.responses.Response
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import snc.openchargingnetwork.node.models.*

class HttpServiceTest {

    private val httpService = HttpService()

    @Test
    fun getVersions() {
        val versionNumber = "2.2"
        val versionUrl = "http://localhost:8080/ocpi/2.2"

        val mockResponse = mockk<Response>()
        every { mockResponse.text } returns
            """
            {
                "status_code": 1000,
                "data": [
                    {
                        "version": "$versionNumber",
                        "url": "$versionUrl"
                    }
                ],
                "timestamp": "2019-11-06T16:11:16.267Z"
            }
            """
        every { mockResponse.statusCode } returns 200

        mockkStatic("khttp.KHttp")
        every { khttp.get(any<String>(), any<Map<String, String?>>()) } returns mockResponse

        val versions = httpService.getVersions("https://www.example.com/ocpi/cpo/versions", "authToken")
        assertThat(versions.count()).isEqualTo(1)
        
        val firstVersion = versions[0]
        assertThat(firstVersion.version).isEqualTo(versionNumber)
        assertThat(firstVersion.url).isEqualTo(versionUrl)
    }
}