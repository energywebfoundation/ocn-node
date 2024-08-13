package snc.openchargingnetwork.node.controllers.ocpi.v2_2

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import org.hamcrest.Matchers
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import snc.openchargingnetwork.node.config.NodeProperties
import snc.openchargingnetwork.node.models.entities.*
import snc.openchargingnetwork.node.models.ocpi.*
import snc.openchargingnetwork.node.models.ocpi.Role
import snc.openchargingnetwork.node.repositories.*
import snc.openchargingnetwork.node.services.HttpService
import snc.openchargingnetwork.node.services.RegistryService
import snc.openchargingnetwork.node.tools.bs64Encoder

@WebMvcTest(CredentialsController::class)
class CredentialsControllerTest(@Autowired val mockMvc: MockMvc) {

    @MockkBean
    lateinit var platformRepo: PlatformRepository

    @MockkBean
    lateinit var roleRepo: RoleRepository

    @MockkBean
    lateinit var endpointRepo: EndpointRepository

    @MockkBean
    lateinit var networkClientInfoRepo: NetworkClientInfoRepository
    
    @MockkBean
    lateinit var ocnRulesListRepo: OcnRulesListRepository

    @MockkBean
    lateinit var properties: NodeProperties

    @MockkBean
    lateinit var registryService: RegistryService

    @MockkBean
    lateinit var httpService: HttpService

    private fun enc(token: String): String = bs64Encoder.encodeToString(token.toByteArray())

