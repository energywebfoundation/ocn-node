package snc.openchargingnetwork.node.services

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import snc.openchargingnetwork.node.data.examplePlatforms
import snc.openchargingnetwork.node.data.exampleRoles
import snc.openchargingnetwork.node.models.entities.EndpointEntity
import snc.openchargingnetwork.node.models.entities.PlatformEntity
import snc.openchargingnetwork.node.models.ocpi.*
import snc.openchargingnetwork.node.repositories.EndpointRepository
import snc.openchargingnetwork.node.repositories.NetworkClientInfoRepository
import snc.openchargingnetwork.node.repositories.PlatformRepository
import snc.openchargingnetwork.node.repositories.RoleRepository
import snc.openchargingnetwork.node.tools.getTimestamp

class HubClientInfoServiceTest {

    private val platformRepo: PlatformRepository = mockk()
    private val roleRepo: RoleRepository = mockk()
    private val endpointRepo: EndpointRepository = mockk()
    private val networkClientInfoRepo: NetworkClientInfoRepository = mockk()
    private val httpService: HttpService = mockk()
    private val routingService: RoutingService = mockk()
    private val walletService: WalletService = mockk()
    private val ocnRulesService: OcnRulesService = mockk()

    private val hubClientInfoService: HubClientInfoService

    init {
        hubClientInfoService = HubClientInfoService(
                platformRepo,
                roleRepo,
                endpointRepo,
                networkClientInfoRepo,
                httpService,
                routingService,
                walletService,
                ocnRulesService)
    }

    @Test
    fun getList() {
        every { platformRepo.findByAuth_TokenC("123123") } returns examplePlatforms[0]

        every { platformRepo.findAll() } returns examplePlatforms.asIterable()

        every { roleRepo.findAllByPlatformID(1L) } returns exampleRoles.filter { it.platformID == 1L }
        every { roleRepo.findAllByPlatformID(2L) } returns exampleRoles.filter { it.platformID == 2L }
        every { roleRepo.findAllByPlatformID(3L) } returns exampleRoles.filter { it.platformID == 3L }

        for (role in exampleRoles) {
            val basicRole = BasicRole(id = role.partyID, country = role.countryCode)
            every { ocnRulesService.isWhitelisted(examplePlatforms[0], basicRole) } returns true
        }

        every { networkClientInfoRepo.findAll() } returns listOf()
        val localList = hubClientInfoService.getList("Token 123123")
        assertThat(localList.size).isEqualTo(exampleRoles.size)
        assertThat(localList.filter { it.status == ConnectionStatus.CONNECTED }.size).isEqualTo(3)
    }

    @Test
    fun `getPartiesToNotifyOfClientInfoChange should only notify connected platforms`() {
        val updatedClientInfo = ClientInfo(
                partyID = "ABC",
                countryCode = "DE",
                role = Role.EMSP,
                status = ConnectionStatus.OFFLINE,
                lastUpdated = getTimestamp())

        val updatedPlatform = PlatformEntity(id = 3L)

        every { platformRepo.findAll() } returns examplePlatforms.asIterable()
        every { roleRepo.findAllByPlatformID(1L) } returns exampleRoles.filter { it.platformID == 1L }
        every { endpointRepo.findByPlatformIDAndIdentifierAndRole(1L, ModuleID.HUB_CLIENT_INFO.id, InterfaceRole.RECEIVER) } returns EndpointEntity(
                platformID = 1L,
                identifier = ModuleID.HUB_CLIENT_INFO.id,
                role = InterfaceRole.RECEIVER,
                url = "http://testplatform.com/ocpi/cpo/2.2/clientinfo")

        every { ocnRulesService.isWhitelisted(
                platform = examplePlatforms.find { it.id == 1L }!!,
                counterParty = BasicRole(id = updatedClientInfo.partyID, country = updatedClientInfo.countryCode))
        } returns true

        val parties = hubClientInfoService.getPartiesToNotifyOfClientInfoChange(updatedPlatform, updatedClientInfo)
        assertThat(parties.count() == 1)
    }
}