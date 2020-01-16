package snc.openchargingnetwork.node.controllers.ocpi.v2_2

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import snc.openchargingnetwork.node.data.exampleToken
import snc.openchargingnetwork.node.models.*
import snc.openchargingnetwork.node.models.ocpi.*
import snc.openchargingnetwork.node.services.RequestHandler
import snc.openchargingnetwork.node.services.RequestHandlerBuilder
import snc.openchargingnetwork.node.tools.generateUUIDv4Token
import snc.openchargingnetwork.node.tools.getTimestamp


@WebMvcTest(CommandsController::class)
class CommandsControllerTest(@Autowired val mockMvc: MockMvc) {

    @MockkBean
    lateinit var requestHandlerBuilder: RequestHandlerBuilder


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
                headers = OcnHeaders(
                        authorization = "Token token-c",
                        requestID = generateUUIDv4Token(),
                        correlationID = generateUUIDv4Token(),
                        sender = sender,
                        receiver = receiver),
                urlPathVariables = uid,
                body = body)

        val mockRequestHandler = mockk<RequestHandler<Unit>>()

        every { requestHandlerBuilder.build<Unit>(requestVariables) } returns mockRequestHandler

        every { mockRequestHandler.validateSender().forwardRequest(true).getResponse() } returns ResponseEntity
                .status(200)
                .body(OcpiResponse(statusCode = 1000))

        mockMvc.perform(MockMvcRequestBuilders.post("/ocpi/sender/2.2/commands/START_SESSION/$uid")
                .header("Authorization", "Token token-c")
                .header("X-Request-ID", requestVariables.headers.requestID)
                .header("X-Correlation-ID", requestVariables.headers.correlationID)
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

        val sender = BasicRole("EMY", "DE")
        val receiver = BasicRole("ZTP", "CH")

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.COMMANDS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.POST,
                headers = OcnHeaders(
                        authorization = "Token token-c",
                        requestID = generateUUIDv4Token(),
                        correlationID = generateUUIDv4Token(),
                        sender = sender,
                        receiver = receiver),
                urlPathVariables = "CANCEL_RESERVATION",
                body = body)

        val mockRequestHandler = mockk<RequestHandler<CommandResponse>>()

        every { requestHandlerBuilder.build<CommandResponse>(requestVariables) } returns mockRequestHandler

        every { mockRequestHandler
                .validateSender()
                .forwardCommandsReceiverRequest(body.responseURL, any())
                .getResponse() } returns ResponseEntity
                    .status(200)
                    .body(OcpiResponse(
                            statusCode = 1000,
                            data = CommandResponse(CommandResponseType.ACCEPTED, timeout = 5)))

        mockMvc.perform(MockMvcRequestBuilders.post("/ocpi/receiver/2.2/commands/CANCEL_RESERVATION")
                .header("Authorization", "Token token-c")
                .header("X-Request-ID", requestVariables.headers.requestID)
                .header("X-Correlation-ID", requestVariables.headers.correlationID)
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

        val sender = BasicRole("EMY", "DE")
        val receiver = BasicRole("ZTP", "CH")

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.COMMANDS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.POST,
                headers = OcnHeaders(
                        authorization = "Token token-c",
                        requestID = generateUUIDv4Token(),
                        correlationID = generateUUIDv4Token(),
                        sender = sender,
                        receiver = receiver),
                urlPathVariables = "RESERVE_NOW",
                body = body)

        val mockRequestHandler = mockk<RequestHandler<CommandResponse>>()

        every { requestHandlerBuilder.build<CommandResponse>(requestVariables) } returns mockRequestHandler

        every { mockRequestHandler
                .validateSender()
                .forwardCommandsReceiverRequest(body.responseURL, any())
                .getResponse() } returns ResponseEntity
                .status(200)
                .body(OcpiResponse(
                        statusCode = 1000,
                        data = CommandResponse(CommandResponseType.ACCEPTED, timeout = 5)))

        mockMvc.perform(MockMvcRequestBuilders.post("/ocpi/receiver/2.2/commands/RESERVE_NOW")
                .header("Authorization", "Token token-c")
                .header("X-Request-ID", requestVariables.headers.requestID)
                .header("X-Correlation-ID", requestVariables.headers.correlationID)
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

        val sender = BasicRole("EMY", "DE")
        val receiver = BasicRole("ZTP", "CH")

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.COMMANDS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.POST,
                headers = OcnHeaders(
                        authorization = "Token token-c",
                        requestID = generateUUIDv4Token(),
                        correlationID = generateUUIDv4Token(),
                        sender = sender,
                        receiver = receiver),
                urlPathVariables = "START_SESSION",
                body = body)

        val mockRequestHandler = mockk<RequestHandler<CommandResponse>>()

        every { requestHandlerBuilder.build<CommandResponse>(requestVariables) } returns mockRequestHandler

        every { mockRequestHandler
                .validateSender()
                .forwardCommandsReceiverRequest(body.responseURL, any())
                .getResponse() } returns ResponseEntity
                .status(200)
                .body(OcpiResponse(
                        statusCode = 1000,
                        data = CommandResponse(CommandResponseType.ACCEPTED, timeout = 25)))

        mockMvc.perform(MockMvcRequestBuilders.post("/ocpi/receiver/2.2/commands/START_SESSION")
                .header("Authorization", "Token token-c")
                .header("X-Request-ID", requestVariables.headers.requestID)
                .header("X-Correlation-ID", requestVariables.headers.correlationID)
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

        val sender = BasicRole("EMY", "DE")
        val receiver = BasicRole("ZTP", "CH")

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.COMMANDS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.POST,
                headers = OcnHeaders(
                        authorization = "Token token-c",
                        requestID = generateUUIDv4Token(),
                        correlationID = generateUUIDv4Token(),
                        sender = sender,
                        receiver = receiver),
                urlPathVariables = "STOP_SESSION",
                body = body)

        val mockRequestHandler = mockk<RequestHandler<CommandResponse>>()

        every { requestHandlerBuilder.build<CommandResponse>(requestVariables) } returns mockRequestHandler

        every { mockRequestHandler
                .validateSender()
                .forwardCommandsReceiverRequest(body.responseURL, any())
                .getResponse() } returns ResponseEntity
                .status(200)
                .body(OcpiResponse(
                        statusCode = 1000,
                        data = CommandResponse(CommandResponseType.ACCEPTED, timeout = 25)))

        mockMvc.perform(MockMvcRequestBuilders.post("/ocpi/receiver/2.2/commands/STOP_SESSION")
                .header("Authorization", "Token token-c")
                .header("X-Request-ID", requestVariables.headers.requestID)
                .header("X-Correlation-ID", requestVariables.headers.correlationID)
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

        val sender = BasicRole("EMY", "DE")
        val receiver = BasicRole("ZTP", "CH")

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.COMMANDS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.POST,
                headers = OcnHeaders(
                        authorization = "Token token-c",
                        requestID = generateUUIDv4Token(),
                        correlationID = generateUUIDv4Token(),
                        sender = sender,
                        receiver = receiver),
                urlPathVariables = "UNLOCK_CONNECTOR",
                body = body)

        val mockRequestHandler = mockk<RequestHandler<CommandResponse>>()

        every { requestHandlerBuilder.build<CommandResponse>(requestVariables) } returns mockRequestHandler

        every { mockRequestHandler
                .validateSender()
                .forwardCommandsReceiverRequest(body.responseURL, any())
                .getResponse() } returns ResponseEntity
                .status(200)
                .body(OcpiResponse(
                        statusCode = 1000,
                        data = CommandResponse(CommandResponseType.ACCEPTED, timeout = 25)))

        mockMvc.perform(MockMvcRequestBuilders.post("/ocpi/receiver/2.2/commands/UNLOCK_CONNECTOR")
                .header("Authorization", "Token token-c")
                .header("X-Request-ID", requestVariables.headers.requestID)
                .header("X-Correlation-ID", requestVariables.headers.correlationID)
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