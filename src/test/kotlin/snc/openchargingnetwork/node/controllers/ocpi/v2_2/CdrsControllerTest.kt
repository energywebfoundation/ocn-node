package snc.openchargingnetwork.node.controllers.ocpi.v2_2

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.ninjasquad.springmockk.MockkBean
import io.mockk.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import snc.openchargingnetwork.node.config.NodeProperties
import snc.openchargingnetwork.node.data.exampleCDR
import snc.openchargingnetwork.node.services.RoutingService
import snc.openchargingnetwork.node.tools.generateUUIDv4Token
import org.hamcrest.Matchers.hasSize
import org.springframework.http.HttpHeaders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import snc.openchargingnetwork.node.models.*
import snc.openchargingnetwork.node.models.ocpi.*
import snc.openchargingnetwork.node.services.HttpService

@WebMvcTest(CdrsController::class)
class CdrsControllerTest(@Autowired val mockMvc: MockMvc) {

    @MockkBean
    lateinit var routingService: RoutingService

    @MockkBean
    lateinit var httpService: HttpService

    @MockkBean
    lateinit var properties: NodeProperties

    private val mapper = jacksonObjectMapper()


    @Test
    fun `When GET sender CDRs should return paginated response`() {

        val sender = BasicRole("EMY", "DE")
        val receiver = BasicRole("ZTP", "CH")

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.CDRS,
                interfaceRole = InterfaceRole.SENDER,
                method = HttpMethod.GET,
                headers = OcpiRequestHeaders(
                        requestID = generateUUIDv4Token(),
                        correlationID = generateUUIDv4Token(),
                        sender = sender,
                        receiver = receiver),
                urlEncodedParams = OcpiRequestParameters(limit = 100))

        val url = "https://some.emsp.com/ocpi/emsp/2.2/cdrs"

        val forwardingHeaders = requestVariables.headers.copy(
                authorization = "Token token-b",
                requestID = generateUUIDv4Token())

        every { routingService.validateSender("Token token-c", sender) } just Runs
        every { routingService.validateReceiver(receiver) } returns Receiver.LOCAL
        every { routingService.prepareLocalPlatformRequest(requestVariables) } returns Pair(url, forwardingHeaders)

        val responseHeaders = mapOf(
                "Link" to "https://some.cpo.com/actual/cdr/page/2; rel=\"next\"",
                "X-Limit" to "100")

        every {
            httpService.makeOcpiRequest<Array<CDR>>(HttpMethod.GET, url, forwardingHeaders.toMap(), requestVariables.urlEncodedParams?.toMap()!!)
        } returns HttpResponse(
                    statusCode = 200,
                    headers = responseHeaders,
                    body = OcpiResponse(statusCode = 1000, data = arrayOf(exampleCDR)))


        val httpHeaders = HttpHeaders()
        httpHeaders["Link"] = "https://node.ocn.co/ocpi/sender/2.2/cdrs/page/43; rel=\"next\""
        httpHeaders["X-Limit"] = responseHeaders["X-Limit"]

        every { routingService.proxyPaginationHeaders(
                request = requestVariables,
                responseHeaders = responseHeaders) } returns httpHeaders

        mockMvc.perform(get("/ocpi/sender/2.2/cdrs")
                .header("Authorization", "Token token-c")
                .header("X-Request-ID", requestVariables.headers.requestID)
                .header("X-Correlation-ID", requestVariables.headers.correlationID)
                .header("OCPI-from-country-code", requestVariables.headers.sender.country)
                .header("OCPI-from-party-id", requestVariables.headers.sender.id)
                .header("OCPI-to-country-code", requestVariables.headers.receiver.country)
                .header("OCPI-to-party-id", requestVariables.headers.receiver.id)
                .param("limit", "100"))
                .andExpect(status().isOk)
                .andExpect(header().string("Link", "https://node.ocn.co/ocpi/sender/2.2/cdrs/page/43; rel=\"next\""))
                .andExpect(header().string("X-Limit", "100"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("\$.status_code").value(1000))
                .andExpect(jsonPath("\$.status_message").doesNotExist())
                .andExpect(jsonPath("\$.data").isArray)
                .andExpect(jsonPath("\$.data", hasSize<Array<CDR>>(1)))
                .andExpect(jsonPath("\$.data[0].id").value(exampleCDR.id))
                .andExpect(jsonPath("\$.data[0].party_id").value(exampleCDR.partyID))
                .andExpect(jsonPath("\$.timestamp").isString)

    }


