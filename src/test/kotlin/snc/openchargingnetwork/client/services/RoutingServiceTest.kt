package snc.openchargingnetwork.client.services


import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import snc.openchargingnetwork.client.config.Properties
import snc.openchargingnetwork.client.models.*
import snc.openchargingnetwork.client.models.entities.Auth
import snc.openchargingnetwork.client.models.entities.EndpointEntity
import snc.openchargingnetwork.client.models.entities.PlatformEntity
import snc.openchargingnetwork.client.models.entities.RoleEntity
import snc.openchargingnetwork.client.models.ocpi.*
import snc.openchargingnetwork.client.repositories.*
import snc.openchargingnetwork.client.tools.generateUUIDv4Token
import snc.openchargingnetwork.contracts.RegistryFacade

class RoutingServiceTest {

    private val platformRepo: PlatformRepository = mockk()
    private val roleRepo: RoleRepository = mockk()
    private val endpointRepo: EndpointRepository = mockk()
    private val proxyResourceRepo: ProxyResourceRepository = mockk()
    private val httpRequestService: HttpRequestService = mockk()
    private val registry: RegistryFacade = mockk()
    private val credentialsService: CredentialsService = mockk()
    private val properties: Properties = mockk()

    private val routingService: RoutingService

    init {
        routingService = RoutingService(
                platformRepo,
                roleRepo,
                endpointRepo,
                proxyResourceRepo,
                registry,
                httpRequestService,
                credentialsService,
                properties)
    }

    @Test
    fun `prepareLocalPlatformRequest without proxy`() {
        val request = OcpiRequestVariables(
                module = ModuleID.TOKENS,
                method = HttpMethod.GET,
                interfaceRole = InterfaceRole.RECEIVER,
                requestID = generateUUIDv4Token(),
                correlationID = generateUUIDv4Token(),
                sender = BasicRole("SNC", "DE"),
                receiver = BasicRole("ABC", "CH"),
                urlPathVariables = "DE/SNC/abc123",
                urlEncodedParameters = OcpiRequestParameters(type = TokenType.APP_USER),
                expectedResponseType = OcpiResponseDataType.TOKEN)

        every { routingService.getPlatformID(request.receiver) } returns 6L
        every { routingService.getPlatformEndpoint(
                platformID = 6L,
                module = request.module,
                interfaceRole = request.interfaceRole)
        } returns EndpointEntity(
                platformID = 6L,
                identifier = request.module.id,
                role = InterfaceRole.SENDER,
                url = "https://ocpi.cpo.com/2.2/tokens")

        every { platformRepo.findById(6L).get() } returns PlatformEntity(
                status = ConnectionStatus.CONNECTED,
                auth = Auth(tokenB = "1234567890", tokenC = generateUUIDv4Token()))

        val (url, headers) = routingService.prepareLocalPlatformRequest(request)

        assertThat(url).isEqualTo("https://ocpi.cpo.com/2.2/tokens/DE/SNC/abc123")
        assertThat(headers.authorization).isEqualTo("Token 1234567890")
        assertThat(headers.requestID.length).isEqualTo(36)
        assertThat(headers.correlationID).isEqualTo(request.correlationID)
        assertThat(headers.ocpiFromCountryCode).isEqualTo(request.sender.country)
        assertThat(headers.ocpiFromPartyID).isEqualTo(request.sender.id)
        assertThat(headers.ocpiToCountryCode).isEqualTo(request.receiver.country)
        assertThat(headers.ocpiToPartyID).isEqualTo(request.receiver.id)
    }


