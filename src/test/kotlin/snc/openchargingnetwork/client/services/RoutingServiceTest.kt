package snc.openchargingnetwork.client.services

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import snc.openchargingnetwork.client.config.Configuration
import snc.openchargingnetwork.client.config.Properties
import snc.openchargingnetwork.client.data.exampleLocation1
import snc.openchargingnetwork.client.data.exampleLocation2
import snc.openchargingnetwork.client.models.HttpResponse
import snc.openchargingnetwork.client.models.entities.Auth
import snc.openchargingnetwork.client.models.entities.PlatformEntity
import snc.openchargingnetwork.client.models.entities.RoleEntity
import snc.openchargingnetwork.client.models.ocpi.*
import snc.openchargingnetwork.client.repositories.*
import snc.openchargingnetwork.client.tools.generatePrivateKey
import snc.openchargingnetwork.client.tools.generateUUIDv4Token
import snc.openchargingnetwork.contracts.RegistryFacade

class RoutingServiceTest {

    private val platformRepo: PlatformRepository = mockk()
    private val roleRepo: RoleRepository = mockk()
    private val endpointRepo: EndpointRepository = mockk()
    private val cdrRepo: CdrRepository = mockk()
    private val responseUrlRepo: CommandResponseUrlRepository = mockk()
    private val httpRequestService: HttpRequestService = mockk()
    private val registry: RegistryFacade = mockk()
    private val config: Configuration = mockk()
    private val properties: Properties = mockk()

    private val routingService: RoutingService

    init {
        routingService = RoutingService(
                platformRepo,
                roleRepo,
                endpointRepo,
                cdrRepo,
                responseUrlRepo,
                httpRequestService,
                registry,
                config,
                properties)
    }

    @Test
    fun isRoleKnown() {
        val role = BasicRole("ABC", "FR")
        every { roleRepo.existsByCountryCodeAndPartyIDAllIgnoreCase(role.country, role.id) } returns true
        assertThat(routingService.isRoleKnown(role)).isEqualTo(true)
    }

    @Test
    fun getPlatformID() {
        val role = RoleEntity(5L, Role.CPO, BusinessDetails("SENDER Co"), "SENDER", "DE", generatePrivateKey())
        every { roleRepo.findByCountryCodeAndPartyIDAllIgnoreCase(role.countryCode, role.partyID) } returns role
        assertThat(routingService.getPlatformID(BasicRole(role.partyID, role.countryCode))).isEqualTo(5L)
    }

    @Test
    fun `validateSender 1`() {
        val role = BasicRole("YUT", "BE")
        val platform = PlatformEntity(id = 3L)
        every { platformRepo.findByAuth_TokenC("0102030405") } returns platform
        every { roleRepo.existsByPlatformIDAndCountryCodeAndPartyIDAllIgnoreCase(3L, role.country, role.id) } returns true
        routingService.validateSender("Token 0102030405", role)
    }

    @Test
    fun `validateSender 2`() {
        val role = BasicRole("YUT", "BE")
        val objectOwner = BasicRole("yut", "be")
        val platform = PlatformEntity(id = 3L)
        every { platformRepo.findByAuth_TokenC("0102030405") } returns platform
        every { roleRepo.existsByPlatformIDAndCountryCodeAndPartyIDAllIgnoreCase(3L, role.country, role.id) } returns true
        routingService.validateSender("Token 0102030405", role, objectOwner)
    }

    @Test
    fun makeHeaders() {
        val sender = BasicRole("IOU", "UK")
        val receiver = BasicRole("OII", "DE")
        val platform = PlatformEntity(id = 34L, auth = Auth(tokenB = "abcdefghijklmnop"))
        every { platformRepo.findById(34L).get() } returns platform
        val headers = routingService.makeHeaders(platform.id, "0987654321", sender, receiver)
        assertThat(headers["Authorization"]).isEqualTo("Token ${platform.auth.tokenB}")
        assertThat(headers["X-Request-ID"]?.length ?: throw IllegalStateException()).isEqualTo(generateUUIDv4Token().length)
        assertThat(headers["X-Correlation-ID"]).isEqualTo("0987654321")
        assertThat(headers["OCPI-from-country-code"]).isEqualTo(sender.country)
        assertThat(headers["OCPI-from-party-id"]).isEqualTo(sender.id)
        assertThat(headers["OCPI-to-country-code"]).isEqualTo(receiver.country)
        assertThat(headers["OCPI-to-party-id"]).isEqualTo(receiver.id)
    }

    @Test
    fun `forwardRequest with GET`() {
        val url = "http://localhost:8090/locations"
        val headers = mapOf(
                "Authorization" to "Token 1234567",
                "X-Request-ID" to "123",
                "X-Correlation-ID" to "456",
                "OCPI-from-country-code" to "DE",
                "OCPI-from-party-id" to "XXX",
                "OCPI-to-country-code" to "DE",
                "OCPI-to-party-id" to "AAA")
        val params = mapOf("limit" to "100")
        every { httpRequestService.makeRequest("GET", url, headers, params, body = null, expectedDataType = Array<Location>::class) } returns HttpResponse(
                statusCode = 200,
                headers = mapOf(),
                body = OcpiResponse(
                    statusCode = 1000,
                    data = arrayOf(exampleLocation1, exampleLocation2)))
        val response = routingService.forwardRequest("GET", url, headers, params, null, Array<Location>::class)
        assertThat(response.statusCode).isEqualTo(200)
        assertThat(response.body.statusCode).isEqualTo(1000)
        assertThat(response.body.statusMessage).isNull()
        assertThat(response.body.data?.size).isEqualTo(2)
    }

    @Test
    fun `forwardRequest with POST`() {
        val url = "http://localhost:8090/locations/LOC1/1234"
        val headers = mapOf(
                "Authorization" to "Token 1234567",
                "X-Request-ID" to "123",
                "X-Correlation-ID" to "456",
                "OCPI-from-country-code" to "DE",
                "OCPI-from-party-id" to "XXX",
                "OCPI-to-country-code" to "DE",
                "OCPI-to-party-id" to "AAA")
        every { httpRequestService.makeRequest("POST", url, headers, body = exampleLocation1, expectedDataType = Nothing::class) } returns HttpResponse(
                statusCode = 200,
                headers = mapOf(),
                body = OcpiResponse(
                    statusCode = 1000,
                    data = null))
        val response = routingService.forwardRequest("POST", url, headers, null, exampleLocation1, Nothing::class)
        assertThat(response.statusCode).isEqualTo(200)
        assertThat(response.body.statusCode).isEqualTo(1000)
        assertThat(response.body.statusMessage).isNull()
    }

}