package snc.openchargingnetwork.node.services

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import snc.openchargingnetwork.node.config.NodeProperties
import snc.openchargingnetwork.node.models.*
import snc.openchargingnetwork.node.models.entities.*
import snc.openchargingnetwork.node.models.ocpi.*
import snc.openchargingnetwork.node.repositories.*
import snc.openchargingnetwork.node.tools.generateUUIDv4Token
import snc.openchargingnetwork.node.tools.urlJoin
import snc.openchargingnetwork.contracts.RegistryFacade


class RoutingServiceTest {

    private val platformRepo: PlatformRepository = mockk()
    private val roleRepo: RoleRepository = mockk()
    private val endpointRepo: EndpointRepository = mockk()
    private val ocnRulesListRepo: OcnRulesListRepository = mockk()
    private val proxyResourceRepo: ProxyResourceRepository = mockk()
    private val httpService: HttpService = mockk()
    private val registry: RegistryFacade = mockk()
    private val walletService: WalletService = mockk()
    private val properties: NodeProperties = mockk()

    private val routingService: RoutingService

    init {
        routingService = RoutingService(
                platformRepo,
                roleRepo,
                endpointRepo,
                proxyResourceRepo,
                ocnRulesListRepo,
                registry,
                httpService,
                walletService,
                properties)
    }

    @Test
    fun `prepareLocalPlatformRequest without proxy`() {
        val request = OcpiRequestVariables(
                module = ModuleID.TOKENS,
                method = HttpMethod.GET,
                interfaceRole = InterfaceRole.RECEIVER,
                headers = OcnHeaders(
                        authorization = "Token token-c",
                        requestID = generateUUIDv4Token(),
                        correlationID = generateUUIDv4Token(),
                        sender = BasicRole("SNC", "DE"),
                        receiver = BasicRole("ABC", "CH")),
                urlPathVariables = "DE/SNC/abc123",
                urlEncodedParams = mapOf("type" to TokenType.APP_USER))

        every { routingService.getPlatformID(request.headers.receiver) } returns 6L
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
        assertThat(headers.correlationID).isEqualTo(request.headers.correlationID)
        assertThat(headers.sender.country).isEqualTo(request.headers.sender.country)
        assertThat(headers.sender.id).isEqualTo(request.headers.sender.id)
        assertThat(headers.receiver.country).isEqualTo(request.headers.receiver.country)
        assertThat(headers.receiver.id).isEqualTo(request.headers.receiver.id)
    }


    @Test
    fun `prepareLocalPlatformRequest with proxy`() {
        val request = OcpiRequestVariables(
                module = ModuleID.CDRS,
                method = HttpMethod.GET,
                interfaceRole = InterfaceRole.SENDER,
                headers = OcnHeaders(
                        authorization = "Token token-c",
                        requestID = generateUUIDv4Token(),
                        correlationID = generateUUIDv4Token(),
                        sender = BasicRole("SNC", "DE"),
                        receiver = BasicRole("ABC", "CH")),
                urlPathVariables = "67")

        every { routingService.getPlatformID(request.headers.receiver) } returns 126L
        every { routingService.getProxyResource(
                id = "67",
                sender = request.headers.sender,
                receiver = request.headers.receiver)
        } returns "https://cpo.com/cdrs?limit=20"

        every { platformRepo.findById(126L).get() } returns PlatformEntity(
                status = ConnectionStatus.CONNECTED,
                auth = Auth(tokenB = "0102030405", tokenC = generateUUIDv4Token()))

        val (url, headers) = routingService.prepareLocalPlatformRequest(request, proxied = true)

        assertThat(url).isEqualTo("https://cpo.com/cdrs?limit=20")
        assertThat(headers.authorization).isEqualTo("Token 0102030405")
        assertThat(headers.requestID.length).isEqualTo(36)
        assertThat(headers.correlationID).isEqualTo(request.headers.correlationID)
        assertThat(headers.sender.country).isEqualTo(request.headers.sender.country)
        assertThat(headers.sender.id).isEqualTo(request.headers.sender.id)
        assertThat(headers.receiver.country).isEqualTo(request.headers.receiver.country)
        assertThat(headers.receiver.id).isEqualTo(request.headers.receiver.id)
    }