    @Test
    fun `When GET sender CDRs page should return proxied cdrs list page`() {

        val sender = BasicRole("EMY", "DE")
        val receiver = BasicRole("ZTP", "CH")

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.CDRS,
                interfaceRole = InterfaceRole.SENDER,
                method = HttpMethod.GET,
                headers = OcpiRequestHeaders(
                        requestID = generateUUIDv4Token(),
                        correlationID = generateUUIDv4Token(),
                        sender = sender,
                        receiver = receiver),
                urlPathVariables = "67")

        val url = "https://some.emsp.com/actual/cdr/page/2"

        val forwardingHeaders = requestVariables.headers.copy(
                authorization = "Token token-b",
                requestID = generateUUIDv4Token())

        every { routingService.validateSender("Token token-c", sender) } just Runs
        every { routingService.validateReceiver(receiver) } returns Receiver.LOCAL
        every { routingService.prepareLocalPlatformRequest(requestVariables, proxied = true) } returns Pair(url, forwardingHeaders)

        val responseHeaders = mapOf(
                "Link" to "https://some.emsp.com/actual/cdr/page/3; rel=\"next\"",
                "X-Limit" to "100")

        every {
            httpService.makeOcpiRequest<Array<CDR>>(HttpMethod.GET, url, forwardingHeaders.toMap())
        } returns HttpResponse(
                statusCode = 200,
                headers = responseHeaders,
                body = OcpiResponse(statusCode = 1000, data = arrayOf(exampleCDR)))

        val httpHeaders = HttpHeaders()
        httpHeaders["Link"] = "https://node.ocn.co/ocpi/sender/2.2/cdrs/page/68; rel=\"next\""
        httpHeaders["X-Limit"] = responseHeaders["X-Limit"]

        every { routingService.deleteProxyResource(requestVariables.urlPathVariables!!) } just Runs

        every { routingService.proxyPaginationHeaders(
                request = requestVariables,
                responseHeaders = responseHeaders) } returns httpHeaders

        mockMvc.perform(get("/ocpi/sender/2.2/cdrs/page/${requestVariables.urlPathVariables}")
                .header("Authorization", "Token token-c")
                .header("X-Request-ID", requestVariables.headers.requestID)
                .header("X-Correlation-ID", requestVariables.headers.correlationID)
                .header("OCPI-from-country-code", requestVariables.headers.sender.country)
                .header("OCPI-from-party-id", requestVariables.headers.sender.id)
                .header("OCPI-to-country-code", requestVariables.headers.receiver.country)
                .header("OCPI-to-party-id", requestVariables.headers.receiver.id)
                .param("limit", "100"))
                .andExpect(status().isOk)
                .andExpect(header().string("Link", "https://node.ocn.co/ocpi/sender/2.2/cdrs/page/68; rel=\"next\""))
                .andExpect(header().string("X-Limit", "100"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("\$.status_code").value(1000))
                .andExpect(jsonPath("\$.status_message").doesNotExist())
                .andExpect(jsonPath("\$.data").isArray)
                .andExpect(jsonPath("\$.data", hasSize<Array<CDR>>(1)))
                .andExpect(jsonPath("\$.data[0].id").value(exampleCDR.id))
                .andExpect(jsonPath("\$.data[0].party_id").value(exampleCDR.partyID))
                .andExpect(jsonPath("\$.timestamp").isString)
    }


    @Test
    fun `When GET receiver CDRs should return single CDR`() {

        val sender = BasicRole("ZTP", "CH")
        val receiver = BasicRole("EMY", "DE")

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.CDRS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.GET,
                headers = OcpiRequestHeaders(
                        requestID = generateUUIDv4Token(),
                        correlationID = generateUUIDv4Token(),
                        sender = sender,
                        receiver = receiver),
                urlPathVariables = "6534")

        val url = "https://some.cpo.com/ocpi/emsp/2.2/cdrs"

