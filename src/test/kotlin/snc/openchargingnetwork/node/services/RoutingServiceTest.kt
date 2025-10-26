package snc.openchargingnetwork.node.services

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import snc.openchargingnetwork.node.components.HttpClientComponent
import snc.openchargingnetwork.node.models.OcnHeaders
import snc.openchargingnetwork.node.models.Receiver
import snc.openchargingnetwork.node.models.entities.Auth
import snc.openchargingnetwork.node.models.entities.EndpointEntity
import snc.openchargingnetwork.node.models.entities.PlatformEntity
import snc.openchargingnetwork.node.models.entities.RoleEntity
import snc.openchargingnetwork.node.models.exceptions.OcpiClientInvalidParametersException
import snc.openchargingnetwork.node.models.exceptions.OcpiClientUnknownLocationException
import snc.openchargingnetwork.node.models.exceptions.OcpiHubUnknownReceiverException
import snc.openchargingnetwork.node.models.ocpi.BasicRole
import snc.openchargingnetwork.node.models.ocpi.InterfaceRole
import snc.openchargingnetwork.node.models.ocpi.OcpiRequestVariables
import snc.openchargingnetwork.node.repositories.EndpointRepository
import snc.openchargingnetwork.node.repositories.PlatformRepository
import snc.openchargingnetwork.node.repositories.ProxyResourceRepository
import snc.openchargingnetwork.node.repositories.RoleRepository

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RoutingServiceTest(
        @Autowired private val routingService: RoutingService,
        @Autowired private val platformRepository: PlatformRepository,
        @Autowired private val roleRepository: RoleRepository,
        @Autowired private val endpointRepository: EndpointRepository,
        @Autowired private val proxyResourceRepository: ProxyResourceRepository,
        @Autowired private val httpClientComponent: HttpClientComponent
) {

    private lateinit var testPlatform: PlatformEntity
    private lateinit var testRole: RoleEntity
    private lateinit var testEndpoint: EndpointEntity
    private lateinit var testSender: BasicRole
    private lateinit var testReceiver: BasicRole

    @BeforeEach
    fun setUp() {
        // Create test platform
        testPlatform =
                PlatformEntity(
                        status =
                                snc.openchargingnetwork.node.models.ocpi.ConnectionStatus.CONNECTED,
                        auth =
                                Auth(
                                        tokenA = "test-token-a",
                                        tokenB = "test-token-b",
                                        tokenC = "test-token-c"
                                )
                )
        testPlatform = platformRepository.save(testPlatform)

        // Create test role
        testRole =
                RoleEntity(
                        platformID = testPlatform.id!!,
                        role = snc.openchargingnetwork.node.models.ocpi.Role.CPO,
                        businessDetails =
                                snc.openchargingnetwork.node.models.ocpi.BusinessDetails(
                                        name = "Test Company",
                                        website = "https://test.com"
                                ),
                        partyID = "TST",
                        countryCode = "DE"
                )
        testRole = roleRepository.save(testRole)

        // Create additional test roles for proxy resource tests
        val senderRole =
                RoleEntity(
                        platformID = testPlatform.id!!,
                        role = snc.openchargingnetwork.node.models.ocpi.Role.EMSP,
                        businessDetails =
                                snc.openchargingnetwork.node.models.ocpi.BusinessDetails(
                                        name = "Sender Company",
                                        website = "https://sender.com"
                                ),
                        partyID = "SND",
                        countryCode = "DE"
                )
        roleRepository.save(senderRole)

        val receiverRole =
                RoleEntity(
                        platformID = testPlatform.id!!,
                        role = snc.openchargingnetwork.node.models.ocpi.Role.CPO,
                        businessDetails =
                                snc.openchargingnetwork.node.models.ocpi.BusinessDetails(
                                        name = "Receiver Company",
                                        website = "https://receiver.com"
                                ),
                        partyID = "RCV",
                        countryCode = "DE"
                )
        roleRepository.save(receiverRole)

        // Create test endpoint
        testEndpoint =
                EndpointEntity(
                        platformID = testPlatform.id!!,
                        identifier = "locations",
                        role = InterfaceRole.RECEIVER,
                        url = "https://test-platform.com/ocpi/2.2.1/locations"
                )
        testEndpoint = endpointRepository.save(testEndpoint)

        // Set up test roles
        testSender = BasicRole("SND", "DE")
        testReceiver = BasicRole("TST", "DE")
    }

    @Test
    fun `isRoleKnown should return true for known role`() {
        val result = routingService.isRoleKnown(testReceiver)

        assertThat(result).isTrue()
    }

    @Test
    fun `isRoleKnown should return false for unknown role`() {
        val unknownRole = BasicRole("XXX", "XX")

        val result = routingService.isRoleKnown(unknownRole)

        assertThat(result).isFalse()
    }

    @Test
    fun `getPlatform should return platform for known role`() {
        val platform = routingService.getPlatform(testReceiver)

        assertThat(platform).isNotNull()
        assertThat(platform.id).isEqualTo(testPlatform.id)
    }

    @Test
    fun `getPlatform should throw exception for unknown role`() {
        val unknownRole = BasicRole("XXX", "XX")

        assertThrows<OcpiHubUnknownReceiverException> { routingService.getPlatform(unknownRole) }
    }

    @Test
    fun `getPlatformID should return platform ID for known role`() {
        val platformID = routingService.getPlatformID(testReceiver)

        assertThat(platformID).isEqualTo(testPlatform.id)
    }

    @Test
    fun `getPlatformID should throw exception for unknown role`() {
        val unknownRole = BasicRole("XXX", "XX")

        assertThrows<OcpiHubUnknownReceiverException> { routingService.getPlatformID(unknownRole) }
    }

    @Test
    fun `getPlatformRules should return rules for known role`() {
        val rules = routingService.getPlatformRules(testReceiver)

        assertThat(rules).isNotNull()
        assertThat(rules)
                .isInstanceOf(snc.openchargingnetwork.node.models.entities.OcnRules::class.java)
    }

    @Test
    fun `getPlatformEndpoint should return endpoint for known platform and module`() {
        val endpoint =
                routingService.getPlatformEndpoint(
                        testPlatform.id,
                        "locations",
                        InterfaceRole.RECEIVER
                )

        assertThat(endpoint).isNotNull()
        assertThat(endpoint.url).isEqualTo("https://test-platform.com/ocpi/2.2.1/locations")
        assertThat(endpoint.identifier).isEqualTo("locations")
        assertThat(endpoint.role).isEqualTo(InterfaceRole.RECEIVER)
    }

    @Test
    fun `getPlatformEndpoint should throw exception for unknown endpoint`() {
        assertThrows<OcpiClientInvalidParametersException> {
            routingService.getPlatformEndpoint(
                    testPlatform.id,
                    "unknown-module",
                    InterfaceRole.RECEIVER
            )
        }
    }

    @Test
    fun `checkSenderKnown should not throw for valid authorization`() {
        val authorization = "Token test-token-c"

        routingService.checkSenderKnown(authorization)
        // If no exception is thrown, the test passes
    }

    @Test
    fun `checkSenderKnown should throw exception for invalid authorization`() {
        val invalidAuthorization = "Token invalid-token"

        assertThrows<OcpiClientInvalidParametersException> {
            routingService.checkSenderKnown(invalidAuthorization)
        }
    }

    @Test
    fun `checkSenderKnown with role should not throw for valid sender`() {
        val authorization = "Token test-token-c"
        val sender = BasicRole("TST", "DE") // Same as testRole

        routingService.checkSenderKnown(authorization, sender)
        // If no exception is thrown, the test passes
    }

    @Test
    fun `checkSenderKnown with role should throw exception for invalid sender`() {
        val authorization = "Token test-token-c"
        val invalidSender = BasicRole("XXX", "XX")

        assertThrows<OcpiClientInvalidParametersException> {
            routingService.checkSenderKnown(authorization, invalidSender)
        }
    }

    @Test
    fun `getReceiverType should return LOCAL for known role`() {
        val receiverType = routingService.getReceiverType(testReceiver)

        assertThat(receiverType).isEqualTo(Receiver.LOCAL)
    }

    @Test
    fun `getReceiverType should throw exception for unknown role`() {
        val unknownRole = BasicRole("XXX", "XX")

        assertThrows<OcpiHubUnknownReceiverException> {
            routingService.getReceiverType(unknownRole)
        }
    }

    @Test
    fun `setProxyResource should save and return resource ID`() {
        val resource = "https://example.com/resource"
        val sender = BasicRole("SND", "DE")
        val receiver = BasicRole("RCV", "DE")

        val resourceID = routingService.setProxyResource(resource, sender, receiver)

        assertThat(resourceID).isNotNull()
        assertThat(resourceID).isNotEmpty()

        // Verify it was saved
        val savedResource = proxyResourceRepository.findById(resourceID.toLong())
        assertThat(savedResource).isPresent()
        assertThat(savedResource.get().resource).isEqualTo(resource)
    }

    @Test
    fun `setProxyResource with alternativeUID should save with custom ID`() {
        val resource = "https://example.com/resource"
        val sender = BasicRole("SND", "DE")
        val receiver = BasicRole("RCV", "DE")
        val alternativeUID = "custom-id"

        val resourceID = routingService.setProxyResource(resource, sender, receiver, alternativeUID)

        assertThat(resourceID).isEqualTo(alternativeUID)
    }

    @Test
    fun `getProxyResource should return resource for valid ID`() {
        val resource = "https://example.com/resource"
        val sender = BasicRole("SND", "DE")
        val receiver = BasicRole("RCV", "DE")
        val resourceID = routingService.setProxyResource(resource, sender, receiver)

        val retrievedResource = routingService.getProxyResource(resourceID, sender, receiver)

        assertThat(retrievedResource).isEqualTo(resource)
    }

    @Test
    fun `getProxyResource should throw exception for invalid ID`() {
        val sender = BasicRole("SND", "DE")
        val receiver = BasicRole("RCV", "DE")

        assertThrows<OcpiClientUnknownLocationException> {
            routingService.getProxyResource("99999", sender, receiver)
        }
    }

    @Test
    fun `getProxyResource should throw exception for null ID`() {
        val sender = BasicRole("SND", "DE")
        val receiver = BasicRole("RCV", "DE")

        assertThrows<OcpiClientUnknownLocationException> {
            routingService.getProxyResource(null, sender, receiver)
        }
    }

    @Test
    fun `deleteProxyResource should remove resource`() {
        val resource = "https://example.com/resource"
        val sender = BasicRole("SND", "DE")
        val receiver = BasicRole("RCV", "DE")
        val resourceID = routingService.setProxyResource(resource, sender, receiver)

        routingService.deleteProxyResource(resourceID)

        val deletedResource = proxyResourceRepository.findById(resourceID.toLong())
        assertThat(deletedResource).isEmpty()
    }

    @Test
    fun `prepareLocalPlatformRequest should return URL and headers`() {
        val request =
                OcpiRequestVariables(
                        headers =
                                OcnHeaders(
                                        sender = testSender,
                                        receiver = testReceiver,
                                        authorization = "Token test-token",
                                        requestID = "test-request-id",
                                        correlationID = "test-correlation-id"
                                ),
                        urlPath = "/locations",
                        interfaceRole = InterfaceRole.RECEIVER,
                        module = snc.openchargingnetwork.node.models.ocpi.ModuleID.LOCATIONS,
                        method = org.springframework.http.HttpMethod.GET
                )

        val (url, headers) = routingService.prepareLocalPlatformRequest(request)

        assertThat(url).isNotNull()
        assertThat(url).isNotEmpty()
        assertThat(headers).isNotNull()
        assertThat(headers.authorization).isEqualTo("Token test-token-b")
        assertThat(headers.requestID).isNotEqualTo("test-request-id") // Should be regenerated
    }

    @Test
    fun `prepareLocalPlatformRequest with proxied should return proxy resource`() {
        val resource = "https://example.com/proxy-resource"
        val sender = BasicRole("TST", "DE") // Use existing test role
        val receiver = BasicRole("TST", "DE") // Use existing test role
        val resourceID = routingService.setProxyResource(resource, sender, receiver)

        val request =
                OcpiRequestVariables(
                        headers =
                                OcnHeaders(
                                        sender = sender,
                                        receiver = receiver,
                                        authorization = "Token test-token",
                                        requestID = "test-request-id",
                                        correlationID = "test-correlation-id"
                                ),
                        urlPath = resourceID,
                        interfaceRole = InterfaceRole.RECEIVER,
                        module = snc.openchargingnetwork.node.models.ocpi.ModuleID.LOCATIONS,
                        method = org.springframework.http.HttpMethod.GET
                )

        val (url, headers) = routingService.prepareLocalPlatformRequest(request, proxied = true)

        assertThat(url).isEqualTo(resource)
        assertThat(headers).isNotNull()
    }
}
