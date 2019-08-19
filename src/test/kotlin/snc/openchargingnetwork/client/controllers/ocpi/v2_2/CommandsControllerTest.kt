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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import snc.openchargingnetwork.client.config.Properties
import snc.openchargingnetwork.client.data.exampleToken
import snc.openchargingnetwork.client.models.*
import snc.openchargingnetwork.client.models.ocpi.*
import snc.openchargingnetwork.client.services.HttpRequestService
import snc.openchargingnetwork.client.services.RoutingService
import snc.openchargingnetwork.client.tools.generateUUIDv4Token
import snc.openchargingnetwork.client.tools.getTimestamp

@WebMvcTest(CommandsController::class)
class CommandsControllerTest(@Autowired val mockMvc: MockMvc) {

    @MockkBean
    lateinit var routingService: RoutingService

    @MockkBean
    lateinit var httpService: HttpRequestService

    @MockkBean
    lateinit var properties: Properties

    @Test
    fun `When POST sender Commands should return basic OCPI success response`() {

        val uid = "9876"
        val body = CommandResult(CommandResultType.ACCEPTED)

        val sender = BasicRole("EMY", "DE")
        val receiver = BasicRole("ZTP", "CH")

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.COMMANDS,
                interfaceRole = InterfaceRole.SENDER,
                method = HttpMethod.POST,
                requestID = generateUUIDv4Token(),
                correlationID = generateUUIDv4Token(),
                sender = sender,
                receiver = receiver,
                urlPathVariables = uid,
                body = body,
                expectedResponseType = OcpiResponseDataType.NOTHING)

