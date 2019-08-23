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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import snc.openchargingnetwork.client.data.exampleSession
import snc.openchargingnetwork.client.models.*
import snc.openchargingnetwork.client.models.ocpi.*
import snc.openchargingnetwork.client.services.HttpService
import snc.openchargingnetwork.client.services.RoutingService
import snc.openchargingnetwork.client.tools.generateUUIDv4Token
import snc.openchargingnetwork.client.tools.getTimestamp

@WebMvcTest(SessionsController::class)
class SessionsControllerTest(@Autowired val mockMvc: MockMvc) {

    @MockkBean
    lateinit var routingService: RoutingService

    @MockkBean
    lateinit var httpService: HttpService


    @Test
    fun `When GET sender Sessions return paginated session list`() {

        val dateFrom = getTimestamp()

        val sender = BasicRole("EMY", "DE")
        val receiver = BasicRole("EON", "DE")

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.SESSIONS,
                interfaceRole = InterfaceRole.SENDER,
                method = HttpMethod.GET,
                headers = OcpiRequestHeaders(
                    requestID = generateUUIDv4Token(),
                    correlationID = generateUUIDv4Token(),
                    sender = sender,
                    receiver = receiver),
                urlEncodedParams = OcpiRequestParameters(dateFrom = dateFrom, limit = 20))

        val url = "https://ocpi.emsp.com/2.2/sessions"

        val forwardingHeaders = requestVariables.headers.copy(
                authorization = "Token token-b",
                requestID = generateUUIDv4Token())

        every { routingService.validateSender("Token token-c", sender) } just Runs
        every { routingService.validateReceiver(receiver) } returns Recipient.LOCAL
        every { routingService.prepareLocalPlatformRequest(requestVariables) } returns Pair(url, forwardingHeaders)

        val responseHeaders = mapOf(
                "Link" to "https://ocpi.cpo.com/sessions/page/2?dateFrom=$dateFrom?limit=20?offset=20; rel=\"next\"",
                "X-Limit" to "20",
                "X-Total-Count" to "87")

        every {

            httpService.makeOcpiRequest<Array<Session>>(
                    method = requestVariables.method,
                    url = url,
                    headers = forwardingHeaders,
                    urlEncodedParams = requestVariables.urlEncodedParams)

        } returns HttpResponse(
                statusCode = 200,
                headers = responseHeaders,
                body = OcpiResponse(statusCode = 1000, data = arrayOf(exampleSession)))

        val httpHeaders = HttpHeaders()
        httpHeaders["Link"] = "https://client.ocn.co/ocpi/sender/2.2/sessions/page/2247; rel=\"next\""
        httpHeaders["X-Limit"] = responseHeaders["X-Limit"]
        httpHeaders["X-Total-Count"] = responseHeaders["X-Total-Count"]

        every {
            routingService.proxyPaginationHeaders(
                    request = requestVariables,
                    responseHeaders = responseHeaders) } returns httpHeaders