    @Test
    fun `prepareLocalPlatformRequest with proxy`() {
        val request = OcpiRequestVariables(
                module = ModuleID.CDRS,
                method = HttpMethod.GET,
                interfaceRole = InterfaceRole.SENDER,
                requestID = generateUUIDv4Token(),
                correlationID = generateUUIDv4Token(),
                sender = BasicRole("SNC", "DE"),
                receiver = BasicRole("ABC", "CH"),
                urlPathVariables = "67",
                expectedResponseType = OcpiResponseDataType.CDR_ARRAY)

        every { routingService.getPlatformID(request.receiver) } returns 126L
        every { routingService.getProxyResource(
                id = "67",
                sender = request.sender,
                receiver = request.receiver)
        } returns "https://cpo.com/cdrs?limit=20"

        every { platformRepo.findById(126L).get() } returns PlatformEntity(
                status = ConnectionStatus.CONNECTED,
                auth = Auth(tokenB = "0102030405", tokenC = generateUUIDv4Token()))

        val (url, headers) = routingService.prepareLocalPlatformRequest(request, proxied = true)

        assertThat(url).isEqualTo("https://cpo.com/cdrs?limit=20")
        assertThat(headers.authorization).isEqualTo("Token 0102030405")
        assertThat(headers.requestID.length).isEqualTo(36)
        assertThat(headers.correlationID).isEqualTo(request.correlationID)
        assertThat(headers.ocpiFromCountryCode).isEqualTo(request.sender.country)
        assertThat(headers.ocpiFromPartyID).isEqualTo(request.sender.id)
        assertThat(headers.ocpiToCountryCode).isEqualTo(request.receiver.country)
        assertThat(headers.ocpiToPartyID).isEqualTo(request.receiver.id)
    }


    @Test
    fun `prepareRemotePlatformRequest without proxy`() {
        val request = OcpiRequestVariables(
                module = ModuleID.TOKENS,
                method = HttpMethod.GET,
                interfaceRole = InterfaceRole.RECEIVER,
                requestID = generateUUIDv4Token(),
                correlationID = generateUUIDv4Token(),
                sender = BasicRole("SNC", "DE"),
                receiver = BasicRole("ABC", "CH"),
                urlPathVariables = "DE/SNC/abc123",
                urlEncodedParameters = OcpiRequestParameters(type = TokenType.APP_USER),
                expectedResponseType = OcpiResponseDataType.TOKEN_ARRAY)

        val sig = "0x9955af11969a2d2a7f860cb00e6a00cfa7c581f5df2dbe8ea16700b33f4b4b9" +
                "b69f945012f7ea7d3febf11eb1b78e1adc2d1c14c2cf48b25000938cc1860c83e01"

        val ocnBody = OcnMessageRequestBody(
                method = request.method,
                interfaceRole = request.interfaceRole,
                module = request.module,
                headers = OcpiRequestHeaders(
                        requestID = request.requestID,
                        correlationID = request.correlationID,
                        ocpiFromCountryCode = request.sender.country,
                        ocpiFromPartyID = request.sender.id,
                        ocpiToCountryCode = request.receiver.country,
                        ocpiToPartyID = request.receiver.id),
                urlPathVariables = request.urlPathVariables,
                urlEncodedParameters = request.urlEncodedParameters,
                body = request.body,
                expectedResponseType = request.expectedResponseType)

        every { registry.clientURLOf(
                request.receiver.country.toByteArray(),
                request.receiver.id.toByteArray()).sendAsync().get() } returns "https://ocn.client.net"

        val jsonString = jacksonObjectMapper().writeValueAsString(ocnBody)
        every { httpRequestService.mapper.writeValueAsString(ocnBody) } returns jsonString
        every { credentialsService.signRequest(jsonString) } returns sig

        val (url, headers, body) = routingService.prepareRemotePlatformRequest(request)

        assertThat(url).isEqualTo("https://ocn.client.net")
        assertThat(headers.requestID.length).isEqualTo(36)
        assertThat(headers.signature).isEqualTo(sig)
        assertThat(body).isEqualTo(ocnBody)
    }


