package snc.openchargingnetwork.node.controllers.ocpi.v2_2

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.mockk
import org.hamcrest.Matchers
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import snc.openchargingnetwork.node.data.exampleToken
import snc.openchargingnetwork.node.models.*
import snc.openchargingnetwork.node.models.ocpi.*
import snc.openchargingnetwork.node.services.RequestHandler
import snc.openchargingnetwork.node.services.RequestHandlerBuilder
import snc.openchargingnetwork.node.tools.generateUUIDv4Token


@WebMvcTest(TokensController::class)
class TokensControllerTest(@Autowired val mockMvc: MockMvc) {

    @MockkBean
    lateinit var requestHandlerBuilder: RequestHandlerBuilder


    @Test
    fun `When GET sender tokens return paginated tokens list`() {

        val sender = BasicRole("EMY", "DE")
        val receiver = BasicRole("MUN", "DE")

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.TOKENS,
                interfaceRole = InterfaceRole.SENDER,
                method = HttpMethod.GET,
                headers = OcnHeaders(
                        authorization = "Token token-c",
                        requestID = generateUUIDv4Token(),
                        correlationID = generateUUIDv4Token(),
                        sender = sender,
                        receiver = receiver),
                urlEncodedParams = OcpiRequestParameters(limit = 50))

        val mockRequestHandler = mockk<RequestHandler<Array<Token>>>()

        val responseHeaders = HttpHeaders()
        responseHeaders["Link"] = "https://node.ocn.co/ocpi/sender/2.2/tariffs/page/935432; rel=\"next\""
        responseHeaders["X-Limit"] = "50"
        responseHeaders["X-Total-Count"] = "10675"

        every { requestHandlerBuilder.build<Array<Token>>(requestVariables) } returns mockRequestHandler

        every { mockRequestHandler.validateSender().forwardRequest().getResponseWithPaginationHeaders() } returns ResponseEntity
                .status(200)
                .headers(responseHeaders)
                .body(OcpiResponse(statusCode = 1000, data = arrayOf(exampleToken)))

