package snc.connect.broker.controllers.ocpi

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import snc.connect.broker.repositories.OrganizationRepository
import snc.connect.broker.Properties
import snc.connect.broker.enums.ModuleID
import snc.connect.broker.enums.Status
import snc.connect.broker.models.entities.OrganizationEntity
import snc.connect.broker.models.ocpi.Endpoint

@WebMvcTest(VersionsController::class)
class VersionsControllerTest(@Autowired val mockMvc: MockMvc) {

    @MockkBean
    lateinit var repository: OrganizationRepository

    @MockkBean
    lateinit var properties: Properties

    @Test
    fun `When GET versions then return version information`() {
        val org = OrganizationEntity()
        every { repository.existsByAuth_TokenA(org.auth.tokenA) } returns true
        every { properties.host } returns "http://localhost:8070"
        mockMvc.perform(get("/ocpi/hub/versions")
                .header("Authorization", "Token ${org.auth.tokenA}"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("\$.status_code").value(Status.SUCCESS.code))
                .andExpect(jsonPath("\$.status_message").doesNotExist())
                .andExpect(jsonPath("\$.timestamp").isString)
                .andExpect(jsonPath("\$.data.versions").isArray)
                .andExpect(jsonPath("\$.data.versions[0]").isMap)
                .andExpect(jsonPath("\$.data.versions[0].version").value("2.2"))
                .andExpect(jsonPath("\$.data.versions[0].url").value("http://localhost:8070/ocpi/hub/2.2"))
    }

    @Test
    fun `When GET 2_2 then return version details`() {
        val org = OrganizationEntity()
        every { repository.existsByAuth_TokenA(org.auth.tokenA) } returns true
        every { properties.host } returns "https://broker.provider.com"
        mockMvc.perform(get("/ocpi/hub/2.2")
                .header("Authorization", "Token ${org.auth.tokenA}"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("\$.status_code").value(Status.SUCCESS.code))
                .andExpect(jsonPath("\$.status_message").doesNotExist())
                .andExpect(jsonPath("\$.timestamp").isString)
                .andExpect(jsonPath("\$.data.version").value("2.2"))
                .andExpect(jsonPath("\$.data.endpoints", hasSize<Array<Endpoint>>(ModuleID.values().size * 2)))
                .andExpect(jsonPath("\$.data.endpoints[0].identifier").value("cdrs"))
                .andExpect(jsonPath("\$.data.endpoints[0].role").value("MSP"))
                .andExpect(jsonPath("\$.data.endpoints[0].url").value("https://broker.provider.com/ocpi/emsp/2.2/cdrs"))
                .andExpect(jsonPath("\$.data.endpoints[1].identifier").value("cdrs"))
                .andExpect(jsonPath("\$.data.endpoints[1].role").value("CPO"))
                .andExpect(jsonPath("\$.data.endpoints[1].url").value("https://broker.provider.com/ocpi/cpo/2.2/cdrs"))
    }

}