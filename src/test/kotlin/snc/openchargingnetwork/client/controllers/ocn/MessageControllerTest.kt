package snc.openchargingnetwork.client.controllers.ocn

import com.ninjasquad.springmockk.MockkBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import snc.openchargingnetwork.client.services.RoutingService

@WebMvcTest(MessageController::class)
class MessageControllerTest(@Autowired val mockMvc: MockMvc) {

    @MockkBean
    lateinit var routingService: RoutingService

//    @Test
//    fun `When POST message should forward request and return ocpi response`() {
//
//        val senderRole = BasicRole("ABC", "DE")
//        val receiverRole = BasicRole("XYZ", "NL")
//
//        val receiverEndpoint = EndpointEntity(8L, "locations", InterfaceRole.SENDER, "http://client.net/locations")
//
//        val body = HubGenericRequest(
//                method = "GET",
//                module = "locations",
//                role = InterfaceRole.SENDER,
//                path = "/LOC2",
//                headers = HubRequestHeaders(
//                        requestID = "123",
//                        correlationID = "abc-123",
//                        ocpiFromCountryCode = senderRole.country,
//                        ocpiFromPartyID = senderRole.id,
//                        ocpiToCountryCode = receiverRole.country,
//                        ocpiToPartyID = receiverRole.id),
//                body = null,
//                expectedResponseType = HubRequestResponseType.LOCATION)
//
//        every { routingService.verifyRequest(any(), "0x1234", senderRole) } returns Unit
//        every { routingService.isRoleKnownOnNetwork(senderRole) } returns true
//        every { routingService.isRoleKnown(receiverRole) } returns true
//        every { routingService.getPlatformID(receiverRole) } returns 8L
//        every { routingService.getPlatformEndpoint(8L, "locations", InterfaceRole.SENDER) } returns receiverEndpoint
//        every { routingService.makeHeaders(8L, "abc-123", any(), any()) } returns mockk()
//        every { routingService.forwardRequest(
//                method = "GET",
//                url = "http://client.net/locations/LOC2",
//                headers = any(),
//                params = null,
//                body = null,
//                expectedDataType = Location::class)
//        } returns HttpResponse(
//                statusCode = 200,
//                headers = mapOf(),
//                body = OcpiResponse(1000, data = exampleLocation2))
//
//        mockMvc.perform(post("/ocn/message")
//                .contentType(MediaType.APPLICATION_JSON)
//                .header("X-Request-ID", "xyz")
//                .header("OCN-Signature", "0x1234")
//                .content(jacksonObjectMapper().writeValueAsString(body)))
//                .andExpect(MockMvcResultMatchers.status().isOk)
//                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8))
//                .andExpect(MockMvcResultMatchers.jsonPath("\$.status_code").value(OcpiStatus.SUCCESS.code))
//                .andExpect(MockMvcResultMatchers.jsonPath("\$.status_message").doesNotExist())
//                .andExpect(MockMvcResultMatchers.jsonPath("\$.timestamp").isString)
//                .andExpect(MockMvcResultMatchers.jsonPath("\$.data.id").value(exampleLocation2.id))
//    }
}