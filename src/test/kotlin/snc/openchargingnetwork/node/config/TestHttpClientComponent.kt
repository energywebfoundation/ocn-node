package snc.openchargingnetwork.node.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import snc.openchargingnetwork.node.components.HttpClientComponent
import snc.openchargingnetwork.node.models.ocpi.Version
import snc.openchargingnetwork.node.models.ocpi.VersionDetail
import snc.openchargingnetwork.node.models.ocpi.OcpiResponse
import snc.openchargingnetwork.node.models.SyncedHttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.Headers
import io.ktor.http.ContentType

@TestConfiguration
class TestHttpClientComponent {

        @Bean
        @Primary
        fun mockHttpClientComponent(): HttpClientComponent {
                return object : HttpClientComponent() {
                        override fun getVersions(url: String, authorization: String): List<Version> {
                                // Return mock versions data
                                return listOf(
                                        Version(
                                                version = "2.2",
                                                url = "$url/2.2.1"
                                        ),
                                        Version(
                                                version = "2.1.1",
                                                url = "$url/2.1.1"
                                        )
                                )
                        }

                        override fun getVersionDetail(url: String, authorization: String): VersionDetail {
                                // Return mock version detail data
                                return VersionDetail(
                                        version = "2.2",
                                        endpoints = listOf(
                                                snc.openchargingnetwork.node.models.ocpi.Endpoint(
                                                        identifier = "locations",
                                                        role = snc.openchargingnetwork.node.models.ocpi.InterfaceRole.RECEIVER,
                                                        url = "$url/locations"
                                                ),
                                                snc.openchargingnetwork.node.models.ocpi.Endpoint(
                                                        identifier = "sessions",
                                                        role = snc.openchargingnetwork.node.models.ocpi.InterfaceRole.RECEIVER,
                                                        url = "$url/sessions"
                                                )
                                        )
                                )
                        }

                        override fun checkVersionsHealth(url: String): List<Version> {
                                // Return mock health check data
                                return listOf(
                                        Version(
                                                version = "2.2",
                                                url = "$url/2.2.1"
                                        )
                                )
                        }

                        override fun sendHttpRequest(
                                endpoint: String,
                                method: org.springframework.http.HttpMethod,
                                body: Any?,
                                headers: Map<String, String>,
                                queryParams: Map<String, String>
                        ): SyncedHttpResponse {
                                // Return mock HTTP response
                                return SyncedHttpResponse(
                                        statusCode = HttpStatusCode.OK,
                                        headers = Headers.build {
                                                append("Content-Type", "application/json")
                                        },
                                        contentType = ContentType.Application.Json,
                                        contentLength = 100L,
                                        body = """
                    {
                        "status_code": 1000,
                        "status_message": "Success",
                        "data": [
                            {
                                "version": "2.2",
                                "url": "$endpoint/2.2.1"
                            },
                            {
                                "version": "2.1.1",
                                "url": "$endpoint/2.1.1"
                            }
                        ],
                        "timestamp": "2023-01-01T00:00:00Z"
                    }
                    """.trimIndent()
                                )
                        }
                }
        }
}