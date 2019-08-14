package snc.openchargingnetwork.client.controllers.ocpi.v2_2

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.mockk
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import snc.openchargingnetwork.client.data.exampleLocation1
import snc.openchargingnetwork.client.models.HttpResponse
import snc.openchargingnetwork.client.models.entities.Auth
import snc.openchargingnetwork.client.models.entities.EndpointEntity
import snc.openchargingnetwork.client.models.entities.PlatformEntity
import snc.openchargingnetwork.client.models.ocpi.*
import snc.openchargingnetwork.client.services.RoutingService

@WebMvcTest(LocationsController::class)
class LocationsControllerTest(@Autowired val mockMvc: MockMvc) {

    @MockkBean
    lateinit var routingService: RoutingService

//    @Test
//    fun `When GET sender Locations returns location list`() {
//        // sender
//        val senderPlatform = PlatformEntity(id = 1L, auth = Auth(tokenC = "010203040506070809"))
//        val senderRole = BasicRole("XYZ", "DE")
//        // receiver
//        val receiverRole = BasicRole("ABC", "DE")
//        val receiverEndpoint = EndpointEntity(4L, "locations", InterfaceRole.SENDER, "http://localhost:3000/locations")
//
//        every { routingService.validateSender("Token ${senderPlatform.auth.tokenC}", senderRole) } returns mockk()
//        every { routingService.isRoleKnown(receiverRole) } returns true
//        every { routingService.getPlatformID(receiverRole) } returns 4L
//        every { routingService.getPlatformEndpoint(4L, "locations", InterfaceRole.SENDER) } returns receiverEndpoint
//        every { routingService.makeHeaders(4L, "abc-123", senderRole, receiverRole) } returns mockk()
//
//        every { routingService.forwardRequest(
//                method = "GET",
//                url = receiverEndpoint.url,
//                headers = any(),
//                params = any(),
//                expectedDataType = Array<Location>::class)
//        } returns HttpResponse(
//                statusCode = 200,
//                headers = mapOf(
//                        "Link" to "<https://example.com/ocpi/2.2/page/2",
//                        "X-Total-Count" to "150",
//                        "X-Limit" to "300"),
//                body = OcpiResponse(1000, data = arrayOf(exampleLocation1)))
//
//        mockMvc.perform(get("/ocpi/sender/2.2/locations")
//                .header("Authorization", "Token ${senderPlatform.auth.tokenC}")
//                .header("X-Request-ID", "123")
//                .header("X-Correlation-ID", "abc-123")
//                .header("OCPI-from-country-code", senderRole.country)
//                .header("OCPI-from-party-id", senderRole.id)
//                .header("OCPI-to-country-code", receiverRole.country)
//                .header("OCPI-to-party-id", receiverRole.id))
//                .andExpect(status().isOk)
//                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
//                .andExpect(jsonPath("\$.status_code").value(OcpiStatus.SUCCESS.code))
//                .andExpect(jsonPath("\$.status_message").doesNotExist())
//                .andExpect(jsonPath("\$.timestamp").isString)
//                .andExpect(jsonPath("\$.data").isArray)
//                .andExpect(jsonPath("\$.data", hasSize<Array<Location>>(1)))
//                .andExpect(jsonPath("\$.data[0].id").value(exampleLocation1.id))
//    }

}