package snc.openchargingnetwork.client.controllers.ocpi.v2_2

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
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
import snc.openchargingnetwork.client.config.Properties
import snc.openchargingnetwork.client.data.exampleCDR
import snc.openchargingnetwork.client.services.RoutingService
import snc.openchargingnetwork.client.tools.generateUUIDv4Token
import org.hamcrest.Matchers.hasSize
import org.springframework.http.HttpHeaders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import snc.openchargingnetwork.client.models.*
import snc.openchargingnetwork.client.models.ocpi.*
import snc.openchargingnetwork.client.services.HttpRequestService

@WebMvcTest(CdrsController::class)
class CdrsControllerTest(@Autowired val mockMvc: MockMvc) {

    @MockkBean
    lateinit var routingService: RoutingService

    @MockkBean
    lateinit var httpService: HttpRequestService

    @MockkBean
    lateinit var properties: Properties


    @Test
    fun `When GET sender CDRs should return paginated response`() {

        val sender = BasicRole("EMY", "DE")
        val receiver = BasicRole("ZTP", "CH")

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.CDRS,
                interfaceRole = InterfaceRole.SENDER,
                method = HttpMethod.GET,
                requestID = generateUUIDv4Token(),
                correlationID = generateUUIDv4Token(),
                sender = sender,
                receiver = receiver,
                urlEncodedParameters = OcpiRequestParameters(limit = 100),
                expectedResponseType = OcpiResponseDataType.CDR_ARRAY)

        val url = "https://some.emsp.com/ocpi/emsp/2.2/cdrs"

        val headers = OcpiRequestHeaders(
                authorization = "Token token-b",
                requestID = generateUUIDv4Token(),
                correlationID = requestVariables.correlationID,
                ocpiFromCountryCode = sender.country,
                ocpiFromPartyID = sender.id,
                ocpiToCountryCode = receiver.country,
                ocpiToPartyID = receiver.id)

        every { routingService.validateSender("Token token-c", sender) } just Runs
        every { routingService.validateReceiver(receiver) } returns OcpiRequestType.LOCAL
        every { routingService.prepareLocalPlatformRequest(requestVariables) } returns Pair(url, headers)

        val responseHeaders = mapOf(
                "Link" to "https://some.cpo.com/actual/cdr/page/2; rel=\"next\"",
                "X-Limit" to "100")

        every {

            httpService.makeRequest(
                method = requestVariables.method,
                url = url,
                headers = headers,
                params = requestVariables.urlEncodedParameters,
                expectedDataType = requestVariables.expectedResponseType)

        } returns HttpResponse(
                statusCode = 200,
                headers = responseHeaders,
                body = OcpiResponse(statusCode = 1000, data = arrayOf(exampleCDR)))

        val httpHeaders = HttpHeaders()
        httpHeaders["Link"] = "https://client.ocn.co/ocpi/sender/2.2/cdrs/page/43; rel=\"next\""
        httpHeaders["X-Limit"] = responseHeaders["X-Limit"]

        every { routingService.proxyPaginationHeaders(
                responseHeaders = responseHeaders,
                proxyEndpoint = "/ocpi/sender/2.2/cdrs/page",
                sender = sender,
                receiver = receiver) } returns httpHeaders