    @Test
    fun `prepareRemotePlatformRequest without proxy`() {
        val request = OcpiRequestVariables(
                module = ModuleID.TOKENS,
                method = HttpMethod.GET,
                interfaceRole = InterfaceRole.RECEIVER,
                headers = OcnHeaders(
                        authorization = "Token token-c",
                        requestID = generateUUIDv4Token(),
                        correlationID = generateUUIDv4Token(),
                        sender = BasicRole("SNC", "DE"),
                        receiver = BasicRole("ABC", "CH")),
                urlPathVariables = "DE/SNC/abc123",
                urlEncodedParams = mapOf("type" to TokenType.APP_USER))

        val sig = "0x9955af11969a2d2a7f860cb00e6a00cfa7c581f5df2dbe8ea16700b33f4b4b9" +
                "b69f945012f7ea7d3febf11eb1b78e1adc2d1c14c2cf48b25000938cc1860c83e01"

        val modifiedRequest = request.copy(headers = request.headers.copy(authorization = ""))

        every { registry.nodeURLOf(
                request.headers.receiver.country.toByteArray(),
                request.headers.receiver.id.toByteArray()).sendAsync().get() } returns "https://ocn.node.net"

        val jsonString = jacksonObjectMapper().writeValueAsString(modifiedRequest)
        every { httpService.mapper.writeValueAsString(modifiedRequest) } returns jsonString
        every { walletService.sign(jsonString) } returns sig

        val (url, headers, body) = routingService.prepareRemotePlatformRequest(modifiedRequest)

        assertThat(url).isEqualTo("https://ocn.node.net")
        assertThat(headers.requestID.length).isEqualTo(36)
        assertThat(headers.signature).isEqualTo(sig)
        assertThat(body).isEqualTo(jsonString)
    }


    @Test
    fun `prepareRemotePlatformRequest with proxy`() {
        val request = OcpiRequestVariables(
                module = ModuleID.SESSIONS,
                method = HttpMethod.GET,
                interfaceRole = InterfaceRole.SENDER,
                headers = OcnHeaders(
                        authorization = "Token token-c",
                        requestID = generateUUIDv4Token(),
                        correlationID = generateUUIDv4Token(),
                        sender = BasicRole("SNC", "DE"),
                        receiver = BasicRole("ABC", "CH")),
                urlPathVariables = "45")

        val sig = "0x9955af11969a2d2a7f860cb00e6a00cfa7c581f5df2dbe8ea16700b33f4b4b9" +
                "b69f945012f7ea7d3febf11eb1b78e1adc2d1c14c2cf48b25000938cc1860c83e01"

        val modifiedRequest = request.copy(
                headers = request.headers.copy(authorization = ""),
                proxyResource = "https://actual.cpo.com/ocpi/sender/2.2/sessions?limit=10&offset=50; rel =\"next\"")

        every { registry.nodeURLOf(
                request.headers.receiver.country.toByteArray(),
                request.headers.receiver.id.toByteArray()).sendAsync().get() } returns "https://ocn-node.provider.net"

        every { routingService.getProxyResource("45", request.headers.sender, request.headers.receiver) } returns
                "https://actual.cpo.com/ocpi/sender/2.2/sessions?limit=10&offset=50; rel =\"next\""

        val jsonString = jacksonObjectMapper().writeValueAsString(modifiedRequest)
        every { httpService.mapper.writeValueAsString(modifiedRequest) } returns jsonString
        every { walletService.sign(jsonString) } returns sig

        val (url, headers, body) = routingService.prepareRemotePlatformRequest(modifiedRequest, proxied = true)

        assertThat(url).isEqualTo("https://ocn-node.provider.net")
        assertThat(headers.requestID.length).isEqualTo(36)
        assertThat(headers.signature).isEqualTo(sig)
        assertThat(body).isEqualTo(jsonString)
    }


