package snc.openchargingnetwork.client.services

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import snc.openchargingnetwork.client.data.examplePlatforms
import snc.openchargingnetwork.client.data.exampleRoles
import snc.openchargingnetwork.client.models.ocpi.ConnectionStatus
import snc.openchargingnetwork.client.repositories.PlatformRepository
import snc.openchargingnetwork.client.repositories.RoleRepository

class HubClientInfoServiceTest {

    private val platformRepo: PlatformRepository = mockk()
    private val roleRepo: RoleRepository = mockk()

    private val hubClientInfoService: HubClientInfoService

    init {
        hubClientInfoService = HubClientInfoService(platformRepo, roleRepo)
    }

    @Test
    fun getLocalList() {
        every { platformRepo.findAll() } returns examplePlatforms.asIterable()
        every { roleRepo.findAllByPlatformID(1L) } returns exampleRoles.filter { it.platformID == 1L }
        every { roleRepo.findAllByPlatformID(2L) } returns exampleRoles.filter { it.platformID == 2L }
        every { roleRepo.findAllByPlatformID(3L) } returns exampleRoles.filter { it.platformID == 3L }
        val localList = hubClientInfoService.getLocalList()
        assertThat(localList.size).isEqualTo(exampleRoles.size)
        assertThat(localList.filter { it.status == ConnectionStatus.CONNECTED }.size).isEqualTo(3)
    }

}