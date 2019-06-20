package snc.connect.broker.repositories

import org.springframework.data.repository.CrudRepository
import snc.connect.broker.enums.InterfaceRole
import snc.connect.broker.models.entities.CredentialEntity
import snc.connect.broker.models.entities.EndpointEntity
import snc.connect.broker.models.entities.OrganizationEntity

interface OrganizationRepository: CrudRepository<OrganizationEntity, Long> {
    fun existsByAuth_TokenA(tokenA: String?): Boolean
    fun findByAuth_TokenA(tokenA: String?): OrganizationEntity?
    fun findByAuth_TokenC(tokenC: String?): OrganizationEntity?
}

interface CredentialRepository: CrudRepository<CredentialEntity, Long> {
    fun existsByCountryCodeAndPartyID(countryCode: String, partyID: String): Boolean
//    fun findByOrganizationAndCountryCodeAndPartyID(organization: Long?, countryCode: String, partyID: String): CredentialEntity?
    fun deleteByOrganization(organization: Long?)
}

interface EndpointRepository: CrudRepository<EndpointEntity, Long> {
    fun findByOrganization(organization: Long?): Iterable<EndpointEntity>
//    fun findByOrganizationAndIdentifierAndRole(organization: Long?, identifier: String, Role: InterfaceRole): EndpointEntity?
    fun deleteByOrganization(organization: Long?)
}