package snc.connect.broker.controllers

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
import snc.connect.broker.repositories.CredentialRepository
import snc.connect.broker.repositories.OrganizationRepository
import snc.connect.broker.Properties
import snc.connect.broker.models.entities.OrganizationEntity
import snc.connect.broker.models.ocpi.BasicParty

@WebMvcTest(AdminController::class)
@ExtendWith(RestDocumentationExtension::class)
class AdminControllerTest {

    lateinit var mockMvc: MockMvc

    @MockkBean
    lateinit var orgRepo: OrganizationRepository

    @MockkBean
    lateinit var credentialRepo: CredentialRepository

    @MockkBean
    lateinit var properties: Properties

    @BeforeEach
    fun setUp(webApplicationContext: WebApplicationContext,
              restDocumentation: RestDocumentationContextProvider) {

        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply<DefaultMockMvcBuilder>(MockMvcRestDocumentation.documentationConfiguration(restDocumentation))
                .build()
    }

    @Test
    fun `When POST generate-registration-token then return TOKEN_A and versions endpoint`() {
        val party = BasicParty(country = "DE", id = "SNC")
        val org = OrganizationEntity()
        every { properties.apikey } returns "1234567890"
        every { properties.host } returns "http://localhost:8090"
        every { credentialRepo.existsByCountryCodeAndPartyID(party.country, party.id) } returns false
        every { orgRepo.save(any<OrganizationEntity>()) } returns org
        mockMvc.perform(post("/admin/generate-registration-token")
                .header("Authorization", "Token 1234567890")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jacksonObjectMapper().writeValueAsString(arrayOf(party))))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("\$.token").isString)
                .andExpect(jsonPath("\$.versions").value("http://localhost:8090/ocpi/hub/versions"))
                .andDo(document("admin"))
    }

}