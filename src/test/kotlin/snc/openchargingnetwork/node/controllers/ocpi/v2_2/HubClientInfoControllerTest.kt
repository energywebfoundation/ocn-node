package snc.openchargingnetwork.node.controllers.ocpi.v2_2

import com.ninjasquad.springmockk.MockkBean
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import org.hamcrest.Matchers
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import snc.openchargingnetwork.node.data.exampleClientInfo
import snc.openchargingnetwork.node.models.OcnHeaders
import snc.openchargingnetwork.node.models.ocpi.*
import snc.openchargingnetwork.node.services.HubClientInfoService
import snc.openchargingnetwork.node.services.RoutingService
import snc.openchargingnetwork.node.tools.generateUUIDv4Token
import snc.openchargingnetwork.node.tools.getTimestamp

@WebMvcTest(HubClientInfoController::class)
class HubClientInfoControllerTest(@Autowired val mockMvc: MockMvc) {

    @MockkBean
    lateinit var hubClientInfoService: HubClientInfoService

    @MockkBean
    lateinit var routingService: RoutingService

    @Test
    fun `When GET sender HubClientInfo return paginated clientInfo list`() {

        val dateFrom = getTimestamp()

        val sender = BasicRole("EMY", "DE")
        val receiver = BasicRole("IGY", "DE")

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.HUB_CLIENT_INFO,
                interfaceRole = InterfaceRole.SENDER,
                method = HttpMethod.GET,
                headers = OcnHeaders(
                        authorization = "Token token-c",
                        requestID = generateUUIDv4Token(),
                        correlationID = generateUUIDv4Token(),
                        sender = sender,
                        receiver = receiver),
                urlEncodedParams = mapOf("date_from" to dateFrom))

        every { hubClientInfoService.getList(requestVariables.headers.authorization) } returns listOf(exampleClientInfo)
        every { routingService.validateSender(requestVariables.headers.authorization, sender) } just Runs

        mockMvc.perform(MockMvcRequestBuilders.get("/ocpi/2.2/hubclientinfo")
                .header("Authorization", "Token token-c")
                .header("X-Request-ID", requestVariables.headers.requestID)
                .header("X-Correlation-ID", requestVariables.headers.correlationID)
                .header("OCPI-from-country-code", sender.country)
                .header("OCPI-from-party-id", sender.id)
                .header("OCPI-to-country-code", receiver.country)
                .header("OCPI-to-party-id", receiver.id)
                .param("date_from", dateFrom))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.header().string("X-Total-Count", "1"))
                .andExpect(MockMvcResultMatchers.header().string("X-Limit", "1"))
                .andExpect(MockMvcResultMatchers.jsonPath("\$.status_code").value(OcpiStatus.SUCCESS.code))
                .andExpect(MockMvcResultMatchers.jsonPath("\$.status_message").isString)
                .andExpect(MockMvcResultMatchers.jsonPath("\$.timestamp").isString)
                .andExpect(MockMvcResultMatchers.jsonPath("\$.data").isArray)
                .andExpect(MockMvcResultMatchers.jsonPath("\$.data", Matchers.hasSize<Array<ClientInfo>>(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("\$.data[0].status").value(exampleClientInfo.status.toString()))
                .andExpect(MockMvcResultMatchers.jsonPath("\$.data[0].party_id").value(exampleClientInfo.partyID))
    }
}