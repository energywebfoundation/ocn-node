package snc.openchargingnetwork.node.controllers

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.restdocs.RestDocumentationContextProvider
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import snc.openchargingnetwork.node.repositories.RoleRepository
import snc.openchargingnetwork.node.repositories.PlatformRepository
import snc.openchargingnetwork.node.config.NodeProperties
import snc.openchargingnetwork.node.models.entities.PlatformEntity
import snc.openchargingnetwork.node.models.ocpi.BasicRole
import snc.openchargingnetwork.node.repositories.EndpointRepository

@WebMvcTest(AdminController::class)
@ExtendWith(RestDocumentationExtension::class)
class AdminControllerTest {

    lateinit var mockMvc: MockMvc

    @MockkBean
    lateinit var platformRepo: PlatformRepository

    @MockkBean
    lateinit var roleRepo: RoleRepository

    @MockkBean
    lateinit var endpointRepo: EndpointRepository // Mock the missing dependency

    @MockkBean
    lateinit var properties: NodeProperties

    @BeforeEach
    fun setUp(webApplicationContext: WebApplicationContext,
              restDocumentation: RestDocumentationContextProvider) {

        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply<DefaultMockMvcBuilder>(MockMvcRestDocumentation.documentationConfiguration(restDocumentation))
            .build()
    }

    @Test
    fun `When POST generate-registration-token then return TOKEN_A and versions endpoint`() {
        val platform = PlatformEntity()
        val role = BasicRole(country = "DE", id = "SNC")
        every { properties.apikey } returns "1234567890"
        every { properties.url } returns "https://node.ocn.org"
        every { roleRepo.existsByCountryCodeAndPartyIDAllIgnoreCase(role.country, role.id) } returns false
        every { platformRepo.save<PlatformEntity>(any()) } returns platform
        every { endpointRepo.findByPlatformID(any()) } returns emptyList() // Mock endpointRepo behavior as needed

        mockMvc.perform(post("/admin/generate-registration-token")
            .header("Authorization", "Token 1234567890")
            .contentType(MediaType.APPLICATION_JSON)
            .content(jacksonObjectMapper().writeValueAsString(arrayOf(role))))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("\$.token").isString)
            .andExpect(jsonPath("\$.versions").value("https://node.ocn.org/ocpi/versions"))
            .andDo(document("admin/generate-registration-token"))
    }

}