        mockMvc.perform(get("/ocpi/sender/2.2/sessions")
                .header("Authorization", "Token token-c")
                .header("X-Request-ID", requestVariables.headers.requestID)
                .header("X-Correlation-ID", requestVariables.headers.correlationID)
                .header("OCPI-from-country-code", sender.country)
                .header("OCPI-from-party-id", sender.id)
                .header("OCPI-to-country-code", receiver.country)
                .header("OCPI-to-party-id", receiver.id)
                .param("date_from", dateFrom)
                .param("limit", "20"))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(header().string("Link", "https://client.ocn.co/ocpi/sender/2.2/sessions/page/2247; rel=\"next\""))
                .andExpect(header().string("X-Limit", "20"))
                .andExpect(header().string("X-Total-Count", "87"))
                .andExpect(jsonPath("\$.status_code").value(OcpiStatus.SUCCESS.code))
                .andExpect(jsonPath("\$.status_message").doesNotExist())
                .andExpect(jsonPath("\$.timestamp").isString)
                .andExpect(jsonPath("\$.data").isArray)
                .andExpect(jsonPath("\$.data", Matchers.hasSize<Array<Session>>(1)))
                .andExpect(jsonPath("\$.data[0].id").value(exampleSession.id))
                .andExpect(jsonPath("\$.data[0].party_id").value(exampleSession.partyID))
    }


    @Test
    fun `When GET sender Sessions page should return proxied sessions list page`() {

        val sender = BasicRole("EMY", "DE")
        val receiver = BasicRole("EON", "DE")

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.SESSIONS,
                interfaceRole = InterfaceRole.SENDER,
                method = HttpMethod.GET,
                headers = OcpiRequestHeaders(
                        requestID = generateUUIDv4Token(),
                        correlationID = generateUUIDv4Token(),
                        sender = sender,
                        receiver = receiver),
                urlPathVariables = "2247")

        val url = "https://ocpi.cpo.com/sessions/page/2?dateFrom=${getTimestamp()}?limit=20?offset=20"

        val forwardingHeaders = requestVariables.headers.copy(
                authorization = "Token token-b",
                requestID = generateUUIDv4Token())

        every { routingService.validateSender("Token token-c", sender) } just Runs
        every { routingService.validateReceiver(receiver) } returns Recipient.LOCAL
        every { routingService.prepareLocalPlatformRequest(requestVariables, proxied = true) } returns Pair(url, forwardingHeaders)

        val responseHeaders = mapOf(
                "Link" to "https://some.emsp.com/actual/sessions/page/3?limit=20&offset=40; rel=\"next\"",
                "X-Limit" to "20",
                "X-Total-Count" to "87")

        every {

            httpService.makeOcpiRequest<Array<Session>>(
                    method = requestVariables.method,
                    url = url,
                    headers = forwardingHeaders,
                    urlEncodedParams = requestVariables.urlEncodedParams)

        } returns HttpResponse(
                statusCode = 200,
                headers = responseHeaders,
                body = OcpiResponse(statusCode = 1000, data = arrayOf(exampleSession)))

        val httpHeaders = HttpHeaders()
        httpHeaders["Link"] = "https://client.ocn.co/ocpi/sender/2.2/sessions/page/2248; rel=\"next\""
        httpHeaders["X-Limit"] = responseHeaders["X-Limit"]

        every { routingService.deleteProxyResource(requestVariables.urlPathVariables!!) } just Runs

        every {
            routingService.proxyPaginationHeaders(
                    request = requestVariables,
                    responseHeaders = responseHeaders) } returns httpHeaders

        mockMvc.perform(get("/ocpi/sender/2.2/sessions/page/${requestVariables.urlPathVariables}")
                .header("Authorization", "Token token-c")
                .header("X-Request-ID", requestVariables.headers.requestID)
                .header("X-Correlation-ID", requestVariables.headers.correlationID)
                .header("OCPI-from-country-code", requestVariables.headers.sender.country)
                .header("OCPI-from-party-id", requestVariables.headers.sender.id)
                .header("OCPI-to-country-code", requestVariables.headers.receiver.country)
                .header("OCPI-to-party-id", requestVariables.headers.receiver.id)
                .param("limit", "100"))
                .andExpect(status().isOk)
                .andExpect(header().string("Link", "https://client.ocn.co/ocpi/sender/2.2/sessions/page/2248; rel=\"next\""))
                .andExpect(header().string("X-Limit", "20"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("\$.status_code").value(1000))
                .andExpect(jsonPath("\$.status_message").doesNotExist())
                .andExpect(jsonPath("\$.data").isArray)
                .andExpect(jsonPath("\$.data", Matchers.hasSize<Array<Session>>(1)))
                .andExpect(jsonPath("\$.data[0].id").value(exampleSession.id))
                .andExpect(jsonPath("\$.data[0].party_id").value(exampleSession.partyID))
                .andExpect(jsonPath("\$.timestamp").isString)
    }


    @Test
    fun `When PUT charging_preferences return ChargingPreferencesResponse`() {

        val body = ChargingPreferences(profileType = ProfileType.GREEN)

        val sender = BasicRole("EMY", "DE")
        val receiver = BasicRole("EON", "DE")

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.SESSIONS,
                interfaceRole = InterfaceRole.SENDER,
                method = HttpMethod.PUT,
                headers = OcpiRequestHeaders(
                        requestID = generateUUIDv4Token(),
                        correlationID = generateUUIDv4Token(),
                        sender = sender,
                        receiver = receiver),
                urlPathVariables = "/2247/charging_preferences",
                body = body)

        val url = "https://ocpi.cpo.com/sessions/2247/charging_preferences"

        val forwardingHeaders = requestVariables.headers.copy(
                authorization = "Token token-b",
                requestID = generateUUIDv4Token())

        every { routingService.validateSender("Token token-c", sender) } just Runs
        every { routingService.validateReceiver(receiver) } returns Recipient.LOCAL
        every { routingService.prepareLocalPlatformRequest(requestVariables) } returns Pair(url, forwardingHeaders)

        every {

            httpService.makeOcpiRequest<ChargingPreferencesResponse>(
                    method = requestVariables.method,
                    url = url,
                    headers = forwardingHeaders,
                    body = body)

        } returns HttpResponse(
                statusCode = 200,
                headers = mapOf(),
                body = OcpiResponse(statusCode = 1000, data = ChargingPreferencesResponse.NOT_POSSIBLE))

        mockMvc.perform(put("/ocpi/sender/2.2/sessions/2247/charging_preferences")
                .header("Authorization", "Token token-c")
                .header("X-Request-ID", requestVariables.headers.requestID)
                .header("X-Correlation-ID", requestVariables.headers.correlationID)
                .header("OCPI-from-country-code", requestVariables.headers.sender.country)
                .header("OCPI-from-party-id", requestVariables.headers.sender.id)
                .header("OCPI-to-country-code", requestVariables.headers.receiver.country)
                .header("OCPI-to-party-id", requestVariables.headers.receiver.id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jacksonObjectMapper().writeValueAsString(body)))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("\$.status_code").value(1000))
                .andExpect(jsonPath("\$.status_message").doesNotExist())
                .andExpect(jsonPath("\$.data").value(ChargingPreferencesResponse.NOT_POSSIBLE.toString()))
                .andExpect(jsonPath("\$.timestamp").isString)
    }


    @Test
    fun `When GET receiver Session return session object`() {

        val sessionID = "12345"

        val sender = BasicRole("EON", "DE")
        val receiver = BasicRole("EMY", "DE")

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.SESSIONS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.GET,
                headers = OcpiRequestHeaders(
                        requestID = generateUUIDv4Token(),
                        correlationID = generateUUIDv4Token(),
                        sender = sender,
                        receiver = receiver),
                urlPathVariables = "/${sender.country}/${sender.id}/$sessionID")

        val url = "https://ocpi.cpo.com/2.2/sessions/${sender.country}/${sender.id}/$sessionID"

        val forwardingHeaders = requestVariables.headers.copy(
                authorization = "Token token-b",
                requestID = generateUUIDv4Token())

        every { routingService.validateSender("Token token-c", sender) } just Runs
        every { routingService.validateReceiver(receiver) } returns Recipient.LOCAL
        every { routingService.prepareLocalPlatformRequest(requestVariables) } returns Pair(url, forwardingHeaders)

        every {

            httpService.makeOcpiRequest<Session>(
                    method = requestVariables.method,
                    url = url,
                    headers = forwardingHeaders)

        } returns HttpResponse(
                statusCode = 200,
                headers = mapOf(),
                body = OcpiResponse(statusCode = 1000, data = exampleSession))

        mockMvc.perform(get("/ocpi/receiver/2.2/sessions/${sender.country}/${sender.id}/$sessionID")
                .header("Authorization", "Token token-c")
                .header("X-Request-ID", requestVariables.headers.requestID)
                .header("X-Correlation-ID", requestVariables.headers.correlationID)
                .header("OCPI-from-country-code", sender.country)
                .header("OCPI-from-party-id", sender.id)
                .header("OCPI-to-country-code", receiver.country)
                .header("OCPI-to-party-id", receiver.id))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("\$.status_code").value(OcpiStatus.SUCCESS.code))
                .andExpect(jsonPath("\$.status_message").doesNotExist())
                .andExpect(jsonPath("\$.timestamp").isString)
                .andExpect(jsonPath("\$.data.id").value(exampleSession.id))
                .andExpect(jsonPath("\$.data.party_id").value(exampleSession.partyID))
    }


    @Test
    fun `When PUT receiver Session return OCPI success`() {

        val sessionID = "4567"
        val body = exampleSession

        val sender = BasicRole("EON", "DE")
        val receiver = BasicRole("EMY", "DE")

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.SESSIONS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.PUT,
                headers = OcpiRequestHeaders(
                        requestID = generateUUIDv4Token(),
                        correlationID = generateUUIDv4Token(),
                        sender = sender,
                        receiver = receiver),
                urlPathVariables = "/${sender.country}/${sender.id}/$sessionID",
                body = body)

        val url = "https://ocpi.cpo.com/2.2/sessions/${sender.country}/${sender.id}/$sessionID"

        val forwardingHeaders = requestVariables.headers.copy(
                authorization = "Token token-b",
                requestID = generateUUIDv4Token())

        every { routingService.validateSender("Token token-c", sender) } just Runs
        every { routingService.validateReceiver(receiver) } returns Recipient.LOCAL
        every { routingService.prepareLocalPlatformRequest(requestVariables) } returns Pair(url, forwardingHeaders)

        every {

            httpService.makeOcpiRequest<Unit>(
                    method = requestVariables.method,
                    url = url,
                    headers = forwardingHeaders,
                    body = body)

        } returns HttpResponse(
                statusCode = 200,
                headers = mapOf(),
                body = OcpiResponse(statusCode = 1000))

        mockMvc.perform(put("/ocpi/receiver/2.2/sessions/${sender.country}/${sender.id}/$sessionID")
                .header("Authorization", "Token token-c")
                .header("X-Request-ID", requestVariables.headers.requestID)
                .header("X-Correlation-ID", requestVariables.headers.correlationID)
                .header("OCPI-from-country-code", sender.country)
                .header("OCPI-from-party-id", sender.id)
                .header("OCPI-to-country-code", receiver.country)
                .header("OCPI-to-party-id", receiver.id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jacksonObjectMapper().writeValueAsString(body)))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("\$.status_code").value(OcpiStatus.SUCCESS.code))
                .andExpect(jsonPath("\$.status_message").doesNotExist())
                .andExpect(jsonPath("\$.data").doesNotExist())
                .andExpect(jsonPath("\$.timestamp").isString)
    }


    @Test
    fun `When PATCH receiver Session return OCPI success`() {

        val sessionID = "4567"
        val body = mapOf("kwh" to 5.5)

        val sender = BasicRole("EON", "DE")
        val receiver = BasicRole("EMY", "DE")

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.SESSIONS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.PATCH,
                headers = OcpiRequestHeaders(
                        requestID = generateUUIDv4Token(),
                        correlationID = generateUUIDv4Token(),
                        sender = sender,
                        receiver = receiver),
                urlPathVariables = "/${sender.country}/${sender.id}/$sessionID",
                body = body)

        val url = "https://ocpi.cpo.com/2.2/sessions/${sender.country}/${sender.id}/$sessionID"

        val forwardingHeaders = requestVariables.headers.copy(
                authorization = "Token token-b",
                requestID = generateUUIDv4Token())

        every { routingService.validateSender("Token token-c", sender) } just Runs
        every { routingService.validateReceiver(receiver) } returns Recipient.LOCAL
        every { routingService.prepareLocalPlatformRequest(requestVariables) } returns Pair(url, forwardingHeaders)

        every {

            httpService.makeOcpiRequest<Unit>(
                    method = requestVariables.method,
                    url = url,
                    headers = forwardingHeaders,
                    body = body)

        } returns HttpResponse(
                statusCode = 200,
                headers = mapOf(),
                body = OcpiResponse(statusCode = 1000))

        mockMvc.perform(patch("/ocpi/receiver/2.2/sessions/${sender.country}/${sender.id}/$sessionID")
                .header("Authorization", "Token token-c")
                .header("X-Request-ID", requestVariables.headers.requestID)
                .header("X-Correlation-ID", requestVariables.headers.correlationID)
                .header("OCPI-from-country-code", sender.country)
                .header("OCPI-from-party-id", sender.id)
                .header("OCPI-to-country-code", receiver.country)
                .header("OCPI-to-party-id", receiver.id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jacksonObjectMapper().writeValueAsString(body)))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("\$.status_code").value(OcpiStatus.SUCCESS.code))
                .andExpect(jsonPath("\$.status_message").doesNotExist())
                .andExpect(jsonPath("\$.data").doesNotExist())
                .andExpect(jsonPath("\$.timestamp").isString)
    }

}