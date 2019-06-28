package snc.openchargingnetwork.client.controllers.ocpi.v2_2

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import snc.openchargingnetwork.client.data.exampleSession
import snc.openchargingnetwork.client.models.entities.Auth
import snc.openchargingnetwork.client.models.entities.EndpointEntity
import snc.openchargingnetwork.client.models.entities.PlatformEntity
import snc.openchargingnetwork.client.models.ocpi.*
import snc.openchargingnetwork.client.services.RoutingService
import snc.openchargingnetwork.client.tools.urlJoin

@WebMvcTest(SessionsController::class)
class SessionsControllerTest(@Autowired val mockMvc: MockMvc) {

    @MockkBean
    lateinit var routingService: RoutingService

    @Test
    fun `When PUT EMSP Session returns success`() {
        // sender
        val senderPlatform = PlatformEntity(id = 1L, auth = Auth(tokenC = "010203040506070809"))
        val senderRole = BasicRole("XYZ", "DE")
        // receiver
        val receiverRole = BasicRole("ABC", "DE")
        val receiverEndpoint = EndpointEntity(4L, "sessions", InterfaceRole.MSP, "http://localhost:3000/sessions")

        every { routingService.validateSender("Token ${senderPlatform.auth.tokenC}", senderRole) } returns mockk()
        every { routingService.isRoleKnown(receiverRole) } returns true
        every { routingService.getPlatformID(receiverRole) } returns 4L
        every { routingService.getPlatformEndpoint(4L, "sessions", InterfaceRole.MSP) } returns receiverEndpoint
        every { routingService.makeHeaders(4L, "abc-123", senderRole, receiverRole) } returns mockk()
        every { routingService.forwardRequest(
                method = "PUT",
                url = urlJoin(receiverEndpoint.url, "/DE/XYZ/1234"),
                headers = any(),
                body = exampleSession,
                expectedDataType = Nothing::class)
        } returns OcpiResponse(1000, data = null)

        mockMvc.perform(put("/ocpi/emsp/2.2/sessions/DE/XYZ/1234")
                .header("Authorization", "Token ${senderPlatform.auth.tokenC}")
                .header("X-Request-ID", "123")
                .header("X-Correlation-ID", "abc-123")
                .header("OCPI-from-country-code", senderRole.country)
                .header("OCPI-from-party-id", senderRole.id)
                .header("OCPI-to-country-code", receiverRole.country)
                .header("OCPI-to-party-id", receiverRole.id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jacksonObjectMapper().writeValueAsString(exampleSession)))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("\$.status_code").value(OcpiStatus.SUCCESS.code))
                .andExpect(jsonPath("\$.status_message").doesNotExist())
                .andExpect(jsonPath("\$.data").doesNotExist())
                .andExpect(jsonPath("\$.timestamp").isString)
    }

}