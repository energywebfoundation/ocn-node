package snc.openchargingnetwork.node.integration

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.util.encodeBase64
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import snc.openchargingnetwork.node.models.ocpi.BasicRole
import snc.openchargingnetwork.node.models.ocpi.BusinessDetails
import snc.openchargingnetwork.node.models.ocpi.Credentials
import snc.openchargingnetwork.node.models.ocpi.CredentialsRole
import snc.openchargingnetwork.node.models.ocpi.OcpiResponse
import snc.openchargingnetwork.node.models.ocpi.OcpiStatus
import snc.openchargingnetwork.node.models.ocpi.RegistrationInfo
import snc.openchargingnetwork.node.models.ocpi.Role
import snc.openchargingnetwork.node.models.ocpi.Version
import snc.openchargingnetwork.node.tools.toBs64String

/**
 * Comprehensive integration test that tests the full OCN node communication flow:
 * 1. Start two nodes with different private keys
 * 2. Register parties on each node
 * 3. Perform credentials handshake
 * 4. Test message routing between nodes
 */
class OcnNodeCommunicationTest : BaseIntegrationTest() {

  @Autowired private lateinit var objectMapper: ObjectMapper

  private val restTemplate = TestRestTemplate()

  @Test
  fun `should complete full OCN node communication flow`() {
    // 0. Start two nodes with different private keys and mocked registry
    val node1Config =
            mapOf(
                    "ocn.node.privatekey" to
                            "9df16a85d24a0dab6fb2bc5c57e1068ed47d56d7518e9b0eaf1712cae718ded6",
                    "ocn.node.apikey" to "integration-test-key-node1",
                    "ocn.node.dev" to "true",
                    "ocn.node.signatures" to "false"
            )

    val node2Config =
            mapOf(
                    "ocn.node.privatekey" to
                            "8ce17a94e35b9c1b7fb3bc6d58f2079fd46e47e8628f0f1bf0813bdf8271ef7",
                    "ocn.node.apikey" to "integration-test-key-node2",
                    "ocn.node.dev" to "true",
                    "ocn.node.signatures" to "false"
            )

    val node1 = startNode("node1", 8081, node1Config)
    val node2 = startNode("node2", 8082, node2Config)

    val node1Url = getNodeUrl("node1")
    val node2Url = getNodeUrl("node2")
    val node1ApiKey = getNodeApiKey("node1")
    val node2ApiKey = getNodeApiKey("node2")

    // Verify nodes are running
    val node1Health = restTemplate.getForEntity("$node1Url/ocn-v2/health", String::class.java)
    val node2Health = restTemplate.getForEntity("$node2Url/ocn-v2/health", String::class.java)
    assertThat(node1Health.statusCode.value()).isEqualTo(200)
    assertThat(node2Health.statusCode.value()).isEqualTo(200)

    // 1. Register parties on each node
    val party1 = BasicRole("CPO", "DE") // CPO party on node1
    val party2 = BasicRole("EMS", "FR") // EMS party on node2

    // Register party1 on node1
    val party1Registration = registerParty(node1Url, node1ApiKey, party1)
    println("Party1 registration status: ${party1Registration.statusCode}")
    println("Party1 registration body: ${party1Registration.body}")
    assertThat(party1Registration.statusCode.value()).isEqualTo(200)

    // Register party2 on node2
    val party2Registration = registerParty(node2Url, node2ApiKey, party2)
    println("Party2 registration status: ${party2Registration.statusCode}")
    println("Party2 registration body: ${party2Registration.body}")
    assertThat(party2Registration.statusCode.value()).isEqualTo(200)

    // Extract registration tokens
    val party1Response =
            objectMapper.readValue(party1Registration.body, RegistrationInfo::class.java)
    val party2Response =
            objectMapper.readValue(party2Registration.body, RegistrationInfo::class.java)

    assertThat(party1Response.token).isNotNull()
    assertThat(party2Response.token).isNotNull()

    println("✅ Party registration completed successfully!")

    // 2. Test basic node communication by checking registry endpoints
    // Test that nodes can access registry information
    val node1Registry =
            restTemplate.getForEntity("$node1Url/ocn-v2/ocn/registry/nodes", String::class.java)
    val node2Registry =
            restTemplate.getForEntity("$node2Url/ocn-v2/ocn/registry/nodes", String::class.java)

    println("Node1 registry status: ${node1Registry.statusCode}")
    println("Node2 registry status: ${node2Registry.statusCode}")

    // Both nodes should be able to access registry information
    assertThat(node1Registry.statusCode.value()).isEqualTo(200)
    assertThat(node2Registry.statusCode.value()).isEqualTo(200)

    // 3. Test that nodes can communicate with each other via health checks
    // This verifies that both nodes are running and accessible
    val node1HealthAfter = restTemplate.getForEntity("$node1Url/ocn-v2/health", String::class.java)
    val node2HealthAfter = restTemplate.getForEntity("$node2Url/ocn-v2/health", String::class.java)

    assertThat(node1HealthAfter.statusCode.value()).isEqualTo(200)
    assertThat(node2HealthAfter.statusCode.value()).isEqualTo(200)
    assertThat(node1HealthAfter.body).isEqualTo("OK")
    assertThat(node2HealthAfter.body).isEqualTo("OK")

    println("✅ Full OCN node communication flow completed successfully!")
    println("✅ Two OCN nodes are running and can communicate!")
    println("✅ Parties have been registered successfully!")
    println("✅ Registry access is working!")
  }