        val url = "https://cool.cpo.com/ocpi/commands/START_SESSION/6"

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
                    body = body,
                    expectedDataType = requestVariables.expectedResponseType)

        } returns HttpResponse(
                statusCode = 200,
                headers = mapOf(),
                body = OcpiResponse(
                        statusCode = 1000))

        mockMvc.perform(MockMvcRequestBuilders.post("/ocpi/sender/2.2/commands/START_SESSION/$uid")
                .header("Authorization", "Token token-c")
                .header("X-Request-ID", requestVariables.requestID)
                .header("X-Correlation-ID", requestVariables.correlationID)
                .header("OCPI-from-country-code", sender.country)
                .header("OCPI-from-party-id", sender.id)
                .header("OCPI-to-country-code", receiver.country)
                .header("OCPI-to-party-id", receiver.id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jacksonObjectMapper().writeValueAsString(body)))
                .andExpect(jsonPath("\$.status_code").value(1000))
                .andExpect(jsonPath("\$.status_message").doesNotExist())
                .andExpect(jsonPath("\$.data").doesNotExist())
                .andExpect(jsonPath("\$.timestamp").isString)
    }


    @Test
    fun `When POST CANCEL_RESERVATION should return command response`() {

        val body = CancelReservation(
                responseURL = "https://cool.emsp.co/async/cmd/response/56",
                reservationID = "777")
        val proxyBody = body.copy(responseURL = "https://client.ocn.org/ocpi/sender/2.2/commands/CANCEL_RESERVATION/6")

        val sender = BasicRole("EMY", "DE")
        val receiver = BasicRole("ZTP", "CH")

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.COMMANDS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.POST,
                requestID = generateUUIDv4Token(),
                correlationID = generateUUIDv4Token(),
                sender = sender,
                receiver = receiver,
                urlPathVariables = "CANCEL_RESERVATION",
                body = body,
                expectedResponseType = OcpiResponseDataType.COMMAND_RESPONSE)

        val url = "https://cool.emsp.co/ocpi/commands/CANCEL_RESERVATION/"

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

        every { routingService.setProxyResource(body.responseURL, sender, receiver) } returns 6L
        every { properties.url } returns "https://client.ocn.org"

        every { routingService.prepareLocalPlatformRequest(requestVariables) } returns Pair(url, headers)

        every {

            httpService.makeRequest(
                    method = requestVariables.method,
                    url = url,
                    headers = headers,
                    body = proxyBody,
                    expectedDataType = requestVariables.expectedResponseType)

        } returns HttpResponse(
                statusCode = 200,
                headers = mapOf(),
                body = OcpiResponse(
                        statusCode = 1000,
                        data = CommandResponse(CommandResponseType.ACCEPTED, timeout = 5)))

        mockMvc.perform(MockMvcRequestBuilders.post("/ocpi/receiver/2.2/commands/CANCEL_RESERVATION")
                .header("Authorization", "Token token-c")
                .header("X-Request-ID", requestVariables.requestID)
                .header("X-Correlation-ID", requestVariables.correlationID)
                .header("OCPI-from-country-code", sender.country)
                .header("OCPI-from-party-id", sender.id)
                .header("OCPI-to-country-code", receiver.country)
                .header("OCPI-to-party-id", receiver.id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jacksonObjectMapper().writeValueAsString(body)))
                .andExpect(jsonPath("\$.status_code").value(1000))
                .andExpect(jsonPath("\$.status_message").doesNotExist())
                .andExpect(jsonPath("\$.data.result").value("ACCEPTED"))
                .andExpect(jsonPath("\$.data.timeout").value(5))
                .andExpect(jsonPath("\$.timestamp").isString)
    }


    @Test
    fun `When POST RESERVE_NOW should return command response`() {

        val body = ReserveNow(
                responseURL = "https://cool.emsp.co/async/cmd/response/56",
                token = exampleToken,
                expiryDate = getTimestamp(),
                reservationID = "666",
                locationID = "LOC1")
        val proxyBody = body.copy(responseURL = "https://client.ocn.org/ocpi/sender/2.2/commands/RESERVE_NOW/6")

        val sender = BasicRole("EMY", "DE")
        val receiver = BasicRole("ZTP", "CH")

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.COMMANDS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.POST,
                requestID = generateUUIDv4Token(),
                correlationID = generateUUIDv4Token(),
                sender = sender,
                receiver = receiver,
                urlPathVariables = "RESERVE_NOW",
                body = body,
                expectedResponseType = OcpiResponseDataType.COMMAND_RESPONSE)

        val url = "https://cool.emsp.co/ocpi/commands/RESERVE_NOW/"

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

        every { routingService.setProxyResource(body.responseURL, sender, receiver) } returns 6L
        every { properties.url } returns "https://client.ocn.org"

        every { routingService.prepareLocalPlatformRequest(requestVariables) } returns Pair(url, headers)

        every {

            httpService.makeRequest(
                    method = requestVariables.method,
                    url = url,
                    headers = headers,
                    body = proxyBody,
                    expectedDataType = requestVariables.expectedResponseType)

        } returns HttpResponse(
                statusCode = 200,
                headers = mapOf(),
                body = OcpiResponse(
                        statusCode = 1000,
                        data = CommandResponse(CommandResponseType.ACCEPTED, timeout = 5)))

        mockMvc.perform(MockMvcRequestBuilders.post("/ocpi/receiver/2.2/commands/RESERVE_NOW")
                .header("Authorization", "Token token-c")
                .header("X-Request-ID", requestVariables.requestID)
                .header("X-Correlation-ID", requestVariables.correlationID)
                .header("OCPI-from-country-code", sender.country)
                .header("OCPI-from-party-id", sender.id)
                .header("OCPI-to-country-code", receiver.country)
                .header("OCPI-to-party-id", receiver.id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jacksonObjectMapper().writeValueAsString(body)))
                .andExpect(jsonPath("\$.status_code").value(1000))
                .andExpect(jsonPath("\$.status_message").doesNotExist())
                .andExpect(jsonPath("\$.data.result").value("ACCEPTED"))
                .andExpect(jsonPath("\$.data.timeout").value(5))
                .andExpect(jsonPath("\$.timestamp").isString)
    }


    @Test
    fun `When POST START_SESSION should return command response`() {

        val body = StartSession(
                responseURL = "https://cool.emsp.co/async/cmd/response/56",
                token = exampleToken,
                locationID = "LOC1")
        val proxyBody = body.copy(responseURL = "https://client.ocn.org/ocpi/sender/2.2/commands/START_SESSION/6")

        val sender = BasicRole("EMY", "DE")
        val receiver = BasicRole("ZTP", "CH")

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.COMMANDS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.POST,
                requestID = generateUUIDv4Token(),
                correlationID = generateUUIDv4Token(),
                sender = sender,
                receiver = receiver,
                urlPathVariables = "START_SESSION",
                body = body,
                expectedResponseType = OcpiResponseDataType.COMMAND_RESPONSE)

        val url = "https://cool.emsp.co/ocpi/commands/START_SESSION/"

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

        every { routingService.setProxyResource(body.responseURL, sender, receiver) } returns 6L
        every { properties.url } returns "https://client.ocn.org"

        every { routingService.prepareLocalPlatformRequest(requestVariables) } returns Pair(url, headers)

        every {

            httpService.makeRequest(
                    method = requestVariables.method,
                    url = url,
                    headers = headers,
                    body = proxyBody,
                    expectedDataType = requestVariables.expectedResponseType)

        } returns HttpResponse(
                statusCode = 200,
                headers = mapOf(),
                body = OcpiResponse(
                        statusCode = 1000,
                        data = CommandResponse(CommandResponseType.ACCEPTED, timeout = 25)))

        mockMvc.perform(MockMvcRequestBuilders.post("/ocpi/receiver/2.2/commands/START_SESSION")
                .header("Authorization", "Token token-c")
                .header("X-Request-ID", requestVariables.requestID)
                .header("X-Correlation-ID", requestVariables.correlationID)
                .header("OCPI-from-country-code", sender.country)
                .header("OCPI-from-party-id", sender.id)
                .header("OCPI-to-country-code", receiver.country)
                .header("OCPI-to-party-id", receiver.id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jacksonObjectMapper().writeValueAsString(body)))
                .andExpect(jsonPath("\$.status_code").value(1000))
                .andExpect(jsonPath("\$.status_message").doesNotExist())
                .andExpect(jsonPath("\$.data.result").value("ACCEPTED"))
                .andExpect(jsonPath("\$.data.timeout").value(25))
                .andExpect(jsonPath("\$.timestamp").isString)
    }


    @Test
    fun `When POST STOP_SESSION should return command response`() {

        val body = StopSession(
                responseURL = "https://cool.emsp.co/async/cmd/response/56",
                sessionID = "abc-123-567")
        val proxyBody = body.copy(responseURL = "https://client.ocn.org/ocpi/sender/2.2/commands/STOP_SESSION/6")

        val sender = BasicRole("EMY", "DE")
        val receiver = BasicRole("ZTP", "CH")

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.COMMANDS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.POST,
                requestID = generateUUIDv4Token(),
                correlationID = generateUUIDv4Token(),
                sender = sender,
                receiver = receiver,
                urlPathVariables = "STOP_SESSION",
                body = body,
                expectedResponseType = OcpiResponseDataType.COMMAND_RESPONSE)

        val url = "https://cool.emsp.co/ocpi/commands/STOP_SESSION/"

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

        every { routingService.setProxyResource(body.responseURL, sender, receiver) } returns 6L
        every { properties.url } returns "https://client.ocn.org"

        every { routingService.prepareLocalPlatformRequest(requestVariables) } returns Pair(url, headers)

        every {

            httpService.makeRequest(
                    method = requestVariables.method,
                    url = url,
                    headers = headers,
                    body = proxyBody,
                    expectedDataType = requestVariables.expectedResponseType)

        } returns HttpResponse(
                statusCode = 200,
                headers = mapOf(),
                body = OcpiResponse(
                        statusCode = 1000,
                        data = CommandResponse(CommandResponseType.ACCEPTED, timeout = 25)))

        mockMvc.perform(MockMvcRequestBuilders.post("/ocpi/receiver/2.2/commands/STOP_SESSION")
                .header("Authorization", "Token token-c")
                .header("X-Request-ID", requestVariables.requestID)
                .header("X-Correlation-ID", requestVariables.correlationID)
                .header("OCPI-from-country-code", sender.country)
                .header("OCPI-from-party-id", sender.id)
                .header("OCPI-to-country-code", receiver.country)
                .header("OCPI-to-party-id", receiver.id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jacksonObjectMapper().writeValueAsString(body)))
                .andExpect(jsonPath("\$.status_code").value(1000))
                .andExpect(jsonPath("\$.status_message").doesNotExist())
                .andExpect(jsonPath("\$.data.result").value("ACCEPTED"))
                .andExpect(jsonPath("\$.data.timeout").value(25))
                .andExpect(jsonPath("\$.timestamp").isString)
    }


    @Test
    fun `When POST UNLOCK_CONNECTOR should return command response`() {

        val body = UnlockConnector(
                responseURL = "https://cool.emsp.co/async/cmd/response/56",
                locationID = "LOC1",
                evseUID = "12345",
                connectorID = "1")
        val proxyBody = body.copy(responseURL = "https://client.ocn.org/ocpi/sender/2.2/commands/UNLOCK_CONNECTOR/6")

        val sender = BasicRole("EMY", "DE")
        val receiver = BasicRole("ZTP", "CH")

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.COMMANDS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.POST,
                requestID = generateUUIDv4Token(),
                correlationID = generateUUIDv4Token(),
                sender = sender,
                receiver = receiver,
                urlPathVariables = "UNLOCK_CONNECTOR",
                body = body,
                expectedResponseType = OcpiResponseDataType.COMMAND_RESPONSE)

        val url = "https://cool.emsp.co/ocpi/commands/UNLOCK_CONNECTOR/"

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

        every { routingService.setProxyResource(body.responseURL, sender, receiver) } returns 6L
        every { properties.url } returns "https://client.ocn.org"

        every { routingService.prepareLocalPlatformRequest(requestVariables) } returns Pair(url, headers)

        every {

            httpService.makeRequest(
                    method = requestVariables.method,
                    url = url,
                    headers = headers,
                    body = proxyBody,
                    expectedDataType = requestVariables.expectedResponseType)

        } returns HttpResponse(
                statusCode = 200,
                headers = mapOf(),
                body = OcpiResponse(
                        statusCode = 1000,
                        data = CommandResponse(CommandResponseType.ACCEPTED, timeout = 25)))

        mockMvc.perform(MockMvcRequestBuilders.post("/ocpi/receiver/2.2/commands/UNLOCK_CONNECTOR")
                .header("Authorization", "Token token-c")
                .header("X-Request-ID", requestVariables.requestID)
                .header("X-Correlation-ID", requestVariables.correlationID)
                .header("OCPI-from-country-code", sender.country)
                .header("OCPI-from-party-id", sender.id)
                .header("OCPI-to-country-code", receiver.country)
                .header("OCPI-to-party-id", receiver.id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jacksonObjectMapper().writeValueAsString(body)))
                .andExpect(jsonPath("\$.status_code").value(1000))
                .andExpect(jsonPath("\$.status_message").doesNotExist())
                .andExpect(jsonPath("\$.data.result").value("ACCEPTED"))
                .andExpect(jsonPath("\$.data.timeout").value(25))
                .andExpect(jsonPath("\$.timestamp").isString)
    }

}