    @Test
    fun `prepareRemotePlatformRequest with proxy`() {
        val request = OcpiRequestVariables(
                module = ModuleID.SESSIONS,
                method = HttpMethod.GET,
                interfaceRole = InterfaceRole.SENDER,
                requestID = generateUUIDv4Token(),
                correlationID = generateUUIDv4Token(),
                sender = BasicRole("SNC", "DE"),
                receiver = BasicRole("ABC", "CH"),
                urlPathVariables = "45",
                expectedResponseType = OcpiResponseDataType.SESSION_ARRAY)

        val sig = "0x9955af11969a2d2a7f860cb00e6a00cfa7c581f5df2dbe8ea16700b33f4b4b9" +
                "b69f945012f7ea7d3febf11eb1b78e1adc2d1c14c2cf48b25000938cc1860c83e01"

        val ocnBody = OcnMessageRequestBody(
                method = request.method,
                interfaceRole = request.interfaceRole,
                module = request.module,
                headers = OcpiRequestHeaders(
                        requestID = request.requestID,
                        correlationID = request.correlationID,
                        ocpiFromCountryCode = request.sender.country,
                        ocpiFromPartyID = request.sender.id,
                        ocpiToCountryCode = request.receiver.country,
                        ocpiToPartyID = request.receiver.id),
                urlPathVariables = request.urlPathVariables,
                urlEncodedParameters = request.urlEncodedParameters,
                body = request.body,
                proxyResource = "https://actual.cpo.com/ocpi/sender/2.2/sessions?limit=10&offset=50; rel =\"next\"",
                expectedResponseType = request.expectedResponseType)

        every { registry.clientURLOf(
                request.receiver.country.toByteArray(),
                request.receiver.id.toByteArray()).sendAsync().get() } returns "https://ocn-client.provider.net"

        every { routingService.getProxyResource("45", request.sender, request.receiver) } returns
                "https://actual.cpo.com/ocpi/sender/2.2/sessions?limit=10&offset=50; rel =\"next\""

        val jsonString = jacksonObjectMapper().writeValueAsString(ocnBody)
        every { httpRequestService.mapper.writeValueAsString(ocnBody) } returns jsonString
        every { credentialsService.signRequest(jsonString) } returns sig

        val (url, headers, body) = routingService.prepareRemotePlatformRequest(request, proxied = true)

        assertThat(url).isEqualTo("https://ocn-client.provider.net")
        assertThat(headers.requestID.length).isEqualTo(36)
        assertThat(headers.signature).isEqualTo(sig)
        assertThat(body).isEqualTo(ocnBody)
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

//    @Test
//    fun makeHeaders() {
//        val sender = BasicRole("IOU", "UK")
//        val receiver = BasicRole("OII", "DE")
//        val platform = PlatformEntity(id = 34L, auth = Auth(tokenB = "abcdefghijklmnop"))
//        every { platformRepo.findById(34L).get() } returns platform
//        val headers = routingService.makeHeaders(platform.id, "0987654321", sender, receiver)
//        assertThat(headers["Authorization"]).isEqualTo("Token ${platform.auth.tokenB}")
//        assertThat(headers["X-Request-ID"]?.length ?: throw IllegalStateException()).isEqualTo(generateUUIDv4Token().length)
//        assertThat(headers["X-Correlation-ID"]).isEqualTo("0987654321")
//        assertThat(headers["OCPI-From-Country-Code"]).isEqualTo(sender.country)
//        assertThat(headers["OCPI-From-Party-ID"]).isEqualTo(sender.id)
//        assertThat(headers["OCPI-To-Country-Code"]).isEqualTo(receiver.country)
//        assertThat(headers["OCPI-To-Party-ID"]).isEqualTo(receiver.id)
//    }

//    @Test
//    fun `forwardRequest with GET`() {
//        val url = "http://localhost:8090/locations"
//        val headers = mapOf(
//                "Authorization" to "Token 1234567",
//                "X-Request-ID" to "123",
//                "X-Correlation-ID" to "456",
//                "OCPI-From-Country-Code" to "DE",
//                "OCPI-From-Party-ID" to "XXX",
//                "OCPI-To-Country-Code" to "DE",
//                "OCPI-To-Party-ID" to "AAA")
//        val params = mapOf("limit" to "100")
//        every { httpRequestService.makeRequest("GET", url, headers, params, body = null, expectedDataType = Array<Location>::class) } returns HttpResponse(
//                statusCode = 200,
//                headers = mapOf(),
//                body = OcpiResponse(
//                    statusCode = 1000,
//                    data = arrayOf(exampleLocation1, exampleLocation2)))
//        val response = routingService.forwardRequest("GET", url, headers, params, null, Array<Location>::class)
//        assertThat(response.statusCode).isEqualTo(200)
//        assertThat(response.body.statusCode).isEqualTo(1000)
//        assertThat(response.body.statusMessage).isNull()
//        assertThat(response.body.data?.size).isEqualTo(2)
//    }

//    @Test
//    fun `forwardRequest with POST`() {
//        val url = "http://localhost:8090/locations/LOC1/1234"
//        val headers = mapOf(
//                "Authorization" to "Token 1234567",
//                "X-Request-ID" to "123",
//                "X-Correlation-ID" to "456",
//                "OCPI-From-Country-Code" to "DE",
//                "OCPI-From-Party-ID" to "XXX",
//                "OCPI-To-Country-Code" to "DE",
//                "OCPI-To-Party-ID" to "AAA")
//
//        val body: Map<String, Any>? = mapper.readValue(mapper.writeValueAsString(exampleLocation1))
//        every { httpRequestService.mapper } returns mapper
//        every { httpRequestService.makeRequest("POST", url, headers, body = body, expectedDataType = Nothing::class) } returns HttpResponse(
//                statusCode = 200,
//                headers = mapOf(),
//                body = OcpiResponse(
//                    statusCode = 1000,
//                    data = null))
//        val response = routingService.forwardRequest("POST", url, headers, null, exampleLocation1, Nothing::class)
//        assertThat(response.statusCode).isEqualTo(200)
//        assertThat(response.body.statusCode).isEqualTo(1000)
//        assertThat(response.body.statusMessage).isNull()
//    }

//    @Test
//    fun `signRequest returns concatenated signature`() {
//        val body = OcnMessageRequestBody(
//                method = "GET",
//                module = "sessions",
//                interfaceRole = InterfaceRole.RECEIVER,
//                headers = OcpiRequestHeaders(
//                        requestID = "1",
//                        correlationID = "1",
//                        ocpiFromCountryCode = "DE",
//                        ocpiFromPartyID = "XXX",
//                        ocpiToCountryCode = "DE",
//                        ocpiToPartyID = "AAA"),
//                body = null,
//                expectedResponseType = OcpiResponseDataType.SESSION_ARRAY)
//        every { httpRequestService.mapper } returns mapper
//        every { credentialsService.credentials } returns Credentials.create(generatePrivateKey())
//        val sig = routingService.signRequest(body)
//        assertThat(sig.length).isEqualTo(130)
//    }

//    @Test
//    fun `verifyRequest silently succeeds`() {
//        val privateKey = generatePrivateKey()
//        val credentials = Credentials.create(privateKey)
//        val body = OcnMessageRequestBody(
//                method = "GET",
//                module = "sessions",
//                interfaceRole = InterfaceRole.RECEIVER,
//                headers = OcpiRequestHeaders(
//                        requestID = "1",
//                        correlationID = "1",
//                        ocpiFromCountryCode = "DE",
//                        ocpiFromPartyID = "XXX",
//                        ocpiToCountryCode = "DE",
//                        ocpiToPartyID = "AAA"),
//                body = null,
//                expectedResponseType = OcpiResponseDataType.SESSION_ARRAY)
//        every { httpRequestService.mapper } returns mapper
//        every { credentialsService.credentials } returns credentials
//        val sig = routingService.signRequest(body)
//        every { registry.clientAddressOf("DE".toByteArray(), "XXX".toByteArray()).sendAsync().get() } returns credentials.address
//        routingService.verifyRequest(body, sig, BasicRole("XXX", "DE"))
//    }

//    @Test
//    fun `verifyRequest loudly fails`() {
//        val credentials1 = Credentials.create(generatePrivateKey())
//        val credentials2 = Credentials.create(generatePrivateKey())
//        val body = OcnMessageRequestBody(
//                method = "GET",
//                module = "sessions",
//                interfaceRole = InterfaceRole.RECEIVER,
//                headers = OcpiRequestHeaders(
//                        requestID = "1",
//                        correlationID = "1",
//                        ocpiFromCountryCode = "DE",
//                        ocpiFromPartyID = "XXX",
//                        ocpiToCountryCode = "DE",
//                        ocpiToPartyID = "AAA"),
//                body = null,
//                expectedResponseType = OcpiResponseDataType.SESSION_ARRAY)
//        every { httpRequestService.mapper } returns mapper
//        every { credentialsService.credentials } returns credentials1
//        val sig = routingService.signRequest(body)
//        every { registry.clientAddressOf("DE".toByteArray(), "XXX".toByteArray()).sendAsync().get() } returns credentials2.address
//        try {
//            routingService.verifyRequest(body, sig, BasicRole("XXX", "DE"))
//        } catch (e: OcpiHubConnectionProblemException) {
//            assertThat(e.message).isEqualTo("Could not verify OCN-Signature of request")
//        }
//    }

}