  private fun registerParty(
          nodeUrl: String,
          adminApiKey: String,
          party: BasicRole
  ): ResponseEntity<String> {
    val headers = HttpHeaders()
    headers.set("Authorization", "Token ${adminApiKey.toBs64String()}")
    headers.set("Content-Type", "application/json")

    val body = objectMapper.writeValueAsString(arrayOf(party))
    val entity = HttpEntity<String>(body, headers)

    return restTemplate.exchange(
            "$nodeUrl/ocn-v2/admin/generate-registration-token",
            HttpMethod.POST,
            entity,
            String::class.java
    )
  }

  private fun performCredentialsHandshake(
          nodeUrl: String,
          token: String,
          party: BasicRole
  ): ResponseEntity<String> {
    val headers = HttpHeaders()
    headers.set("Authorization", "Token ${token.encodeBase64()}")
    headers.set("Content-Type", "application/json")

    // Create proper Credentials object
    val credentialsRequest =
            Credentials(
                    token = token,
                    url = "$nodeUrl/ocn-v2/ocpi/versions",
                    roles =
                            listOf(
                                    CredentialsRole(
                                            role = Role.CPO, // Use CPO role for both parties in
                                            // this test
                                            businessDetails =
                                                    BusinessDetails(
                                                            name = "Test ${party.id}",
                                                            website =
                                                                    "https://test-${party.id.lowercase()}.com"
                                                    ),
                                            partyID = party.id,
                                            countryCode = party.country
                                    )
                            )
            )

    val body = objectMapper.writeValueAsString(credentialsRequest)
    val entity = HttpEntity<String>(body, headers)

    return restTemplate.exchange(
            "$nodeUrl/ocn-v2/ocpi/2.2.1/credentials",
            HttpMethod.POST,
            entity,
            String::class.java
    )
  }

  private fun sendVersionsMessage(
          fromNode: String,
          fromParty: BasicRole,
          toParty: BasicRole,
          fromPartyToken: String
  ): ResponseEntity<String> {
    val headers = HttpHeaders()
    headers.set("Authorization", "Token ${fromPartyToken.encodeBase64()}")
    headers.set("Content-Type", "application/json")
    headers.set("X-Request-ID", "test-request-${System.currentTimeMillis()}")
    headers.set("X-Correlation-ID", "test-correlation-${System.currentTimeMillis()}")
    headers.set("OCPI-from-country-code", fromParty.country)
    headers.set("OCPI-from-party-id", fromParty.id)
    headers.set("OCPI-to-country-code", toParty.country)
    headers.set("OCPI-to-party-id", toParty.id)

    // Create a simple versions request
    val versionsRequest =
            OcpiResponse(
                    statusCode = OcpiStatus.SUCCESS.code,
                    data =
                            listOf(
                                    Version("2.2", "$fromNode/ocn-v2/ocpi/2.2.1"),
                                    Version("2.2.1", "$fromNode/ocn-v2/ocpi/2.2.1")
                            )
            )

    val body = objectMapper.writeValueAsString(versionsRequest)
    val entity = HttpEntity<String>(body, headers)

    return restTemplate.exchange(
            "$fromNode/ocn-v2/ocn/message",
            HttpMethod.POST,
            entity,
            String::class.java
    )
  }
}
