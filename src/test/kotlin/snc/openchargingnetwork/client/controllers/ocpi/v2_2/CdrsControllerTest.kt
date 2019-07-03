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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import snc.openchargingnetwork.client.config.Properties
import snc.openchargingnetwork.client.data.exampleCDR
import snc.openchargingnetwork.client.models.HttpResponse
import snc.openchargingnetwork.client.models.entities.EndpointEntity
import snc.openchargingnetwork.client.models.ocpi.BasicRole
import snc.openchargingnetwork.client.models.ocpi.InterfaceRole
import snc.openchargingnetwork.client.models.ocpi.OcpiResponse
import snc.openchargingnetwork.client.services.RoutingService

@WebMvcTest(CdrsController::class)
class CdrsControllerTest(@Autowired val mockMvc: MockMvc) {

    @MockkBean
    lateinit var routingService: RoutingService

    @MockkBean
    lateinit var properties: Properties

    @Test
    fun `when POST receiver cdrs should return header link`() {
        val sender = BasicRole("ZUG", "CH")
        val receiver = BasicRole("TRE", "DE")
        val receiverEndpoint = EndpointEntity(9L, "cdrs", InterfaceRole.RECEIVER, "http://platform.com/cdrs")
        val headers = mapOf(
                "Authorization" to "Token 9342",
                "X-Request-ID" to "53245324",
                "X-Correlation-ID" to "4567878",
                "OCPI-from-country-code" to sender.country,
                "OCPI-from-party-id" to sender.id,
                "OCPI-to-country-code" to receiver.country,
                "OCPI-to-party-id" to receiver.id)
        every { routingService.validateSender("Token 5195923", sender, BasicRole(exampleCDR.partyID, exampleCDR.countryCode)) } returns mockk()
        every { routingService.isRoleKnown(receiver) } returns true
        every { routingService.getPlatformID(receiver) } returns 9L
        every { routingService.getPlatformEndpoint(9L, "cdrs", InterfaceRole.RECEIVER) } returns receiverEndpoint
        every { routingService.makeHeaders(9L, "4567878", sender, receiver) } returns headers
        every { routingService.forwardRequest("PUT", receiverEndpoint.url, headers, null, exampleCDR, Nothing::class) } returns HttpResponse(
                statusCode = 200,
                headers = mapOf("Location" to "http://platform.com/cdrs/42"),
                body = OcpiResponse(statusCode = 1000)
        )
        every { routingService.saveCDR(exampleCDR.id, "http://platform.com/cdrs/42", sender, receiver) } returns mockk()
        every { properties.url } returns "http://hub.net/"

        mockMvc.perform(post("/ocpi/receiver/2.2/cdrs")
                .header("Authorization", "Token 5195923")
                .header("X-Request-ID", "12345")
                .header("X-Correlation-ID", "4567878")
                .header("OCPI-from-country-code", sender.country)
                .header("OCPI-from-party-id", sender.id)
                .header("OCPI-to-country-code", receiver.country)
                .header("OCPI-to-party-id", receiver.id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jacksonObjectMapper().writeValueAsString(exampleCDR)))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("\$.status_code").value(1000))
                .andExpect(jsonPath("\$.status_message").doesNotExist())
                .andExpect(jsonPath("\$.data").doesNotExist())
                .andExpect(header().string("Location", "http://hub.net/ocpi/receiver/2.2/cdrs/${exampleCDR.id}"))
    }


}