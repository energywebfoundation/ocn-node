package snc.openchargingnetwork.node.controllers.ocpi.v2_2

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import snc.openchargingnetwork.node.models.OcnHeaders
import snc.openchargingnetwork.node.models.ocpi.*
import snc.openchargingnetwork.node.components.OcpiRequestHandler
import snc.openchargingnetwork.node.components.OcpiRequestHandlerBuilder
import snc.openchargingnetwork.node.tools.getTimestamp


@WebMvcTest(ChargingProfilesController::class)
class ChargingProfilesControllerTest(@Autowired val mockMvc: MockMvc) {

    @MockkBean
    private lateinit var requestHandlerBuilder: OcpiRequestHandlerBuilder


    @Test
    fun `When POST sender result return OCPI success response`() {
        val request = OcpiRequestVariables(
                module = ModuleID.CHARGING_PROFILES,
                interfaceRole = InterfaceRole.SENDER,
                method = HttpMethod.POST,
                headers = OcnHeaders(
                        authorization = "Token token-c",
                        requestID = "666",
                        correlationID = "666",
                        sender = BasicRole("SNC", "CH"),
                        receiver = BasicRole("ABC", "DE")),
                urlPath = "12345",
                body = GenericChargingProfileResult(result = ChargingProfileResultType.ACCEPTED))

        val requestHandler: OcpiRequestHandler<Unit> = mockk()

        val response = ResponseEntity.status(HttpStatus.ACCEPTED).body(OcpiResponse<Unit>(statusCode = 1000))

        every { requestHandlerBuilder.build<Unit>(request) } returns requestHandler
        every { requestHandler.forwardDefault(true).getResponse() } returns response

        mockMvc.perform(post("/ocpi/2.2/sender/chargingprofiles/result/12345")
                .header("Authorization", "Token token-c")
                .header("X-Request-ID", "666")
                .header("X-Correlation-ID", "666")
                .header("OCPI-From-Country-Code", "CH")
                .header("OCPI-From-Party-ID", "SNC")
                .header("OCPI-To-Country-Code", "DE")
                .header("OCPI-To-Party-ID", "ABC")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jacksonObjectMapper().writeValueAsString(request.body)))
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("\$.status_code").value(1000))
                .andExpect(MockMvcResultMatchers.jsonPath("\$.status_message").doesNotExist())
                .andExpect(MockMvcResultMatchers.jsonPath("\$.data").doesNotExist())
    }

    @Test
    fun `When PUT sender chargingprofiles return OCPI success response`() {
        val request = OcpiRequestVariables(
                module = ModuleID.CHARGING_PROFILES,
                interfaceRole = InterfaceRole.SENDER,
                method = HttpMethod.PUT,
                headers = OcnHeaders(
                        authorization = "Token token-c",
                        requestID = "666",
                        correlationID = "666",
                        sender = BasicRole("SNC", "CH"),
                        receiver = BasicRole("ABC", "DE")),
                urlPath = "1234567890",
                body = ActiveChargingProfile(
                        startDateTime = getTimestamp(),
                        chargingProfile = ChargingProfile(
                                duration = 300,
                                chargingRateUnit = ChargingRateUnit.A,
                                minChargingRate = 32f,
                                chargingProfilePeriod = listOf(ChargingProfilePeriod(
                                        startPeriod = 0,
                                        limit = 64f)))))

        val requestHandler: OcpiRequestHandler<Unit> = mockk()

        val response = ResponseEntity.status(HttpStatus.ACCEPTED).body(OcpiResponse<Unit>(statusCode = 1000))

        every { requestHandlerBuilder.build<Unit>(request) } returns requestHandler
        every { requestHandler.forwardDefault().getResponse() } returns response

        mockMvc.perform(put("/ocpi/2.2/sender/chargingprofiles/1234567890")
                .header("Authorization", "Token token-c")
                .header("X-Request-ID", "666")
                .header("X-Correlation-ID", "666")
                .header("OCPI-From-Country-Code", "CH")
                .header("OCPI-From-Party-ID", "SNC")
                .header("OCPI-To-Country-Code", "DE")
                .header("OCPI-To-Party-ID", "ABC")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jacksonObjectMapper().writeValueAsString(request.body)))
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("\$.status_code").value(1000))
                .andExpect(MockMvcResultMatchers.jsonPath("\$.status_message").doesNotExist())
                .andExpect(MockMvcResultMatchers.jsonPath("\$.data").doesNotExist())
    }

    @Test
    fun `When GET receiver chargingprofiles return ChargingProfileResponse`() {
        val request = OcpiRequestVariables(
                module = ModuleID.CHARGING_PROFILES,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.GET,
                headers = OcnHeaders(
                        authorization = "Token token-c",
                        requestID = "666",
                        correlationID = "666",
                        sender = BasicRole("SNC", "CH"),
                        receiver = BasicRole("ABC", "DE")),
                urlPath = "0987654321",
                queryParams = mapOf("duration" to 30, "response_url" to "https://server.com/profiles/1"))

        val requestHandler: OcpiRequestHandler<ChargingProfileResponse> = mockk()

        every { requestHandlerBuilder.build<ChargingProfileResponse>(request) } returns requestHandler
        every {
            requestHandler.forwardAsync("https://server.com/profiles/1", any()).getResponse()
        } returns ResponseEntity
                .status(200)
                .body(OcpiResponse(
                        statusCode = 1000,
                        data = ChargingProfileResponse(result = ChargingProfileResponseType.ACCEPTED, timeout = 10)))

        mockMvc.perform(get("/ocpi/2.2/receiver/chargingprofiles/0987654321")
                .header("Authorization", "Token token-c")
                .header("X-Request-ID", "666")
                .header("X-Correlation-ID", "666")
                .header("OCPI-From-Country-Code", "CH")
                .header("OCPI-From-Party-ID", "SNC")
                .header("OCPI-To-Country-Code", "DE")
                .header("OCPI-To-Party-ID", "ABC")
                .param("duration", "30")
                .param("response_url", "https://server.com/profiles/1"))
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("\$.status_code").value(1000))
                .andExpect(MockMvcResultMatchers.jsonPath("\$.status_message").doesNotExist())
                .andExpect(MockMvcResultMatchers.jsonPath("\$.data.result").value("ACCEPTED"))
                .andExpect(MockMvcResultMatchers.jsonPath("\$.data.timeout").value(10))
    }

    @Test
    fun `When PUT receiver chargingprofiles return ChargingProfileResponse`() {
        val request = OcpiRequestVariables(
                module = ModuleID.CHARGING_PROFILES,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.PUT,
                headers = OcnHeaders(
                        authorization = "Token token-c",
                        requestID = "666",
                        correlationID = "666",
                        sender = BasicRole("SNC", "CH"),
                        receiver = BasicRole("ABC", "DE")),
                urlPath = "0102030405",
                body = SetChargingProfile(
                        chargingProfile = ChargingProfile(startDateTime = getTimestamp(), chargingRateUnit = ChargingRateUnit.W),
                        responseUrl = "https://smart.charging.net/profiles/0102030405"
                ))

        val requestHandler: OcpiRequestHandler<ChargingProfileResponse> = mockk()

        every { requestHandlerBuilder.build<ChargingProfileResponse>(request) } returns requestHandler
        every {
            requestHandler.forwardAsync("https://smart.charging.net/profiles/0102030405", any()).getResponse()
        } returns ResponseEntity
                .status(200)
                .body(OcpiResponse(
                        statusCode = 1000,
                        data = ChargingProfileResponse(result = ChargingProfileResponseType.ACCEPTED, timeout = 20)))

        mockMvc.perform(put("/ocpi/2.2/receiver/chargingprofiles/0102030405")
                .header("Authorization", "Token token-c")
                .header("X-Request-ID", "666")
                .header("X-Correlation-ID", "666")
                .header("OCPI-From-Country-Code", "CH")
                .header("OCPI-From-Party-ID", "SNC")
                .header("OCPI-To-Country-Code", "DE")
                .header("OCPI-To-Party-ID", "ABC")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jacksonObjectMapper().writeValueAsString(request.body)))
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("\$.status_code").value(1000))
                .andExpect(MockMvcResultMatchers.jsonPath("\$.status_message").doesNotExist())
                .andExpect(MockMvcResultMatchers.jsonPath("\$.data.result").value("ACCEPTED"))
                .andExpect(MockMvcResultMatchers.jsonPath("\$.data.timeout").value(20))
    }

    @Test
    fun `When DELETE receiver chargingprofiles return ChargingProfileResponse`() {
        val request = OcpiRequestVariables(
                module = ModuleID.CHARGING_PROFILES,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.DELETE,
                headers = OcnHeaders(
                        authorization = "Token token-c",
                        requestID = "666",
                        correlationID = "666",
                        sender = BasicRole("SNC", "CH"),
                        receiver = BasicRole("ABC", "DE")),
                urlPath = "333666999",
                queryParams = mapOf("response_url" to "https://scsp.io/ocpi/callback/369"))

        val requestHandler: OcpiRequestHandler<ChargingProfileResponse> = mockk()

        every { requestHandlerBuilder.build<ChargingProfileResponse>(request) } returns requestHandler
        every {
            requestHandler.forwardAsync("https://scsp.io/ocpi/callback/369", any()).getResponse()
        } returns ResponseEntity
                .status(200)
                .body(OcpiResponse(
                        statusCode = 1000,
                        data = ChargingProfileResponse(result = ChargingProfileResponseType.ACCEPTED, timeout = 15)))

        mockMvc.perform(delete("/ocpi/2.2/receiver/chargingprofiles/333666999")
                .header("Authorization", "Token token-c")
                .header("X-Request-ID", "666")
                .header("X-Correlation-ID", "666")
                .header("OCPI-From-Country-Code", "CH")
                .header("OCPI-From-Party-ID", "SNC")
                .header("OCPI-To-Country-Code", "DE")
                .header("OCPI-To-Party-ID", "ABC")
                .param("response_url", "https://scsp.io/ocpi/callback/369")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jacksonObjectMapper().writeValueAsString(request.body)))
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("\$.status_code").value(1000))
                .andExpect(MockMvcResultMatchers.jsonPath("\$.status_message").doesNotExist())
                .andExpect(MockMvcResultMatchers.jsonPath("\$.data.result").value("ACCEPTED"))
                .andExpect(MockMvcResultMatchers.jsonPath("\$.data.timeout").value(15))
    }

}