        mockMvc.perform(get("/ocpi/sender/2.2/tokens")
                .header("Authorization", "Token token-c")
                .header("X-Request-ID", requestVariables.headers.requestID)
                .header("X-Correlation-ID", requestVariables.headers.correlationID)
                .header("OCPI-from-country-code", sender.country)
                .header("OCPI-from-party-id", sender.id)
                .header("OCPI-to-country-code", receiver.country)
                .header("OCPI-to-party-id", receiver.id)
                .param("limit", "50"))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(header().string("Link", "https://node.ocn.co/ocpi/sender/2.2/tariffs/page/935432; rel=\"next\""))
                .andExpect(header().string("X-Limit", "50"))
                .andExpect(header().string("X-Total-Count", "10675"))
                .andExpect(jsonPath("\$.status_code").value(OcpiStatus.SUCCESS.code))
                .andExpect(jsonPath("\$.status_message").doesNotExist())
                .andExpect(jsonPath("\$.timestamp").isString)
                .andExpect(jsonPath("\$.data").isArray)
                .andExpect(jsonPath("\$.data", Matchers.hasSize<Array<Token>>(1)))
                .andExpect(jsonPath("\$.data[0].uid").value(exampleToken.uid))
                .andExpect(jsonPath("\$.data[0].party_id").value(exampleToken.partyID))
    }


    @Test
    fun `When GET sender Tokens page should return proxied tokens list`() {

        val uid = "666"

        val sender = BasicRole("EMY", "DE")
        val receiver = BasicRole("MUN", "DE")

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.TOKENS,
                interfaceRole = InterfaceRole.SENDER,
                method = HttpMethod.GET,
                headers = OcnHeaders(
                        authorization = "Token token-c",
                        requestID = generateUUIDv4Token(),
                        correlationID = generateUUIDv4Token(),
                        sender = sender,
                        receiver = receiver),
                urlPathVariables = uid)

        val mockRequestHandler = mockk<RequestHandler<Array<Token>>>()

        val responseHeaders = HttpHeaders()
        responseHeaders["Link"] = "https://client.ocn.co/ocpi/sender/2.2/tokens/page/935433; rel=\"next\""
        responseHeaders["X-Limit"] = "50"

        every { requestHandlerBuilder.build<Array<Token>>(requestVariables) } returns mockRequestHandler

        every { mockRequestHandler.validateSender().forwardRequest().getResponseWithPaginationHeaders() } returns ResponseEntity
                .status(200)
                .headers(responseHeaders)
                .body(OcpiResponse(statusCode = 1000, data = arrayOf(exampleToken)))

        mockMvc.perform(get("/ocpi/sender/2.2/tokens/page/$uid")
                .header("Authorization", "Token token-c")
                .header("X-Request-ID", requestVariables.headers.requestID)
                .header("X-Correlation-ID", requestVariables.headers.correlationID)
                .header("OCPI-from-country-code", requestVariables.headers.sender.country)
                .header("OCPI-from-party-id", requestVariables.headers.sender.id)
                .header("OCPI-to-country-code", requestVariables.headers.receiver.country)
                .header("OCPI-to-party-id", requestVariables.headers.receiver.id)
                .param("limit", "100"))
                .andExpect(status().isOk)
                .andExpect(header().string("Link", "https://client.ocn.co/ocpi/sender/2.2/tokens/page/935433; rel=\"next\""))
                .andExpect(header().string("X-Limit", "50"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("\$.status_code").value(1000))
                .andExpect(jsonPath("\$.status_message").doesNotExist())
                .andExpect(jsonPath("\$.data").isArray)
                .andExpect(jsonPath("\$.data", Matchers.hasSize<Array<Token>>(1)))
                .andExpect(jsonPath("\$.data[0].uid").value(exampleToken.uid))
                .andExpect(jsonPath("\$.data[0].party_id").value(exampleToken.partyID))
                .andExpect(jsonPath("\$.timestamp").isString)
    }


    @Test
    fun `When POST tokens authorize should return AuthorizationInfo`() {

        val body = LocationReferences(locationID = "LOC99")

        val sender = BasicRole("EMY", "DE")
        val receiver = BasicRole("MUN", "DE")

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.TOKENS,
                interfaceRole = InterfaceRole.SENDER,
                method = HttpMethod.POST,
                headers = OcnHeaders(
                        authorization = "Token token-c",
                        requestID = generateUUIDv4Token(),
                        correlationID = generateUUIDv4Token(),
                        sender = sender,
                        receiver = receiver),
                urlPathVariables = "1234567890/authorize",
                urlEncodedParams = OcpiRequestParameters(type = TokenType.RFID),
                body = body)

        val mockRequestHandler = mockk<RequestHandler<AuthorizationInfo>>()

        every { requestHandlerBuilder.build<AuthorizationInfo>(requestVariables) } returns mockRequestHandler

        every { mockRequestHandler.validateSender().forwardRequest().getResponse() } returns ResponseEntity
                .status(200)
                .body(OcpiResponse(statusCode = 1000, data = AuthorizationInfo(
                        allowed = Allowed.ALLOWED,
                        token = exampleToken)))

        mockMvc.perform(post("/ocpi/sender/2.2/tokens/1234567890/authorize")
                .header("Authorization", "Token token-c")
                .header("X-Request-ID", requestVariables.headers.requestID)
                .header("X-Correlation-ID", requestVariables.headers.correlationID)
                .header("OCPI-from-country-code", requestVariables.headers.sender.country)
                .header("OCPI-from-party-id", requestVariables.headers.sender.id)
                .header("OCPI-to-country-code", requestVariables.headers.receiver.country)
                .header("OCPI-to-party-id", requestVariables.headers.receiver.id)
                .param("limit", "100")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jacksonObjectMapper().writeValueAsString(body)))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("\$.status_code").value(1000))
                .andExpect(jsonPath("\$.status_message").doesNotExist())
                .andExpect(jsonPath("\$.data.allowed").value(Allowed.ALLOWED.toString()))
                .andExpect(jsonPath("\$.data.token.uid").value(exampleToken.uid))
                .andExpect(jsonPath("\$.timestamp").isString)
    }

    @Test
    fun `When GET receiver Tokens return single token object`() {

        val tokenUID = "010203040506070809"

        val sender = BasicRole("EMY", "DE")
        val receiver = BasicRole("MUN", "DE")

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.TOKENS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.GET,
                headers = OcnHeaders(
                        authorization = "Token token-c",
                        requestID = generateUUIDv4Token(),
                        correlationID = generateUUIDv4Token(),
                        sender = sender,
                        receiver = receiver),
                urlPathVariables = "/${sender.country}/${sender.id}/$tokenUID",
                urlEncodedParams = OcpiRequestParameters(type = TokenType.RFID))

        val mockRequestHandler = mockk<RequestHandler<Token>>()

        every { requestHandlerBuilder.build<Token>(requestVariables) } returns mockRequestHandler

        every { mockRequestHandler.validateSender().forwardRequest().getResponse() } returns ResponseEntity
                .status(200)
                .body(OcpiResponse(statusCode = 1000, data = exampleToken))

        mockMvc.perform(get("/ocpi/receiver/2.2/tokens/${sender.country}/${sender.id}/$tokenUID")
                .header("Authorization", "Token token-c")
                .header("X-Request-ID", requestVariables.headers.requestID)
                .header("X-Correlation-ID", requestVariables.headers.correlationID)
                .header("OCPI-from-country-code", sender.country)
                .header("OCPI-from-party-id", sender.id)
                .header("OCPI-to-country-code", receiver.country)
                .header("OCPI-to-party-id", receiver.id))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("\$.status_code").value(OcpiStatus.SUCCESS.code))
                .andExpect(jsonPath("\$.status_message").doesNotExist())
                .andExpect(jsonPath("\$.timestamp").isString)
                .andExpect(jsonPath("\$.data.uid").value(exampleToken.uid))
                .andExpect(jsonPath("\$.data.party_id").value(exampleToken.partyID))
    }


    @Test
    fun `When PUT receiver Tokens return OCPI success`() {

        val tokenUID = "0102030405"

        val sender = BasicRole("EMY", "DE")
        val receiver = BasicRole("MUN", "DE")

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.TOKENS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.PUT,
                headers = OcnHeaders(
                        authorization = "Token token-c",
                        requestID = generateUUIDv4Token(),
                        correlationID = generateUUIDv4Token(),
                        sender = sender,
                        receiver = receiver),
                urlPathVariables = "/${sender.country}/${sender.id}/$tokenUID",
                urlEncodedParams = OcpiRequestParameters(type = TokenType.APP_USER),
                body = exampleToken)

        val mockRequestHandler = mockk<RequestHandler<Unit>>()

        every { requestHandlerBuilder.build<Unit>(requestVariables) } returns mockRequestHandler

        every { mockRequestHandler.validateSender().forwardRequest().getResponse() } returns ResponseEntity
                .status(200)
                .body(OcpiResponse(statusCode = 1000))

        mockMvc.perform(put("/ocpi/receiver/2.2/tokens/${sender.country}/${sender.id}/$tokenUID")
                .header("Authorization", "Token token-c")
                .header("X-Request-ID", requestVariables.headers.requestID)
                .header("X-Correlation-ID", requestVariables.headers.correlationID)
                .header("OCPI-from-country-code", sender.country)
                .header("OCPI-from-party-id", sender.id)
                .header("OCPI-to-country-code", receiver.country)
                .header("OCPI-to-party-id", receiver.id)
                .param("type", TokenType.APP_USER.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(jacksonObjectMapper().writeValueAsString(exampleToken)))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("\$.status_code").value(OcpiStatus.SUCCESS.code))
                .andExpect(jsonPath("\$.status_message").doesNotExist())
                .andExpect(jsonPath("\$.data").doesNotExist())
                .andExpect(jsonPath("\$.timestamp").isString)
    }


    @Test
    fun `When PATCH receiver Tokens return OCPI success`() {

        val tokenUID = "0606060606"

        val body = mapOf("valid" to WhitelistType.NEVER.toString())

        val sender = BasicRole("EMY", "DE")
        val receiver = BasicRole("MUN", "DE")

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.TOKENS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.PATCH,
                headers = OcnHeaders(
                        authorization = "Token token-c",
                        requestID = generateUUIDv4Token(),
                        correlationID = generateUUIDv4Token(),
                        sender = sender,
                        receiver = receiver),
                urlPathVariables = "/${sender.country}/${sender.id}/$tokenUID",
                urlEncodedParams = OcpiRequestParameters(type = TokenType.APP_USER),
                body = body)

        val mockRequestHandler = mockk<RequestHandler<Unit>>()

        every { requestHandlerBuilder.build<Unit>(requestVariables) } returns mockRequestHandler

        every { mockRequestHandler.validateSender().forwardRequest().getResponse() } returns ResponseEntity
                .status(200)
                .body(OcpiResponse(statusCode = 1000))

        mockMvc.perform(patch("/ocpi/receiver/2.2/tokens/${sender.country}/${sender.id}/$tokenUID")
                .header("Authorization", "Token token-c")
                .header("X-Request-ID", requestVariables.headers.requestID)
                .header("X-Correlation-ID", requestVariables.headers.correlationID)
                .header("OCPI-from-country-code", sender.country)
                .header("OCPI-from-party-id", sender.id)
                .header("OCPI-to-country-code", receiver.country)
                .header("OCPI-to-party-id", receiver.id)
                .param("type", TokenType.APP_USER.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(jacksonObjectMapper().writeValueAsString(body)))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("\$.status_code").value(OcpiStatus.SUCCESS.code))
                .andExpect(jsonPath("\$.status_message").doesNotExist())
                .andExpect(jsonPath("\$.data").doesNotExist())
                .andExpect(jsonPath("\$.timestamp").isString)
    }


}