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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import snc.openchargingnetwork.client.data.exampleTariff
import snc.openchargingnetwork.client.models.*
import snc.openchargingnetwork.client.models.ocpi.*
import snc.openchargingnetwork.client.services.HttpRequestService
import snc.openchargingnetwork.client.services.RoutingService
import snc.openchargingnetwork.client.tools.generateUUIDv4Token

@WebMvcTest(TariffsController::class)
class TariffsControllerTest(@Autowired val mockMvc: MockMvc) {

    @MockkBean
    lateinit var routingService: RoutingService

    @MockkBean
    lateinit var httpService: HttpRequestService


    @Test
    fun `When GET sender tariffs return paginated tariffs list`() {

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
                "Link" to "https://ocpi.cpo.com/tariffs?limit=10&offset=10; rel=\"next\"",
                "X-Limit" to "10",
                "X-Total-Count" to "23")

        every {

            httpService.makeRequest<Array<Tariff>>(
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

        every {
            routingService.proxyPaginationHeaders(
                    request = requestVariables,
                    responseHeaders = responseHeaders) } returns httpHeaders

        mockMvc.perform(get("/ocpi/sender/2.2/tariffs")
                .header("Authorization", "Token token-c")
                .header("X-Request-ID", requestVariables.requestID)
                .header("X-Correlation-ID", requestVariables.correlationID)
                .header("OCPI-from-country-code", sender.country)
                .header("OCPI-from-party-id", sender.id)
                .header("OCPI-to-country-code", receiver.country)
                .header("OCPI-to-party-id", receiver.id)
                .param("limit", "10"))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(header().string("Link", "https://client.ocn.co/ocpi/sender/2.2/tariffs/page/39; rel=\"next\""))
                .andExpect(header().string("X-Limit", "10"))
                .andExpect(header().string("X-Total-Count", "23"))
                .andExpect(jsonPath("\$.status_code").value(OcpiStatus.SUCCESS.code))
                .andExpect(jsonPath("\$.status_message").doesNotExist())
                .andExpect(jsonPath("\$.timestamp").isString)
                .andExpect(jsonPath("\$.data").isArray)
                .andExpect(jsonPath("\$.data", Matchers.hasSize<Array<Tariff>>(1)))
                .andExpect(jsonPath("\$.data[0].id").value(exampleTariff.id))
                .andExpect(jsonPath("\$.data[0].party_id").value(exampleTariff.partyID))
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

            httpService.makeRequest<Array<Tariff>>(
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
                request = requestVariables,
                responseHeaders = responseHeaders) } returns httpHeaders

        mockMvc.perform(get("/ocpi/sender/2.2/tariffs/page/${requestVariables.urlPathVariables}")
                .header("Authorization", "Token token-c")
                .header("X-Request-ID", requestVariables.requestID)
                .header("X-Correlation-ID", requestVariables.correlationID)
                .header("OCPI-from-country-code", requestVariables.sender.country)
                .header("OCPI-from-party-id", requestVariables.sender.id)
                .header("OCPI-to-country-code", requestVariables.receiver.country)
                .header("OCPI-to-party-id", requestVariables.receiver.id)
                .param("limit", "100"))
                .andExpect(status().isOk)
                .andExpect(header().string("Link", "https://client.ocn.co/ocpi/sender/2.2/tariffs/page/40; rel=\"next\""))
                .andExpect(header().string("X-Limit", "10"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("\$.status_code").value(1000))
                .andExpect(jsonPath("\$.status_message").doesNotExist())
                .andExpect(jsonPath("\$.data").isArray)
                .andExpect(jsonPath("\$.data", Matchers.hasSize<Array<Tariff>>(1)))
                .andExpect(jsonPath("\$.data[0].id").value(exampleTariff.id))
                .andExpect(jsonPath("\$.data[0].party_id").value(exampleTariff.partyID))
                .andExpect(jsonPath("\$.timestamp").isString)
    }


    @Test
    fun `When GET receiver Tariffs return single tariff object`() {

        val tariffID = "xxx-1234"

        val sender = BasicRole("BSE", "DE")
        val receiver = BasicRole("EMY", "DE")

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.TARIFFS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.GET,
                requestID = generateUUIDv4Token(),
                correlationID = generateUUIDv4Token(),
                sender = sender,
                receiver = receiver,
                urlPathVariables = "/${sender.country}/${sender.id}/$tariffID",
                expectedResponseType = OcpiResponseDataType.TARIFF)

        val url = "https://ocpi.cpo.com/2.2/tariffs/${sender.country}/${sender.id}/$tariffID"

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

        every {

            httpService.makeRequest<Tariff>(
                    method = requestVariables.method,
                    url = url,
                    headers = headers,
                    expectedDataType = requestVariables.expectedResponseType)

        } returns HttpResponse(
                statusCode = 200,
                headers = mapOf(),
                body = OcpiResponse(statusCode = 1000, data = exampleTariff))

        mockMvc.perform(get("/ocpi/receiver/2.2/tariffs/${sender.country}/${sender.id}/$tariffID")
                .header("Authorization", "Token token-c")
                .header("X-Request-ID", requestVariables.requestID)
                .header("X-Correlation-ID", requestVariables.correlationID)
                .header("OCPI-from-country-code", sender.country)
                .header("OCPI-from-party-id", sender.id)
                .header("OCPI-to-country-code", receiver.country)
                .header("OCPI-to-party-id", receiver.id))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("\$.status_code").value(OcpiStatus.SUCCESS.code))
                .andExpect(jsonPath("\$.status_message").doesNotExist())
                .andExpect(jsonPath("\$.timestamp").isString)
                .andExpect(jsonPath("\$.data.id").value(exampleTariff.id))
                .andExpect(jsonPath("\$.data.party_id").value(exampleTariff.partyID))
    }


    @Test
    fun `When PUT receiver Tariffs return OCPI success`() {

        val tariffID = "xxx-1234"

        val sender = BasicRole("BSE", "DE")
        val receiver = BasicRole("EMY", "DE")

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.TARIFFS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.PUT,
                requestID = generateUUIDv4Token(),
                correlationID = generateUUIDv4Token(),
                sender = sender,
                receiver = receiver,
                urlPathVariables = "/${sender.country}/${sender.id}/$tariffID",
                body = exampleTariff,
                expectedResponseType = OcpiResponseDataType.NOTHING)

        val url = "https://ocpi.cpo.com/2.2/tariffs/${sender.country}/${sender.id}/$tariffID"

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

        every {

            httpService.makeRequest<Nothing>(
                    method = requestVariables.method,
                    url = url,
                    headers = headers,
                    body = exampleTariff,
                    expectedDataType = requestVariables.expectedResponseType)

        } returns HttpResponse(
                statusCode = 200,
                headers = mapOf(),
                body = OcpiResponse(statusCode = 1000))

        mockMvc.perform(put("/ocpi/receiver/2.2/tariffs/${sender.country}/${sender.id}/$tariffID")
                .header("Authorization", "Token token-c")
                .header("X-Request-ID", requestVariables.requestID)
                .header("X-Correlation-ID", requestVariables.correlationID)
                .header("OCPI-from-country-code", sender.country)
                .header("OCPI-from-party-id", sender.id)
                .header("OCPI-to-country-code", receiver.country)
                .header("OCPI-to-party-id", receiver.id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jacksonObjectMapper().writeValueAsString(exampleTariff)))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("\$.status_code").value(OcpiStatus.SUCCESS.code))
                .andExpect(jsonPath("\$.status_message").doesNotExist())
                .andExpect(jsonPath("\$.data").doesNotExist())
                .andExpect(jsonPath("\$.timestamp").isString)
    }


    @Test
    fun `When DELETE receiver Tariffs return OCPI success`() {

        val tariffID = "xxx-1234"

        val sender = BasicRole("BSE", "DE")
        val receiver = BasicRole("EMY", "DE")

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.TARIFFS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.DELETE,
                requestID = generateUUIDv4Token(),
                correlationID = generateUUIDv4Token(),
                sender = sender,
                receiver = receiver,
                urlPathVariables = "/${sender.country}/${sender.id}/$tariffID",
                expectedResponseType = OcpiResponseDataType.NOTHING)

        val url = "https://ocpi.cpo.com/2.2/tariffs/${sender.country}/${sender.id}/$tariffID"

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

        every {

            httpService.makeRequest<Nothing>(
                    method = requestVariables.method,
                    url = url,
                    headers = headers,
                    expectedDataType = requestVariables.expectedResponseType)

        } returns HttpResponse(
                statusCode = 200,
                headers = mapOf(),
                body = OcpiResponse(statusCode = 1000))

        mockMvc.perform(delete("/ocpi/receiver/2.2/tariffs/${sender.country}/${sender.id}/$tariffID")
                .header("Authorization", "Token token-c")
                .header("X-Request-ID", requestVariables.requestID)
                .header("X-Correlation-ID", requestVariables.correlationID)
                .header("OCPI-from-country-code", sender.country)
                .header("OCPI-from-party-id", sender.id)
                .header("OCPI-to-country-code", receiver.country)
                .header("OCPI-to-party-id", receiver.id))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("\$.status_code").value(OcpiStatus.SUCCESS.code))
                .andExpect(jsonPath("\$.status_message").doesNotExist())
                .andExpect(jsonPath("\$.data").doesNotExist())
                .andExpect(jsonPath("\$.timestamp").isString)
    }

}