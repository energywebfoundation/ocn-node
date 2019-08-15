package snc.openchargingnetwork.client.controllers.ocpi.v2_2

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import snc.openchargingnetwork.client.config.Properties
import snc.openchargingnetwork.client.models.HttpResponse
import snc.openchargingnetwork.client.models.OcpiRequestHeaders
import snc.openchargingnetwork.client.models.OcpiResponseDataType
import snc.openchargingnetwork.client.models.ocpi.*
import snc.openchargingnetwork.client.repositories.ProxyResourceRepository
import snc.openchargingnetwork.client.services.RoutingService
import snc.openchargingnetwork.client.tools.generateUUIDv4Token

@WebMvcTest(CommandsController::class)
class CommandsControllerTest(@Autowired val mockMvc: MockMvc) {

    @MockkBean
    lateinit var routingService: RoutingService

    @MockkBean
    lateinit var proxyResourceRepo: ProxyResourceRepository

    @MockkBean
    lateinit var properties: Properties

//    @Test
//    fun `When POST sender Commands should return basic OCPI success response`() {
//
//        val uid = "8779234"
//
//        val body = CommandResult(result = CommandResultType.ACCEPTED)
//
//        val headers = OcpiRequestHeaders(
//                authorization = "Token ${generateUUIDv4Token()}",
//                requestID = generateUUIDv4Token(),
//                correlationID = generateUUIDv4Token(),
//                ocpiFromPartyID = "BYY",
//                ocpiFromCountryCode = "NL",
//                ocpiToPartyID = "SDD",
//                ocpiToCountryCode = "DE")
//
//        every { routingService.forwardRequest(
//                proxy = true,
//                module = ModuleID.Commands,
//                interfaceRole = InterfaceRole.SENDER,
//                method = HttpMethod.POST,
//                headers = headers,
//                urlPathVariables = uid,
//                body = body,
//                responseBodyType = OcpiResponseDataType.NOTHING)
//
//        } returns HttpResponse(
//                statusCode = 200,
//                headers = mapOf(),
//                body = OcpiResponse(
//                        statusCode = 1000))
//
//        mockMvc.perform(MockMvcRequestBuilders.post("/ocpi/sender/2.2/commands/START_SESSION/$uid")
//                .header("Authorization", headers.authorization)
//                .header("X-Request-ID", headers.requestID)
//                .header("X-Correlation-ID", headers.correlationID)
//                .header("OCPI-from-country-code", headers.ocpiFromCountryCode)
//                .header("OCPI-from-party-id", headers.ocpiFromPartyID)
//                .header("OCPI-to-country-code", headers.ocpiToCountryCode)
//                .header("OCPI-to-party-id", headers.ocpiToPartyID)
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(jacksonObjectMapper().writeValueAsString(body)))
//                .andExpect(jsonPath("\$.status_code").value(1000))
//                .andExpect(jsonPath("\$.status_message").doesNotExist())
//                .andExpect(jsonPath("\$.data").doesNotExist())
//                .andExpect(jsonPath("\$.timestamp").isString)
//    }



}