    @Test
    fun `When GET credentials then return broker credentials`() {
        val tokenC = "0987654321"
        val platform = PlatformEntity(auth = Auth(tokenC = enc(tokenC)))
        every { platformRepo.findByAuth_TokenC(platform.auth.tokenC) } returns platform
        every { properties.url } returns "http://localhost:8001"
        mockMvc.perform(get("/ocpi/2.2/credentials")
                .header("Authorization", "Token ${platform.auth.tokenC}"))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("\$.status_code").value(OcpiStatus.SUCCESS.code))
                .andExpect(jsonPath("\$.status_message").doesNotExist())
                .andExpect(jsonPath("\$.timestamp").isString)
                .andExpect(jsonPath("\$.data.token").value(tokenC))
                .andExpect(jsonPath("\$.data.url").value("http://localhost:8001/ocpi/versions"))
                .andExpect(jsonPath("\$.data.roles", Matchers.hasSize<Array<CredentialsRole>>(1)))
                .andExpect(jsonPath("\$.data.roles[0].role").value("HUB"))
                .andExpect(jsonPath("\$.data.roles[0].party_id").value("OCN"))
                .andExpect(jsonPath("\$.data.roles[0].country_code").value("CH"))
                .andExpect(jsonPath("\$.data.roles[0].business_details.name").value("Open Charging Network Node"))
    }

    @Test
    fun `When POST credentials then return broker credentials and TOKEN_C`() {
        val platform = PlatformEntity(
                id = 1L,
                auth = Auth(tokenA = enc("12345")),
                rules = OcnRules(signatures = true, blacklist = false, whitelist = false))

        val role1 = CredentialsRole(
                role = Role.CPO,
                businessDetails = BusinessDetails("charging.net"),
                partyID = "CHG",
                countryCode = "FR")
        val role2 = CredentialsRole(
                role = Role.EMSP,
                businessDetails = BusinessDetails("msp.co"),
                partyID = "MSC",
                countryCode = "NL")

        val versionsUrl = "https://org.charging.net/versions"
        val versionDetailUrl = "https://org.charging.net/2.2"
        val tokenB = "67890"

        every { platformRepo.findByAuth_TokenA(platform.auth.tokenA) } returns platform
        every { httpService.getVersions(versionsUrl, enc(tokenB)) } returns listOf(Version(
                        version = "2.2",
                        url = versionDetailUrl))
        every { httpService.getVersionDetail(versionDetailUrl, enc(tokenB)) } returns VersionDetail(
                version = "2.2",
                endpoints = listOf(
                        Endpoint("credentials", InterfaceRole.SENDER, "https://org.charging.net/credentials"),
                        Endpoint("commands", InterfaceRole.RECEIVER, "https://org.charging.net/commands")))
        every { properties.url } returns "http://my.broker.com"
        every { properties.signatures } returns true

        every { registryService.isRoleKnown(BasicRole(role1.partyID, role1.countryCode)) } returns true
        every { networkClientInfoRepo.existsByPartyAndRole(BasicRole(role1.partyID, role1.countryCode), role1.role) } returns false
        every { roleRepo.existsByCountryCodeAndPartyIDAllIgnoreCase(role1.countryCode, role1.partyID) } returns false

        every { registryService.isRoleKnown(BasicRole(role2.partyID, role2.countryCode)) } returns true
        every { roleRepo.existsByCountryCodeAndPartyIDAllIgnoreCase(role2.countryCode, role2.partyID) } returns false
        every { networkClientInfoRepo.existsByPartyAndRole(BasicRole(role2.partyID, role2.countryCode), role2.role) } returns false

        every { platformRepo.save<PlatformEntity>(any()) } returns platform
        every { endpointRepo.save<EndpointEntity>(any()) } returns mockk()
        every { roleRepo.saveAll(any<List<RoleEntity>>())} returns mockk()

        mockMvc.perform(post("/ocpi/2.2/credentials")
                .header("Authorization", "Token ${platform.auth.tokenA!!}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jacksonObjectMapper().writeValueAsString(Credentials(
                        token = tokenB,
                        url = versionsUrl,
                        roles = listOf(role1, role2)))))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("\$.status_code").value(OcpiStatus.SUCCESS.code))
                .andExpect(jsonPath("\$.status_message").doesNotExist())
                .andExpect(jsonPath("\$.timestamp").isString)
                .andExpect(jsonPath("\$.data.token").isString)
                .andExpect(jsonPath("\$.data.url").value("http://my.broker.com/ocpi/versions"))
                .andExpect(jsonPath("\$.data.roles", Matchers.hasSize<Array<CredentialsRole>>(1)))
                .andExpect(jsonPath("\$.data.roles[0].role").value("HUB"))
                .andExpect(jsonPath("\$.data.roles[0].party_id").value("OCN"))
                .andExpect(jsonPath("\$.data.roles[0].country_code").value("CH"))
                .andExpect(jsonPath("\$.data.roles[0].business_details.name").value("Open Charging Network Node"))
    }

    @Test
    fun `When PUT credentials then return broker credentials and new TOKEN_C`() {
        val tokenB = "67890"
        val platform = PlatformEntity(
                id = 1L,
                auth = Auth(tokenA = null, tokenB = enc(tokenB), tokenC = enc("0102030405")),
                rules = OcnRules(signatures = false, whitelist = true, blacklist = false))

        val role1 = CredentialsRole(
                role = Role.CPO,
                businessDetails = BusinessDetails("charging.net"),
                partyID = "CHG",
                countryCode = "FR")
        val role2 = CredentialsRole(
                role = Role.EMSP,
                businessDetails = BusinessDetails("msp.co"),
                partyID = "MSC",
                countryCode = "NL")

        val versionsUrl = "https://org.charging.net/versions"
        val versionDetailUrl = "https://org.charging.net/2.2"

        every { platformRepo.findByAuth_TokenC(platform.auth.tokenC) } returns platform
        every { httpService.getVersions(versionsUrl, platform.auth.tokenB!!) } returns listOf(Version(
                        version = "2.2",
                        url = versionDetailUrl))
        every { httpService.getVersionDetail(versionDetailUrl, platform.auth.tokenB!!) } returns VersionDetail(
                version = "2.2",
                endpoints = listOf(
                        Endpoint("credentials", InterfaceRole.SENDER, "https://org.charging.net/credentials"),
                        Endpoint("commands", InterfaceRole.RECEIVER, "https://org.charging.net/commands")))
        every { properties.url } returns "http://my.broker.com"
        every { properties.signatures } returns false
        every { platformRepo.save<PlatformEntity>(any()) } returns platform
        every { endpointRepo.deleteByPlatformID(platform.id) } returns mockk()
        every { endpointRepo.save<EndpointEntity>(any()) } returns mockk()
        every { roleRepo.findAllByPlatformID(platform.id) } returns listOf()
        every { roleRepo.deleteByPlatformID(platform.id) } returns mockk()
        every { roleRepo.saveAll(any<List<RoleEntity>>())} returns mockk()

        mockMvc.perform(put("/ocpi/2.2/credentials")
                .header("Authorization", "Token ${platform.auth.tokenC}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jacksonObjectMapper().writeValueAsString(Credentials(
                        token = tokenB,
                        url = versionsUrl,
                        roles = listOf(role1, role2)))))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("\$.status_code").value(OcpiStatus.SUCCESS.code))
                .andExpect(jsonPath("\$.status_message").doesNotExist())
                .andExpect(jsonPath("\$.timestamp").isString)
                .andExpect(jsonPath("\$.data.token").isString)
                .andExpect(jsonPath("\$.data.token", Matchers.not("0102030405")))
                .andExpect(jsonPath("\$.data.url").value("http://my.broker.com/ocpi/versions"))
                .andExpect(jsonPath("\$.data.roles", Matchers.hasSize<Array<CredentialsRole>>(1)))
                .andExpect(jsonPath("\$.data.roles[0].role").value("HUB"))
                .andExpect(jsonPath("\$.data.roles[0].party_id").value("OCN"))
                .andExpect(jsonPath("\$.data.roles[0].country_code").value("CH"))
                .andExpect(jsonPath("\$.data.roles[0].business_details.name").value("Open Charging Network Node"))
    }

    @Test
    fun `When DELETE credentials then return OCPI success message`() {
        val platform = PlatformEntity(id = 3L, auth = Auth(tokenA = null, tokenB = "123", tokenC = "456"))
        every { platformRepo.findByAuth_TokenC(platform.auth.tokenC) } returns platform
        every { platformRepo.deleteById(platform.id!!) } just Runs
        every { platformRepo.save(platform) } returns platform
        every { roleRepo.findAllByPlatformID(platform.id) } returns listOf()
        every { roleRepo.deleteByPlatformID(platform.id) } just Runs
        every { endpointRepo.deleteByPlatformID(platform.id) } just Runs
        every { ocnRulesListRepo.deleteByPlatformID(platform.id) } just Runs
        
        mockMvc.perform(delete("/ocpi/2.2/credentials")
                .header("Authorization", "Token ${platform.auth.tokenC}"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("\$.status_code").value(OcpiStatus.SUCCESS.code))
                .andExpect(jsonPath("\$.status_message").doesNotExist())
                .andExpect(jsonPath("\$.data").doesNotExist())
                .andExpect(jsonPath("\$.timestamp").isString)
    }

}