    @Test
    fun `prepareRemotePlatformRequest with altered body`() {
        val body = CancelReservation(
                responseURL = "https://some.emsp.com/async-response/12",
                reservationID = "010203040506070809")

        val request = OcpiRequestVariables(
                module = ModuleID.COMMANDS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.POST,
                headers = OcnHeaders(
                        authorization = "Token token-c",
                        requestID = "123",
                        correlationID = "456",
                        sender = BasicRole("EMY", "DE"),
                        receiver = BasicRole("ING", "DE")),
                urlPathVariables = "CANCEL_RESERVATION",
                proxyUID = "128",
                proxyResource = body.responseURL,
                body = body)

        val sig = "0x9955af11969a2d2a7f860cb00e6a00cfa7c581f5df2dbe8ea16700b33f4b4b9" +
                "b69f945012f7ea7d3febf11eb1b78e1adc2d1c14c2cf48b25000938cc1860c83e01"

        every { registry.nodeURLOf(
                request.headers.receiver.country.toByteArray(),
                request.headers.receiver.id.toByteArray()).sendAsync().get() } returns "https://ocn-node.provider.net"

        val expectedAlteredBody = request.copy(
                headers = request.headers.copy(authorization = ""),
                body = body.copy(responseURL = "https://ocn-node.provider.net/commands/CANCEL_RESERVATION/128"))

        val jsonString = jacksonObjectMapper().writeValueAsString(expectedAlteredBody)
        every { httpService.mapper.writeValueAsString(expectedAlteredBody) } returns jsonString
        every { walletService.sign(jsonString) } returns sig

        val (url, headers, signedBody) = routingService.prepareRemotePlatformRequest(request) {
            request.copy(
                    body = body.copy(responseURL = urlJoin(it, "/commands/CANCEL_RESERVATION/${request.proxyUID}")))
        }

        assertThat(url).isEqualTo("https://ocn-node.provider.net")
        assertThat(headers.requestID.length).isEqualTo(36)
        assertThat(headers.signature).isEqualTo(sig)
        assertThat(signedBody).isEqualTo(jsonString)
        assertThat(signedBody.contains("\"response_url\":\"https://ocn-node.provider.net/commands/CANCEL_RESERVATION")).isEqualTo(true)
        assertThat(signedBody.contains("\"proxy_uid\":\"128\"")).isEqualTo(true)
    }


    @Test
    fun proxyPaginationHeaders() {
        val request = OcpiRequestVariables(
                module = ModuleID.TARIFFS,
                method = HttpMethod.GET,
                interfaceRole = InterfaceRole.SENDER,
                headers = OcnHeaders(
                        authorization = "Token token-c",
                        requestID = generateUUIDv4Token(),
                        correlationID = generateUUIDv4Token(),
                        sender = BasicRole("SNC", "DE"),
                        receiver = BasicRole("ABC", "CH")),
                urlEncodedParams = mapOf("limit" to 25))

        val link = "https://some.link.com/ocpi/tariffs?limit=25&offset=25; rel=\"next\""

        val responseHeaders = mapOf(
                "Link" to link,
                "X-Limit" to "25",
                "X-Total-Count" to "148")

        every { proxyResourceRepo.save<ProxyResourceEntity>(any())
        } returns ProxyResourceEntity(
                resource = link,
                sender = request.headers.sender,
                receiver = request.headers.receiver,
                id = 74L)

        every { properties.url } returns "https://some.node.ocn"

        val proxyHeaders = routingService.proxyPaginationHeaders(request, responseHeaders)
        assertThat(proxyHeaders.getFirst("Link")).isEqualTo("https://some.node.ocn/ocpi/sender/2.2/tariffs/page/74; rel=\"next\"")
        assertThat(proxyHeaders.getFirst("X-Limit")).isEqualTo("25")
        assertThat(proxyHeaders.getFirst("X-Total-Count")).isEqualTo("148")
    }


    @Test
    fun getProxyResource() {
        val id = "123"
        val sender = BasicRole("SNC", "DE")
        val receiver = BasicRole("DIY", "UK")
        val resource = "https://some.co/ocpi/tokens?limit=10; rel=\"next\""
        every { proxyResourceRepo.findByAlternativeUIDAndSenderAndReceiver(id, sender, receiver) } returns null
        every { proxyResourceRepo.findByIdAndSenderAndReceiver(123L, sender, receiver)?.resource } returns resource
        assertThat(routingService.getProxyResource(id, sender, receiver)).isEqualTo(resource)
    }


    @Test
    fun setProxyResource() {
        val resource = "https://some.co/ocpi/tokens?limit=10; rel=\"next\""
        val sender = BasicRole("SNC", "DE")
        val receiver = BasicRole("DIY", "UK")
        every { proxyResourceRepo.save(any<ProxyResourceEntity>()) } returns ProxyResourceEntity(
                resource = resource,
                sender = sender,
                receiver = receiver,
                id = 55L)
        assertThat(routingService.setProxyResource(resource, sender, receiver)).isEqualTo("55")
    }


