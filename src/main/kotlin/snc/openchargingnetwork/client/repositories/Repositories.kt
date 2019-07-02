package snc.openchargingnetwork.client.repositories

import org.springframework.data.repository.CrudRepository
import snc.openchargingnetwork.client.models.entities.*
import snc.openchargingnetwork.client.models.ocpi.CommandType
import snc.openchargingnetwork.client.models.ocpi.InterfaceRole

interface PlatformRepository: CrudRepository<PlatformEntity, Long> {
    fun existsByAuth_TokenA(tokenA: String?): Boolean
    fun findByAuth_TokenA(tokenA: String?): PlatformEntity?
    fun findByAuth_TokenC(tokenC: String?): PlatformEntity?
}

interface RoleRepository: CrudRepository<RoleEntity, Long> {
    // used in registration to prevent multiple roles of the same country_code/party_id combination
    fun existsByCountryCodeAndPartyIDAllIgnoreCase(countryCode: String, partyID: String): Boolean
    // used to ensure sender's role is registered to a platform on the broker (hub)
    fun existsByPlatformIDAndCountryCodeAndPartyIDAllIgnoreCase(platformID: Long?, countryCode: String, partyID: String): Boolean
    // used in routing to find roles registered with broker (hub)
    fun findByCountryCodeAndPartyIDAllIgnoreCase(countryCode: String, partyID: String): RoleEntity?
    fun deleteByPlatformID(platformID: Long?)
}

interface EndpointRepository: CrudRepository<EndpointEntity, Long> {
    fun findByPlatformID(platformID: Long?): Iterable<EndpointEntity>
    fun findByPlatformIDAndIdentifierAndRole(platformID: Long?, identifier: String, Role: InterfaceRole): EndpointEntity?
    fun deleteByPlatformID(platformID: Long?)
}

interface CdrRepository: CrudRepository<CdrEntity, Long> {
    fun findByCdrIDAndOwnerIDAndOwnerCountryAndCreatorIDAndCreatorCountryAllIgnoreCase(cdrID: String,
                                                                                       ownerID: String,
                                                                                       ownerCountry: String,
                                                                                       creatorID: String,
                                                                                       creatorCountry: String): CdrEntity?
}

interface CommandResponseUrlRepository: CrudRepository<CommandResponseUrlEntity, Long> {
    fun findByUidAndTypeAndSenderIDAndSenderCountryAndReceiverIDAndReceiverCountryAllIgnoreCase(uid: String,
                                                                                                type: CommandType,
                                                                                                senderID: String,
                                                                                                senderCountry: String,
                                                                                                receiverID: String,
                                                                                                receiverCountry: String): CommandResponseUrlEntity?
}