        mockMvc.perform(get("/ocpi/sender/2.2/cdrs")
                .header("Authorization", "Token token-c")
                .header("X-Request-ID", requestVariables.requestID)
                .header("X-Correlation-ID", requestVariables.correlationID)
                .header("OCPI-from-country-code", requestVariables.sender.country)
                .header("OCPI-from-party-id", requestVariables.sender.id)
                .header("OCPI-to-country-code", requestVariables.receiver.country)
                .header("OCPI-to-party-id", requestVariables.receiver.id)
                .param("limit", "100"))
                .andExpect(status().isOk)
                .andExpect(header().string("Link", "https://client.ocn.co/ocpi/sender/2.2/cdrs/page/43; rel=\"next\""))
                .andExpect(header().string("X-Limit", "100"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
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
                requestID = generateUUIDv4Token(),
                correlationID = generateUUIDv4Token(),
                sender = sender,
                receiver = receiver,
                urlPathVariables = "67",
                expectedResponseType = OcpiResponseDataType.CDR_ARRAY)

        val url = "https://some.emsp.com/actual/cdr/page/2"

        val headers = OcpiRequestHeaders(
                authorization = "Token token-b",
                requestID = generateUUIDv4Token(),
                correlationID = requestVariables.correlationID,
                ocpiFromCountryCode = sender.country,
                ocpiFromPartyID = sender.id,
                ocpiToCountryCode = receiver.country,
                ocpiToPartyID = receiver.id)

        every { routingService.validateSender("Token token-c", sender) } just Runs
        every { routingService.validateReceiver(receiver) } returns OcpiRequestType.LOCAL
        every { routingService.prepareLocalPlatformRequest(requestVariables, proxied = true) } returns Pair(url, headers)

        val responseHeaders = mapOf(
                "Link" to "https://some.emsp.com/actual/cdr/page/3; rel=\"next\"",
                "X-Limit" to "100")

        every {

            httpService.makeRequest(
                    method = requestVariables.method,
                    url = url,
                    headers = headers,
                    params = requestVariables.urlEncodedParameters,
                    expectedDataType = requestVariables.expectedResponseType)

        } returns HttpResponse(
                statusCode = 200,
                headers = responseHeaders,
                body = OcpiResponse(statusCode = 1000, data = arrayOf(exampleCDR)))

        val httpHeaders = HttpHeaders()
        httpHeaders["Link"] = "https://client.ocn.co/ocpi/sender/2.2/cdrs/page/68; rel=\"next\""
        httpHeaders["X-Limit"] = responseHeaders["X-Limit"]

        every { routingService.deleteProxyResource(requestVariables.urlPathVariables!!) } just Runs

        every { routingService.proxyPaginationHeaders(
                responseHeaders = responseHeaders,
                proxyEndpoint = "/ocpi/sender/2.2/cdrs/page",
                sender = sender,
                receiver = receiver) } returns httpHeaders

        mockMvc.perform(get("/ocpi/sender/2.2/cdrs/page/${requestVariables.urlPathVariables}")
                .header("Authorization", "Token token-c")
                .header("X-Request-ID", requestVariables.requestID)
                .header("X-Correlation-ID", requestVariables.correlationID)
                .header("OCPI-from-country-code", requestVariables.sender.country)
                .header("OCPI-from-party-id", requestVariables.sender.id)
                .header("OCPI-to-country-code", requestVariables.receiver.country)
                .header("OCPI-to-party-id", requestVariables.receiver.id)
                .param("limit", "100"))
                .andExpect(status().isOk)
                .andExpect(header().string("Link", "https://client.ocn.co/ocpi/sender/2.2/cdrs/page/68; rel=\"next\""))
                .andExpect(header().string("X-Limit", "100"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
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
                requestID = generateUUIDv4Token(),
                correlationID = generateUUIDv4Token(),
                sender = sender,
                receiver = receiver,
                urlPathVariables = "6534",
                expectedResponseType = OcpiResponseDataType.CDR)

        val url = "https://some.cpo.com/ocpi/emsp/2.2/cdrs"

        val headers = OcpiRequestHeaders(
                authorization = "Token token-b",
                requestID = generateUUIDv4Token(),
                correlationID = requestVariables.correlationID,
                ocpiFromCountryCode = sender.country,
                ocpiFromPartyID = sender.id,
                ocpiToCountryCode = receiver.country,
                ocpiToPartyID = receiver.id)

        every { routingService.validateSender("Token token-c", sender) } just Runs
        every { routingService.validateReceiver(receiver) } returns OcpiRequestType.LOCAL
        every { routingService.prepareLocalPlatformRequest(requestVariables, proxied = true) } returns Pair(url, headers)

        every {

            httpService.makeRequest(
                    method = requestVariables.method,
                    url = url,
                    headers = headers,
                    params = requestVariables.urlEncodedParameters,
                    expectedDataType = requestVariables.expectedResponseType)

        } returns HttpResponse(
                statusCode = 200,
                headers = mapOf(),
                body = OcpiResponse(statusCode = 1000, data = exampleCDR))

        mockMvc.perform(get("/ocpi/receiver/2.2/cdrs/${requestVariables.urlPathVariables}")
                .header("Authorization", "Token token-c")
                .header("X-Request-ID", requestVariables.requestID)
                .header("X-Correlation-ID", requestVariables.correlationID)
                .header("OCPI-from-country-code", requestVariables.sender.country)
                .header("OCPI-from-party-id", requestVariables.sender.id)
                .header("OCPI-to-country-code", requestVariables.receiver.country)
                .header("OCPI-to-party-id", requestVariables.receiver.id))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
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
                requestID = generateUUIDv4Token(),
                correlationID = generateUUIDv4Token(),
                sender = sender,
                receiver = receiver,
                body = exampleCDR,
                expectedResponseType = OcpiResponseDataType.NOTHING)

        val url = "https://some.cpo.com/ocpi/emsp/2.2/cdrs"

        val headers = OcpiRequestHeaders(
                authorization = "Token token-b",
                requestID = generateUUIDv4Token(),
                correlationID = requestVariables.correlationID,
                ocpiFromCountryCode = sender.country,
                ocpiFromPartyID = sender.id,
                ocpiToCountryCode = receiver.country,
                ocpiToPartyID = receiver.id)

        every { routingService.validateSender("Token token-c", sender) } just Runs
        every { routingService.validateReceiver(receiver) } returns OcpiRequestType.LOCAL
        every { routingService.prepareLocalPlatformRequest(requestVariables) } returns Pair(url, headers)

        val locationHeader = "https://real.msp.net/path/to/location/1"

        every {

            httpService.makeRequest(
                    method = requestVariables.method,
                    url = url,
                    headers = headers,
                    params = requestVariables.urlEncodedParameters,
                    expectedDataType = requestVariables.expectedResponseType)

        } returns HttpResponse(
                statusCode = 200,
                headers = mapOf("Location" to locationHeader),
                body = OcpiResponse(statusCode = 1000, data = null))

        every { routingService.setProxyResource(locationHeader, sender, receiver) } returns 6545L
        every { properties.url } returns "https://super.hub.net"

        mockMvc.perform(post("/ocpi/receiver/2.2/cdrs")
                .header("Authorization", "Token token-c")
                .header("X-Request-ID", requestVariables.requestID)
                .header("X-Correlation-ID", requestVariables.correlationID)
                .header("OCPI-from-country-code", requestVariables.sender.country)
                .header("OCPI-from-party-id", requestVariables.sender.id)
                .header("OCPI-to-country-code", requestVariables.receiver.country)
                .header("OCPI-to-party-id", requestVariables.receiver.id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jacksonObjectMapper().writeValueAsString(exampleCDR)))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("\$.status_code").value(1000))
                .andExpect(jsonPath("\$.status_message").doesNotExist())
                .andExpect(jsonPath("\$.data").doesNotExist())
                .andExpect(jsonPath("\$.timestamp").isString)
                .andExpect(header().string("Location", "https://super.hub.net/ocpi/receiver/2.2/cdrs/6545"))
    }
}