package snc.openchargingnetwork.client.controllers.ocpi.v2_2

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import snc.openchargingnetwork.client.data.exampleCDR
import snc.openchargingnetwork.client.models.exceptions.OcpiClientInvalidParametersException
import snc.openchargingnetwork.client.models.ocpi.BasicRole
import snc.openchargingnetwork.client.models.ocpi.OcpiStatus
import snc.openchargingnetwork.client.services.RoutingService

@WebMvcTest(TariffsController::class)
class TariffsControllerTest(@Autowired val mockMvc: MockMvc) {

    @MockkBean
    lateinit var routingService: RoutingService

    @Test
    fun `when GET tariffs returns unauthorized`() {
        val sender = BasicRole("DIY", "YO")
        val receiver = BasicRole("ADA", "MM")
        every { routingService.validateSender("Token 12345", sender) } throws OcpiClientInvalidParametersException("Invalid CREDENTIALS_TOKEN_C")
        mockMvc.perform(get("/ocpi/cpo/2.2/tariffs")
                .header("Authorization", "Token 12345")
                .header("X-Request-ID", "12345")
                .header("X-Correlation-ID", "4567878")
                .header("OCPI-from-country-code", sender.country)
                .header("OCPI-from-party-id", sender.id)
                .header("OCPI-to-country-code", receiver.country)
                .header("OCPI-to-party-id", receiver.id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jacksonObjectMapper().writeValueAsString(exampleCDR)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest)
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(MockMvcResultMatchers.jsonPath("\$.status_code").value(OcpiStatus.CLIENT_INVALID_PARAMETERS.code))
                .andExpect(MockMvcResultMatchers.jsonPath("\$.status_message").value("Invalid CREDENTIALS_TOKEN_C"))
                .andExpect(MockMvcResultMatchers.jsonPath("\$.data").doesNotExist())
    }
}