    @Test
    fun isRoleKnown() {
        val role = BasicRole("ABC", "FR")
        every { roleRepo.existsByCountryCodeAndPartyIDAllIgnoreCase(role.country, role.id) } returns true
        assertThat(routingService.isRoleKnown(role)).isEqualTo(true)
    }


    @Test
    fun `isRoleKnownOnNetwork with belongsToMe flag returns true`() {
        val role = BasicRole("XYZ", "CH")
        val serverURL = "https://my.server.com"
        val serverEthAddress = "0xd13B66e4027cF454A4a1918394425b9969b99daB"
        every { registry.nodeURLOf(role.country.toByteArray(), role.id.toByteArray()).sendAsync().get() } returns serverURL
        every { registry.nodeAddressOf(role.country.toByteArray(), role.id.toByteArray()).sendAsync().get() } returns serverEthAddress
        every { properties.url } returns serverURL
        every { walletService.address } returns serverEthAddress
        assertThat(routingService.isRoleKnownOnNetwork(role)).isEqualTo(true)
    }


    @Test
    fun `isRoleKnownOnNetwork without belongsToMe flag returns true`() {
        val role = BasicRole("XYZ", "CH")
        val serverURL = "https://my.server.com"
        every { registry.nodeURLOf(role.country.toByteArray(), role.id.toByteArray()).sendAsync().get() } returns serverURL
        assertThat(routingService.isRoleKnownOnNetwork(role, belongsToMe = false)).isEqualTo(true)
    }


    @Test
    fun getPlatformID() {
        val role = RoleEntity(5L, Role.CPO, BusinessDetails("SENDER Co"), "SEN", "DE")
        every { roleRepo.findByCountryCodeAndPartyIDAllIgnoreCase(role.countryCode, role.partyID) } returns role
        assertThat(routingService.getPlatformID(BasicRole(role.partyID, role.countryCode))).isEqualTo(5L)
    }


    @Test
    fun getPlatformEndpoint() {
        val endpoint = EndpointEntity(6L, "tokens", InterfaceRole.SENDER, "https://some.url.com")
        every { endpointRepo.findByPlatformIDAndIdentifierAndRole(
                platformID = endpoint.platformID,
                identifier = endpoint.identifier,
                Role = InterfaceRole.SENDER) } returns endpoint
        assertThat(routingService.getPlatformEndpoint(
                platformID = endpoint.platformID,
                module = ModuleID.TOKENS,
                interfaceRole = InterfaceRole.SENDER).url).isEqualTo(endpoint.url)
    }


    @Test
    fun getRemoteNodeURL() {
        val role = BasicRole("XXX", "NL")
        every { registry.nodeURLOf(role.country.toByteArray(), role.id.toByteArray()).sendAsync().get() } returns "https://some.node.com"
        assertThat(routingService.getRemoteNodeUrl(role)).isEqualTo("https://some.node.com")
    }


    @Test
    fun `validateSender with auth only`() {
        val platform = PlatformEntity(id = 3L)
        every { platformRepo.findByAuth_TokenC("0102030405") } returns platform
        routingService.validateSender("Token 0102030405")
    }


    @Test
    fun `validateSender with auth and role`() {
        val role = BasicRole("YUT", "BE")
        val platform = PlatformEntity(id = 3L)
        every { platformRepo.findByAuth_TokenC("0102030405") } returns platform
        every { roleRepo.existsByPlatformIDAndCountryCodeAndPartyIDAllIgnoreCase(3L, role.country, role.id) } returns true
        routingService.validateSender("Token 0102030405", role)
    }


    @Test
    fun `validateReceiver should return LOCAL`() {
        val role = BasicRole("SNC", "DE")
        every { roleRepo.existsByCountryCodeAndPartyIDAllIgnoreCase(role.country, role.id) } returns true
        assertThat(routingService.validateReceiver(role)).isEqualTo(Receiver.LOCAL)
    }


    @Test
    fun `validateReceiver should return REMOTE`() {
        val role = BasicRole("SNC", "DE")
        every { roleRepo.existsByCountryCodeAndPartyIDAllIgnoreCase(role.country, role.id) } returns false
        every { registry.nodeURLOf(role.country.toByteArray(), role.id.toByteArray()).sendAsync().get() } returns "http://localhost:8080"
        assertThat(routingService.validateReceiver(role)).isEqualTo(Receiver.REMOTE)
    }

}