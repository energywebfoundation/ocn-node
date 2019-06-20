package snc.connect.broker.controllers.ocpi.v2_2

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.mockk
import org.hamcrest.Matchers
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import snc.connect.broker.repositories.*
import snc.connect.broker.Properties
import snc.connect.broker.enums.InterfaceRole
import snc.connect.broker.enums.Role
import snc.connect.broker.enums.Status
import snc.connect.broker.models.entities.Auth
import snc.connect.broker.models.entities.EndpointEntity
import snc.connect.broker.models.entities.OrganizationEntity
import snc.connect.broker.models.entities.CredentialEntity
import snc.connect.broker.models.ocpi.*
import snc.connect.broker.services.HttpRequestService

@WebMvcTest(CredentialsController::class)
class CredentialsControllerTest(@Autowired val mockMvc: MockMvc) {

    @MockkBean
    lateinit var orgRepo: OrganizationRepository

    @MockkBean
    lateinit var credentialRepo: CredentialRepository

    @MockkBean
    lateinit var endpointRepo: EndpointRepository

    @MockkBean
    lateinit var properties: Properties

    @MockkBean
    lateinit var httpRequestService: HttpRequestService

    @Test
    fun `When GET credentials then return broker credentials`() {
        val org = OrganizationEntity(auth = Auth(tokenC = "0987654321"))
        every { orgRepo.findByAuth_TokenC(org.auth.tokenC) } returns org
        every { properties.host } returns "http://localhost:8001"
        mockMvc.perform(get("/ocpi/hub/2.2/credentials")
                .header("Authorization", "Token ${org.auth.tokenC}"))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("\$.status_code").value(Status.SUCCESS.code))
                .andExpect(jsonPath("\$.status_message").doesNotExist())
                .andExpect(jsonPath("\$.timestamp").isString)
                .andExpect(jsonPath("\$.data.token").value(org.auth.tokenC!!))
                .andExpect(jsonPath("\$.data.url").value("http://localhost:8001/ocpi/hub/versions"))
                .andExpect(jsonPath("\$.data.roles", Matchers.hasSize<Array<CredentialsRole>>(1)))
                .andExpect(jsonPath("\$.data.roles[0].role").value("HUB"))
                .andExpect(jsonPath("\$.data.roles[0].party_id").value("SNC"))
                .andExpect(jsonPath("\$.data.roles[0].country_code").value("DE"))
                .andExpect(jsonPath("\$.data.roles[0].business_details.name").value("Share&Charge Message Broker"))
    }

    @Test
    fun `When POST credentials then return broker credentials and TOKEN_C`() {
        val org = OrganizationEntity(id = 1L, auth = Auth(tokenA = "12345"))
        val party1 = CredentialsRole(
                role = Role.CPO,
                businessDetails = BusinessDetails("charging.net"),
                partyID = "CHG",
                countryCode = "FR")
        val party2 = CredentialsRole(
                role = Role.EMSP,
                businessDetails = BusinessDetails("msp.co"),
                partyID = "MSC",
                countryCode = "NL")
        val versionsUrl = "https://org.charging.net/versions"
        val versionDetailUrl = "https://org.charging.net/2.2"
        val tokenB = "67890"

        every { orgRepo.findByAuth_TokenA(org.auth.tokenA) } returns org
        every { httpRequestService.getVersions(versionsUrl, tokenB) } returns Versions(
                versions = arrayOf(Version(
                        version = "2.2",
                        url = versionDetailUrl)))
        every { httpRequestService.getVersionDetail(versionDetailUrl, tokenB) } returns VersionDetail(
                version = "2.2",
                endpoints = arrayOf(
                        Endpoint("credentials", InterfaceRole.CPO, "https://org.charging.net/credentials"),
                        Endpoint("commands", InterfaceRole.MSP, "https://org.charging.net/commands")))
        every { properties.host } returns "http://my.broker.com"
        every { credentialRepo.existsByCountryCodeAndPartyID(party1.countryCode, party1.partyID) } returns false
        every { credentialRepo.existsByCountryCodeAndPartyID(party2.countryCode, party2.partyID) } returns false
        every { orgRepo.save(any<OrganizationEntity>()) } returns org
        every { endpointRepo.save(any<EndpointEntity>()) } returns mockk()
        every { credentialRepo.save(any<CredentialEntity>())} returns mockk()

        mockMvc.perform(post("/ocpi/hub/2.2/credentials")
                .header("Authorization", "Token ${org.auth.tokenA}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jacksonObjectMapper().writeValueAsString(Credentials(
                        token = tokenB,
                        url = versionsUrl,
                        roles = arrayOf(party1, party2)))))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("\$.status_code").value(Status.SUCCESS.code))
                .andExpect(jsonPath("\$.status_message").doesNotExist())
                .andExpect(jsonPath("\$.timestamp").isString)
                .andExpect(jsonPath("\$.data.token").isString)
                .andExpect(jsonPath("\$.data.url").value("http://my.broker.com/ocpi/hub/versions"))
                .andExpect(jsonPath("\$.data.roles", Matchers.hasSize<Array<CredentialsRole>>(1)))
                .andExpect(jsonPath("\$.data.roles[0].role").value("HUB"))
                .andExpect(jsonPath("\$.data.roles[0].party_id").value("SNC"))
                .andExpect(jsonPath("\$.data.roles[0].country_code").value("DE"))
                .andExpect(jsonPath("\$.data.roles[0].business_details.name").value("Share&Charge Message Broker"))
    }

    @Test
    fun `When PUT credentials then return broker credentials and new TOKEN_C`() {
        val org = OrganizationEntity(id = 1L, auth = Auth(tokenA = null, tokenB = "67890", tokenC = "0102030405"))
        val party1 = CredentialsRole(
                role = Role.CPO,
                businessDetails = BusinessDetails("charging.net"),
                partyID = "CHG",
                countryCode = "FR")
        val party2 = CredentialsRole(
                role = Role.EMSP,
                businessDetails = BusinessDetails("msp.co"),
                partyID = "MSC",
                countryCode = "NL")
        val versionsUrl = "https://org.charging.net/versions"
        val versionDetailUrl = "https://org.charging.net/2.2"

        every { orgRepo.findByAuth_TokenC(org.auth.tokenC) } returns org
        every { httpRequestService.getVersions(versionsUrl, org.auth.tokenB!!) } returns Versions(
                versions = arrayOf(Version(
                        version = "2.2",
                        url = versionDetailUrl)))
        every { httpRequestService.getVersionDetail(versionDetailUrl, org.auth.tokenB!!) } returns VersionDetail(
                version = "2.2",
                endpoints = arrayOf(
                        Endpoint("credentials", InterfaceRole.CPO, "https://org.charging.net/credentials"),
                        Endpoint("commands", InterfaceRole.MSP, "https://org.charging.net/commands")))
        every { properties.host } returns "http://my.broker.com"
        every { orgRepo.save(any<OrganizationEntity>()) } returns org
        every { endpointRepo.deleteByOrganization(org.id) } returns mockk()
        every { endpointRepo.save(any<EndpointEntity>()) } returns mockk()
        every { credentialRepo.deleteByOrganization(org.id) } returns mockk()
        every { credentialRepo.save(any<CredentialEntity>())} returns mockk()

        mockMvc.perform(put("/ocpi/hub/2.2/credentials")
                .header("Authorization", "Token ${org.auth.tokenC}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jacksonObjectMapper().writeValueAsString(Credentials(
                        token = org.auth.tokenB!!,
                        url = versionsUrl,
                        roles = arrayOf(party1, party2)))))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("\$.status_code").value(Status.SUCCESS.code))
                .andExpect(jsonPath("\$.status_message").doesNotExist())
                .andExpect(jsonPath("\$.timestamp").isString)
                .andExpect(jsonPath("\$.data.token").isString)
                .andExpect(jsonPath("\$.data.token", Matchers.not("0102030405")))
                .andExpect(jsonPath("\$.data.url").value("http://my.broker.com/ocpi/hub/versions"))
                .andExpect(jsonPath("\$.data.roles", Matchers.hasSize<Array<CredentialsRole>>(1)))
                .andExpect(jsonPath("\$.data.roles[0].role").value("HUB"))
                .andExpect(jsonPath("\$.data.roles[0].party_id").value("SNC"))
                .andExpect(jsonPath("\$.data.roles[0].country_code").value("DE"))
                .andExpect(jsonPath("\$.data.roles[0].business_details.name").value("Share&Charge Message Broker"))
    }

    @Test
    fun `When DELETE credentials then return OCPI success message`() {
        val org = OrganizationEntity(id = 3L, auth = Auth(tokenA = null, tokenB = "123", tokenC = "456"))
        every { orgRepo.findByAuth_TokenC(org.auth.tokenC) } returns org
        every { orgRepo.deleteById(org.id!!) } returns mockk()
        every { credentialRepo.deleteByOrganization(org.id) } returns mockk()
        every { endpointRepo.deleteByOrganization(org.id) } returns mockk()

        mockMvc.perform(delete("/ocpi/hub/2.2/credentials")
                .header("Authorization", "Token ${org.auth.tokenC}"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("\$.status_code").value(Status.SUCCESS.code))
                .andExpect(jsonPath("\$.status_message").doesNotExist())
                .andExpect(jsonPath("\$.data").doesNotExist())
                .andExpect(jsonPath("\$.timestamp").isString)
    }

}