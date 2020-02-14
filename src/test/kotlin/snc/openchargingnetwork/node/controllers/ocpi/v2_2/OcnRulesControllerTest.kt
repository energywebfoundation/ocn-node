package snc.openchargingnetwork.node.controllers.ocpi.v2_2

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import snc.openchargingnetwork.node.models.OcnRules
import snc.openchargingnetwork.node.models.OcnRulesList
import snc.openchargingnetwork.node.models.ocpi.BasicRole
import snc.openchargingnetwork.node.services.OcnRulesService


@WebMvcTest(OcnRulesController::class)
class OcnRulesControllerTest(@Autowired val mockMvc: MockMvc) {

    @MockkBean
    lateinit var ocnRulesService: OcnRulesService

    @Test
    fun getRules() {
        val expected = OcnRules(
                signatures = false,
                whitelist = OcnRulesList(false, listOf()),
                blacklist = OcnRulesList(true, listOf(BasicRole("ABC", "DE"))))

        every { ocnRulesService.getRules("Token token-c") } returns expected

        mockMvc.perform(get("/ocpi/receiver/2.2/ocnrules")
                .header("authorization", "Token token-c"))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("\$.status_code").value(1000))
                .andExpect(jsonPath("\$.status_message").doesNotExist())
                .andExpect(jsonPath("\$.data.signatures").value(expected.signatures))
                .andExpect(jsonPath("\$.data.whitelist.active").value(expected.whitelist.active))
                .andExpect(jsonPath("\$.data.blacklist.active").value(expected.blacklist.active))
                .andExpect(jsonPath("\$.timestamp").isString)
    }

    @Test
    fun updateWhitelist() {
        val body = listOf(BasicRole("ABC", "DE"), BasicRole("DEF", "DE"))

        every { ocnRulesService.updateWhitelist("Token token-c", body) } just Runs

        mockMvc.perform(put("/ocpi/receiver/2.2/ocnrules/whitelist")
                .header("authorization", "Token token-c")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jacksonObjectMapper().writeValueAsString(body)))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("\$.status_code").value(1000))
                .andExpect(jsonPath("\$.status_message").doesNotExist())
                .andExpect(jsonPath("\$.data").doesNotExist())
                .andExpect(jsonPath("\$.timestamp").isString)
    }

    @Test
    fun updateBlacklist() {
        val body = listOf(BasicRole("ABC","DE"), BasicRole("DEF","DE"))

        every { ocnRulesService.updateBlacklist("Token token-c", body) } just Runs

        mockMvc.perform(put("/ocpi/receiver/2.2/ocnrules/blacklist")
                .header("authorization", "Token token-c")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jacksonObjectMapper().writeValueAsString(body)))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("\$.status_code").value(1000))
                .andExpect(jsonPath("\$.status_message").doesNotExist())
                .andExpect(jsonPath("\$.data").doesNotExist())
                .andExpect(jsonPath("\$.timestamp").isString)
    }

    @Test
    fun appendToWhitelist() {
        val party = BasicRole("ABC", "DE")

        every { ocnRulesService.appendToWhitelist("Token token-c", party) } just Runs

        mockMvc.perform(post("/ocpi/receiver/2.2/ocnrules/whitelist/de/abc")
                .header("authorization", "Token token-c"))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("\$.status_code").value(1000))
                .andExpect(jsonPath("\$.status_message").doesNotExist())
                .andExpect(jsonPath("\$.data").doesNotExist())
                .andExpect(jsonPath("\$.timestamp").isString)
    }

    @Test
    fun appendToBlacklist() {
        val party = BasicRole("ABC","DE")

        every { ocnRulesService.appendToBlacklist("Token token-c", party) } just Runs

        mockMvc.perform(post("/ocpi/receiver/2.2/ocnrules/blacklist/de/abc")
                .header("authorization", "Token token-c"))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("\$.status_code").value(1000))
                .andExpect(jsonPath("\$.status_message").doesNotExist())
                .andExpect(jsonPath("\$.data").doesNotExist())
                .andExpect(jsonPath("\$.timestamp").isString)
    }

    @Test
    fun deleteFromWhitelist() {
        val party = BasicRole("ABC", "DE")

        every { ocnRulesService.deleteFromWhitelist("Token token-c", party) } just Runs

        mockMvc.perform(delete("/ocpi/receiver/2.2/ocnrules/whitelist/de/abc")
                .header("authorization", "Token token-c"))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("\$.status_code").value(1000))
                .andExpect(jsonPath("\$.status_message").doesNotExist())
                .andExpect(jsonPath("\$.data").doesNotExist())
                .andExpect(jsonPath("\$.timestamp").isString)
    }

    @Test
    fun deleteFromBlacklist() {
        val party = BasicRole("ABC","DE")

        every { ocnRulesService.deleteFromBlacklist("Token token-c", party) } just Runs

        mockMvc.perform(delete("/ocpi/receiver/2.2/ocnrules/blacklist/de/abc")
                .header("authorization", "Token token-c"))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("\$.status_code").value(1000))
                .andExpect(jsonPath("\$.status_message").doesNotExist())
                .andExpect(jsonPath("\$.data").doesNotExist())
                .andExpect(jsonPath("\$.timestamp").isString)
    }

}