        val forwardingHeaders = requestVariables.headers.copy(
                authorization = "Token token-b",
                requestID = generateUUIDv4Token())

        every { routingService.validateSender("Token token-c", sender) } just Runs
        every { routingService.validateReceiver(receiver) } returns Receiver.LOCAL
        every { routingService.prepareLocalPlatformRequest(requestVariables, proxied = true) } returns Pair(url, forwardingHeaders)

        every {
            httpService.makeOcpiRequest<CDR>(HttpMethod.GET, url, forwardingHeaders.toMap())
        } returns HttpResponse(
                statusCode = 200,
                headers = mapOf(),
                body = OcpiResponse(statusCode = 1000, data = exampleCDR))

        mockMvc.perform(get("/ocpi/receiver/2.2/cdrs/${requestVariables.urlPathVariables}")
                .header("Authorization", "Token token-c")
                .header("X-Request-ID", requestVariables.headers.requestID)
                .header("X-Correlation-ID", requestVariables.headers.correlationID)
                .header("OCPI-from-country-code", requestVariables.headers.sender.country)
                .header("OCPI-from-party-id", requestVariables.headers.sender.id)
                .header("OCPI-to-country-code", requestVariables.headers.receiver.country)
                .header("OCPI-to-party-id", requestVariables.headers.receiver.id))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("\$.status_code").value(1000))
                .andExpect(jsonPath("\$.status_message").doesNotExist())
                .andExpect(jsonPath("\$.data.id").value(exampleCDR.id))
                .andExpect(jsonPath("\$.data.party_id").value(exampleCDR.partyID))
                .andExpect(jsonPath("\$.timestamp").isString)
    }


    @Test
    fun `when POST receiver cdrs should return Location header`() {

        val sender = BasicRole("ZTP", "CH")
        val receiver = BasicRole("EMY", "DE")

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.CDRS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.POST,
                headers = OcpiRequestHeaders(
                        requestID = generateUUIDv4Token(),
                        correlationID = generateUUIDv4Token(),
                        sender = sender,
                        receiver = receiver),
                body = exampleCDR)

        val url = "https://some.cpo.com/ocpi/emsp/2.2/cdrs"

        val forwardingHeaders = requestVariables.headers.copy(
                authorization = "Token token-b",
                requestID = generateUUIDv4Token())

        every { routingService.validateSender("Token token-c", sender) } just Runs
        every { routingService.validateReceiver(receiver) } returns Receiver.LOCAL
        every { routingService.prepareLocalPlatformRequest(requestVariables) } returns Pair(url, forwardingHeaders)

        val locationHeader = "https://real.msp.net/path/to/location/1"

        every { httpService.mapper } returns mapper

        val bodyString = mapper.writeValueAsString(requestVariables.body)
        val bodyMap: Map<String, Any> = mapper.readValue(bodyString)

        every {
            httpService.makeOcpiRequest<Unit>(HttpMethod.POST, url, forwardingHeaders.toMap(), json = bodyMap)
        } returns HttpResponse(
                statusCode = 200,
                headers = mapOf("Location" to locationHeader),
                body = OcpiResponse(statusCode = 1000))

        every { routingService.setProxyResource(locationHeader, sender, receiver) } returns "6545"
        every { properties.url } returns "https://super.hub.net"

        mockMvc.perform(post("/ocpi/receiver/2.2/cdrs")
                .header("Authorization", "Token token-c")
                .header("X-Request-ID", requestVariables.headers.requestID)
                .header("X-Correlation-ID", requestVariables.headers.correlationID)
                .header("OCPI-from-country-code", requestVariables.headers.sender.country)
                .header("OCPI-from-party-id", requestVariables.headers.sender.id)
                .header("OCPI-to-country-code", requestVariables.headers.receiver.country)
                .header("OCPI-to-party-id", requestVariables.headers.receiver.id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jacksonObjectMapper().writeValueAsString(exampleCDR)))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("\$.status_code").value(1000))
                .andExpect(jsonPath("\$.status_message").doesNotExist())
                .andExpect(jsonPath("\$.data").doesNotExist())
                .andExpect(jsonPath("\$.timestamp").isString)
                .andExpect(header().string("Location", "https://super.hub.net/ocpi/receiver/2.2/cdrs/6545"))
    }
}