package snc.openchargingnetwork.client.controllers.ocn

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import khttp.get as khttpGET
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import snc.openchargingnetwork.client.data.exampleLocation2
import snc.openchargingnetwork.client.models.*
import snc.openchargingnetwork.client.models.ocpi.*
import snc.openchargingnetwork.client.services.WalletService
import snc.openchargingnetwork.client.services.HttpService
import snc.openchargingnetwork.client.services.RoutingService

@WebMvcTest(MessageController::class)
class MessageControllerTest(@Autowired val mockMvc: MockMvc) {

    @MockkBean
    lateinit var routingService: RoutingService

    @MockkBean
    lateinit var walletService: WalletService

    @MockkBean
    lateinit var httpService: HttpService


    @Test
    fun `When POST OCN message should forward the request to local recipient and return their OCPI response`() {

        val senderRole = BasicRole("ABC", "DE")
        val receiverRole = BasicRole("XYZ", "NL")

        val body = OcpiRequestVariables(
                method = HttpMethod.GET,
                module = ModuleID.LOCATIONS,
                interfaceRole = InterfaceRole.SENDER,
                urlPathVariables = "LOC2",
                headers = OcpiRequestHeaders(
                        requestID = "123",
                        correlationID = "abc-123",
                        sender = senderRole,
                        receiver = receiverRole))

        val jsonBodyString = "arbitrary_message"
        every { httpService.mapper.writeValueAsString(body) } returns jsonBodyString
        every { walletService.verify(jsonBodyString, "0x1234", senderRole) } just Runs

        every { routingService.isRoleKnownOnNetwork(senderRole) } returns true
        every { routingService.isRoleKnown(receiverRole) } returns true

        val (url, headers) = Pair(
                "https://some.platform.net/locations/LOC2",
                body.headers.copy(authorization = "Token 123", requestID = "789"))

        every { routingService.prepareLocalPlatformRequest(body) } returns Pair(url, headers)

        every { httpService.makeOcpiRequest<Location> { khttpGET(url, headers.encode(), body.urlEncodedParams?.encode()!!) }
        } returns HttpResponse(
                statusCode = 200,
                headers = mapOf(),
                body = OcpiResponse(1000, data = exampleLocation2))

        mockMvc.perform(post("/ocn/message")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Request-ID", "xyz")
                .header("OCN-Signature", "0x1234")
                .content(jacksonObjectMapper().writeValueAsString(body)))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("\$.status_code").value(OcpiStatus.SUCCESS.code))
                .andExpect(jsonPath("\$.status_message").doesNotExist())
                .andExpect(jsonPath("\$.timestamp").isString)
                .andExpect(jsonPath("\$.data.id").value(exampleLocation2.id))
    }
}