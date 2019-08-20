package snc.openchargingnetwork.client.services

import org.springframework.stereotype.Service
import snc.openchargingnetwork.client.models.ocpi.ClientInfo
import snc.openchargingnetwork.client.repositories.PlatformRepository
import snc.openchargingnetwork.client.repositories.RoleRepository

@Service
class HubClientInfoService(private val platformRepo: PlatformRepository,
                           private val roleRepo: RoleRepository) {

    /**
     * Get a HubClientInfo list of local connections
     */
    fun getLocalList(): List<ClientInfo> {
        val allClientInfo = mutableListOf<ClientInfo>()
        for (platform in platformRepo.findAll()) {
            for (role in roleRepo.findAllByPlatformID(platform.id)) {
                allClientInfo.add(ClientInfo(
                        partyID = role.partyID,
                        countryCode = role.countryCode,
                        role = role.role,
                        status = platform.status,
                        lastUpdated = platform.lastUpdated))
            }
        }
        return allClientInfo
    }

}