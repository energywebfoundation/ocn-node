package snc.connect.broker.controllers

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import snc.connect.broker.PartyRepository
import snc.connect.broker.models.entities.Party
import snc.connect.broker.Properties

@WebMvcTest(AdminController::class)
class AdminControllerTest(@Autowired val mockMvc: MockMvc) {

    @MockkBean
    lateinit var repository: PartyRepository

    @MockkBean
    lateinit var properties: Properties

    @Test
    fun `When POST generate-registration-token then return TOKEN_A and versions endpoint`() {
        val party = Party(countryCode = "DE", partyID = "SNC")
        every { properties.apikey } returns "1234567890"
        every { properties.host } returns "http://localhost:8090"
        every { repository.findByCountryCodeAndPartyID("DE", "SNC") } returns null
        every { repository.save(any<Party>()) } returns party
        mockMvc.perform(post("/admin/generate-registration-token")
                .header("Authorization", "Token 1234567890")
                .contentType(MediaType.APPLICATION_JSON).content("[{\"party_id\":\"SNC\",\"country_code\":\"DE\"}]"))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("\$.token").isString)
                .andExpect(jsonPath("\$.versions").value("http://localhost:8090/ocpi/hub/versions"))
    }

}