package snc.openchargingnetwork.node.controllers.ocpi

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
import snc.openchargingnetwork.node.repositories.PlatformRepository
import snc.openchargingnetwork.node.config.NodeProperties
import snc.openchargingnetwork.node.models.ocpi.OcpiStatus
import snc.openchargingnetwork.node.models.entities.PlatformEntity
import snc.openchargingnetwork.node.models.ocpi.Endpoint

@WebMvcTest(VersionsController::class)
class VersionsControllerTest(@Autowired val mockMvc: MockMvc) {

    @MockkBean
    lateinit var repository: PlatformRepository

    @MockkBean
    lateinit var properties: NodeProperties

    @Test
    fun `When GET versions then return version information`() {
        val platform = PlatformEntity()
        every { repository.existsByAuth_TokenA(platform.auth.tokenA) } returns true
        every { properties.url } returns "http://localhost:8070"
        mockMvc.perform(get("/ocpi/versions")
                .header("Authorization", "Token ${platform.auth.tokenA}"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("\$.status_code").value(OcpiStatus.SUCCESS.code))
                .andExpect(jsonPath("\$.status_message").doesNotExist())
                .andExpect(jsonPath("\$.timestamp").isString)
                .andExpect(jsonPath("\$.data").isArray)
                .andExpect(jsonPath("\$.data[0]").isMap)
                .andExpect(jsonPath("\$.data[0].version").value("2.2"))
                .andExpect(jsonPath("\$.data[0].url").value("http://localhost:8070/ocpi/2.2"))
    }

    @Test
    fun `When GET 2_2 then return version details`() {
        val platform = PlatformEntity()
        every { repository.existsByAuth_TokenA(platform.auth.tokenA) } returns true
        every { properties.url } returns "https://broker.provider.com"
        mockMvc.perform(get("/ocpi/2.2")
                .header("Authorization", "Token ${platform.auth.tokenA}"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("\$.status_code").value(OcpiStatus.SUCCESS.code))
                .andExpect(jsonPath("\$.status_message").doesNotExist())
                .andExpect(jsonPath("\$.timestamp").isString)
                .andExpect(jsonPath("\$.data.version").value("2.2"))
                .andExpect(jsonPath("\$.data.endpoints", hasSize<Array<Endpoint>>(17)))
                .andExpect(jsonPath("\$.data.endpoints[0].identifier").value("cdrs"))
                .andExpect(jsonPath("\$.data.endpoints[0].role").value("SENDER"))
                .andExpect(jsonPath("\$.data.endpoints[0].url").value("https://broker.provider.com/ocpi/sender/2.2/cdrs"))
                .andExpect(jsonPath("\$.data.endpoints[1].identifier").value("cdrs"))
                .andExpect(jsonPath("\$.data.endpoints[1].role").value("RECEIVER"))
                .andExpect(jsonPath("\$.data.endpoints[1].url").value("https://broker.provider.com/ocpi/receiver/2.2/cdrs"))
    }

}