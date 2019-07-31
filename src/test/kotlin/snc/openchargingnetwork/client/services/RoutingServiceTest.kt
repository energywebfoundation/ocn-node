package snc.openchargingnetwork.client.services

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.web3j.crypto.Credentials
import snc.openchargingnetwork.client.data.exampleLocation1
import snc.openchargingnetwork.client.data.exampleLocation2
import snc.openchargingnetwork.client.models.HttpResponse
import snc.openchargingnetwork.client.models.HubGenericRequest
import snc.openchargingnetwork.client.models.HubRequestHeaders
import snc.openchargingnetwork.client.models.HubRequestResponseType
import snc.openchargingnetwork.client.models.entities.Auth
import snc.openchargingnetwork.client.models.entities.PlatformEntity
import snc.openchargingnetwork.client.models.entities.RoleEntity
import snc.openchargingnetwork.client.models.exceptions.OcpiHubConnectionProblemException
import snc.openchargingnetwork.client.models.ocpi.*
import snc.openchargingnetwork.client.repositories.*
import snc.openchargingnetwork.client.tools.generatePrivateKey
import snc.openchargingnetwork.client.tools.generateUUIDv4Token
import snc.openchargingnetwork.contracts.RegistryFacade

class RoutingServiceTest {

    private val mapper = jacksonObjectMapper()

    private val platformRepo: PlatformRepository = mockk()
    private val roleRepo: RoleRepository = mockk()
    private val endpointRepo: EndpointRepository = mockk()
    private val cdrRepo: CdrRepository = mockk()
    private val responseUrlRepo: CommandResponseUrlRepository = mockk()
    private val httpRequestService: HttpRequestService = mockk()
    private val registry: RegistryFacade = mockk()
    private val credentialsService: CredentialsService = mockk()

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
                credentialsService)
    }

    @Test
    fun isRoleKnown() {
        val role = BasicRole("ABC", "FR")
        every { roleRepo.existsByCountryCodeAndPartyIDAllIgnoreCase(role.country, role.id) } returns true
        assertThat(routingService.isRoleKnown(role)).isEqualTo(true)
    }

    @Test
    fun getPlatformID() {
        val role = RoleEntity(5L, Role.CPO, BusinessDetails("SENDER Co"), "SEN", "DE")
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
        assertThat(headers["OCPI-From-Country-Code"]).isEqualTo(sender.country)
        assertThat(headers["OCPI-From-Party-ID"]).isEqualTo(sender.id)
        assertThat(headers["OCPI-To-Country-Code"]).isEqualTo(receiver.country)
        assertThat(headers["OCPI-To-Party-ID"]).isEqualTo(receiver.id)
    }

    @Test
    fun `forwardRequest with GET`() {
        val url = "http://localhost:8090/locations"
        val headers = mapOf(
                "Authorization" to "Token 1234567",
                "X-Request-ID" to "123",
                "X-Correlation-ID" to "456",
                "OCPI-From-Country-Code" to "DE",
                "OCPI-From-Party-ID" to "XXX",
                "OCPI-To-Country-Code" to "DE",
                "OCPI-To-Party-ID" to "AAA")
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
                "OCPI-From-Country-Code" to "DE",
                "OCPI-From-Party-ID" to "XXX",
                "OCPI-To-Country-Code" to "DE",
                "OCPI-To-Party-ID" to "AAA")

        val body: Map<String, Any>? = mapper.readValue(mapper.writeValueAsString(exampleLocation1))
        every { httpRequestService.mapper } returns mapper
        every { httpRequestService.makeRequest("POST", url, headers, body = body, expectedDataType = Nothing::class) } returns HttpResponse(
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

    @Test
    fun `signRequest returns concatenated signature`() {
        val body = HubGenericRequest(
                method = "GET",
                module = "sessions",
                role = InterfaceRole.RECEIVER,
                headers = HubRequestHeaders(
                        requestID = "1",
                        correlationID = "1",
                        ocpiFromCountryCode = "DE",
                        ocpiFromPartyID = "XXX",
                        ocpiToCountryCode = "DE",
                        ocpiToPartyID = "AAA"),
                body = null,
                expectedResponseType = HubRequestResponseType.SESSION_ARRAY)
        every { httpRequestService.mapper } returns mapper
        every { credentialsService.credentials } returns Credentials.create(generatePrivateKey())
        val sig = routingService.signRequest(body)
        assertThat(sig.length).isEqualTo(130)
    }

    @Test
    fun `verifyRequest silently succeeds`() {
        val privateKey = generatePrivateKey()
        val credentials = Credentials.create(privateKey)
        val body = HubGenericRequest(
                method = "GET",
                module = "sessions",
                role = InterfaceRole.RECEIVER,
                headers = HubRequestHeaders(
                        requestID = "1",
                        correlationID = "1",
                        ocpiFromCountryCode = "DE",
                        ocpiFromPartyID = "XXX",
                        ocpiToCountryCode = "DE",
                        ocpiToPartyID = "AAA"),
                body = null,
                expectedResponseType = HubRequestResponseType.SESSION_ARRAY)
        every { httpRequestService.mapper } returns mapper
        every { credentialsService.credentials } returns credentials
        val sig = routingService.signRequest(body)
        every { registry.clientAddressOf("DE".toByteArray(), "XXX".toByteArray()).sendAsync().get() } returns credentials.address
        routingService.verifyRequest(body, sig, BasicRole("XXX", "DE"))
    }

    @Test
    fun `verifyRequest loudly fails`() {
        val credentials1 = Credentials.create(generatePrivateKey())
        val credentials2 = Credentials.create(generatePrivateKey())
        val body = HubGenericRequest(
                method = "GET",
                module = "sessions",
                role = InterfaceRole.RECEIVER,
                headers = HubRequestHeaders(
                        requestID = "1",
                        correlationID = "1",
                        ocpiFromCountryCode = "DE",
                        ocpiFromPartyID = "XXX",
                        ocpiToCountryCode = "DE",
                        ocpiToPartyID = "AAA"),
                body = null,
                expectedResponseType = HubRequestResponseType.SESSION_ARRAY)
        every { httpRequestService.mapper } returns mapper
        every { credentialsService.credentials } returns credentials1
        val sig = routingService.signRequest(body)
        every { registry.clientAddressOf("DE".toByteArray(), "XXX".toByteArray()).sendAsync().get() } returns credentials2.address
        try {
            routingService.verifyRequest(body, sig, BasicRole("XXX", "DE"))
        } catch (e: OcpiHubConnectionProblemException) {
            assertThat(e.message).isEqualTo("Could not verify OCN-Signature of request")
        }
    }

}