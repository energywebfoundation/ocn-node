package snc.openchargingnetwork.client.controllers.ocn

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import snc.openchargingnetwork.client.data.exampleLocation2
import snc.openchargingnetwork.client.models.HttpResponse
import snc.openchargingnetwork.client.models.HubGenericRequest
import snc.openchargingnetwork.client.models.HubRequestResponseType
import snc.openchargingnetwork.client.models.entities.EndpointEntity
import snc.openchargingnetwork.client.models.ocpi.*
import snc.openchargingnetwork.client.services.RoutingService

@WebMvcTest(MessageController::class)
class MessageControllerTest(@Autowired val mockMvc: MockMvc) {

    @MockkBean
    lateinit var routingService: RoutingService

    @Test
    fun `When POST message should forward request and return ocpi response`() {

        val senderRole = BasicRole("ABC", "DE")
        val receiverRole = BasicRole("XYZ", "NL")

        val receiverEndpoint = EndpointEntity(8L, "locations", InterfaceRole.SENDER, "http://client.net/locations")

        every { routingService.isRoleKnownOnNetwork(senderRole) } returns true
        every { routingService.isRoleKnown(receiverRole) } returns true
        every { routingService.getPlatformID(receiverRole) } returns 8L
        every { routingService.getPlatformEndpoint(8L, "locations", InterfaceRole.SENDER) } returns receiverEndpoint
        every { routingService.makeHeaders(8L, "abc-123", any(), any()) } returns mockk()
        every { routingService.forwardRequest(
                method = "GET",
                url = "http://client.net/locations/LOC2",
                headers = any(),
                params = null,
                body = null,
                expectedDataType = Location::class)
        } returns HttpResponse(
                statusCode = 200,
                headers = mapOf(),
                body = OcpiResponse(1000, data = exampleLocation2))

        mockMvc.perform(post("/ocn/message")
                .header("X-Request-ID", "123")
                .header("X-Correlation-ID", "abc-123")
                .header("OCPI-from-country-code", senderRole.country)
                .header("OCPI-from-party-id", senderRole.id)
                .header("OCPI-to-country-code", receiverRole.country)
                .header("OCPI-to-party-id", receiverRole.id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jacksonObjectMapper().writeValueAsString(HubGenericRequest(
                        method = "GET",
                        module = "locations",
                        role = InterfaceRole.SENDER,
                        path = "/LOC2",
                        body = null,
                        expectedResponseType = HubRequestResponseType.LOCATION))))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(MockMvcResultMatchers.jsonPath("\$.status_code").value(OcpiStatus.SUCCESS.code))
                .andExpect(MockMvcResultMatchers.jsonPath("\$.status_message").doesNotExist())
                .andExpect(MockMvcResultMatchers.jsonPath("\$.timestamp").isString)
                .andExpect(MockMvcResultMatchers.jsonPath("\$.data.id").value(exampleLocation2.id))
    }
}