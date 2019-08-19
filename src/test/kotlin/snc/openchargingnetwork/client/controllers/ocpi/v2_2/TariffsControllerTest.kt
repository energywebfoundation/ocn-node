package snc.openchargingnetwork.client.controllers.ocpi.v2_2

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import org.hamcrest.Matchers
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import snc.openchargingnetwork.client.data.exampleTariff
import snc.openchargingnetwork.client.models.*
import snc.openchargingnetwork.client.models.ocpi.*
import snc.openchargingnetwork.client.services.HttpRequestService
import snc.openchargingnetwork.client.services.RoutingService
import snc.openchargingnetwork.client.tools.generateUUIDv4Token
import snc.openchargingnetwork.client.tools.getTimestamp

@WebMvcTest(TariffsController::class)
class TariffsControllerTest(@Autowired val mockMvc: MockMvc) {

    @MockkBean
    lateinit var routingService: RoutingService

    @MockkBean
    lateinit var httpService: HttpRequestService


    @Test
    fun `When GET sender tariffs return paginated tariffs list`() {

        val dateFrom = getTimestamp()

        val sender = BasicRole("EMY", "DE")
        val receiver = BasicRole("BSE", "DE")

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.TARIFFS,
                interfaceRole = InterfaceRole.SENDER,
                method = HttpMethod.GET,
                requestID = generateUUIDv4Token(),
                correlationID = generateUUIDv4Token(),
                sender = sender,
                receiver = receiver,
                urlEncodedParameters = OcpiRequestParameters(limit = 10),
                expectedResponseType = OcpiResponseDataType.TARIFF_ARRAY)

        val url = "https://ocpi.emsp.com/2.2/tariffs"

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
                "Link" to "https://ocpi.cpo.com/locations?limit=10&offset=10; rel=\"next\"",
                "X-Limit" to "10",
                "X-Total-Count" to "23")

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
                body = OcpiResponse(statusCode = 1000, data = arrayOf(exampleTariff)))

        val httpHeaders = HttpHeaders()
        httpHeaders["Link"] = "https://client.ocn.co/ocpi/sender/2.2/tariffs/page/39; rel=\"next\""
        httpHeaders["X-Limit"] = responseHeaders["X-Limit"]
        httpHeaders["X-Total-Count"] = responseHeaders["X-Total-Count"]

        every { routingService.proxyPaginationHeaders(
                responseHeaders = responseHeaders,
                proxyEndpoint = "/ocpi/sender/2.2/tariffs/page",
                sender = sender,
                receiver = receiver) } returns httpHeaders

        mockMvc.perform(get("/ocpi/sender/2.2/tariffs")
                .header("Authorization", "Token token-c")
                .header("X-Request-ID", requestVariables.requestID)
                .header("X-Correlation-ID", requestVariables.correlationID)
                .header("OCPI-from-country-code", sender.country)
                .header("OCPI-from-party-id", sender.id)
                .header("OCPI-to-country-code", receiver.country)
                .header("OCPI-to-party-id", receiver.id)
                .param("limit", "10"))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(MockMvcResultMatchers.header().string("Link", "https://client.ocn.co/ocpi/sender/2.2/tariffs/page/39; rel=\"next\""))
                .andExpect(MockMvcResultMatchers.header().string("X-Limit", "10"))
                .andExpect(MockMvcResultMatchers.header().string("X-Total-Count", "23"))
                .andExpect(MockMvcResultMatchers.jsonPath("\$.status_code").value(OcpiStatus.SUCCESS.code))
                .andExpect(MockMvcResultMatchers.jsonPath("\$.status_message").doesNotExist())
                .andExpect(MockMvcResultMatchers.jsonPath("\$.timestamp").isString)
                .andExpect(MockMvcResultMatchers.jsonPath("\$.data").isArray)
                .andExpect(MockMvcResultMatchers.jsonPath("\$.data", Matchers.hasSize<Array<Tariff>>(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("\$.data[0].id").value(exampleTariff.id))
                .andExpect(MockMvcResultMatchers.jsonPath("\$.data[0].party_id").value(exampleTariff.partyID))
    }


    @Test
    fun `When GET sender Tariffs page should return proxied tariff list`() {

        val sender = BasicRole("EMY", "DE")
        val receiver = BasicRole("BSE", "DE")

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.TARIFFS,
                interfaceRole = InterfaceRole.SENDER,
                method = HttpMethod.GET,
                requestID = generateUUIDv4Token(),
                correlationID = generateUUIDv4Token(),
                sender = sender,
                receiver = receiver,
                urlPathVariables = "39",
                expectedResponseType = OcpiResponseDataType.TARIFF_ARRAY)

        val url = "https://ocpi.cpo.com/tariffs?limit=10?offset=10"

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
                "Link" to "https://some.emsp.com/actual/tariffs?limit=10&offset=10; rel=\"next\"",
                "X-Limit" to "10",
                "X-Total-Count" to "23")

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
                body = OcpiResponse(statusCode = 1000, data = arrayOf(exampleTariff)))

        val httpHeaders = HttpHeaders()
        httpHeaders["Link"] = "https://client.ocn.co/ocpi/sender/2.2/tariffs/page/40; rel=\"next\""
        httpHeaders["X-Limit"] = responseHeaders["X-Limit"]

        every { routingService.deleteProxyResource(requestVariables.urlPathVariables!!) } just Runs

        every { routingService.proxyPaginationHeaders(
                responseHeaders = responseHeaders,
                proxyEndpoint = "/ocpi/sender/2.2/tariffs/page",
                sender = sender,
                receiver = receiver) } returns httpHeaders

        mockMvc.perform(get("/ocpi/sender/2.2/tariffs/page/${requestVariables.urlPathVariables}")
                .header("Authorization", "Token token-c")
                .header("X-Request-ID", requestVariables.requestID)
                .header("X-Correlation-ID", requestVariables.correlationID)
                .header("OCPI-from-country-code", requestVariables.sender.country)
                .header("OCPI-from-party-id", requestVariables.sender.id)
                .header("OCPI-to-country-code", requestVariables.receiver.country)
                .header("OCPI-to-party-id", requestVariables.receiver.id)
                .param("limit", "100"))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andExpect(MockMvcResultMatchers.header().string("Link", "https://client.ocn.co/ocpi/sender/2.2/tariffs/page/40; rel=\"next\""))
                .andExpect(MockMvcResultMatchers.header().string("X-Limit", "10"))
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(MockMvcResultMatchers.jsonPath("\$.status_code").value(1000))
                .andExpect(MockMvcResultMatchers.jsonPath("\$.status_message").doesNotExist())
                .andExpect(MockMvcResultMatchers.jsonPath("\$.data").isArray)
                .andExpect(MockMvcResultMatchers.jsonPath("\$.data", Matchers.hasSize<Array<Tariff>>(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("\$.data[0].id").value(exampleTariff.id))
                .andExpect(MockMvcResultMatchers.jsonPath("\$.data[0].party_id").value(exampleTariff.partyID))
                .andExpect(MockMvcResultMatchers.jsonPath("\$.timestamp").isString)
    }

}