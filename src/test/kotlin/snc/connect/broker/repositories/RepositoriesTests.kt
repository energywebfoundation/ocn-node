package snc.connect.broker.repositories

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import snc.connect.broker.enums.InterfaceRole
import snc.connect.broker.enums.Role
import snc.connect.broker.models.entities.*
import snc.connect.broker.models.ocpi.BusinessDetails

@DataJpaTest
class RepositoriesTests @Autowired constructor(
        val entityManager: TestEntityManager,
        val orgRepository: OrganizationRepository,
        val credentialRepository: CredentialRepository,
        val endpointRepository: EndpointRepository) {

    /**
     *   OrganizationRepository Tests
     */

    @Test
    fun `When existsByAuth_TokenA then return true`() {
        val org1 = OrganizationEntity()
        val org2 = OrganizationEntity()
        entityManager.persist(org1)
        entityManager.persistAndFlush(org2)
        val exists = orgRepository.existsByAuth_TokenA(org2.auth.tokenA)
        assertThat(exists).isEqualTo(true)
    }

    @Test
    fun `When findByAuth_TokenA then return organization`() {
        val org1 = OrganizationEntity()
        val org2 = OrganizationEntity()
        entityManager.persist(org1)
        entityManager.persistAndFlush(org2)
        val found = orgRepository.findByAuth_TokenA(org2.auth.tokenA)
        assertThat(found).isEqualTo(org2)
    }

    @Test
    fun `When findByAuth_TokenC then return organization`() {
        val org1 = OrganizationEntity()
        val org2 = OrganizationEntity(auth = Auth(tokenA = null, tokenB = "123", tokenC = "456"))
        entityManager.persist(org1)
        entityManager.persistAndFlush(org2)
        val found = orgRepository.findByAuth_TokenC(org2.auth.tokenC)
        assertThat(found).isEqualTo(org2)
    }

    /**
     *   CredentialRepository Tests
     */

    @Test
    fun `When existsByCountryCodeAndPartyID then return true`() {
        val credentials = CredentialEntity(Role.CPO, BusinessDetails("S&C"), "SNC", "DE", 1L)
        entityManager.persistAndFlush(credentials)
        val exists = credentialRepository.existsByCountryCodeAndPartyID(credentials.countryCode, credentials.partyID)
        assertThat(exists).isEqualTo(true)
    }

    @Test
    fun credentialRepository_deleteByOrganization() {
        val credentials1 = CredentialEntity(Role.CPO, BusinessDetails("S&C"), "SNC", "DE", 1L)
        val credentials2 = CredentialEntity(Role.EMSP, BusinessDetails("eMobilify"), "EMO", "DE", 1L)
        val credentials3 = CredentialEntity(Role.NSP, BusinessDetails("NSP"), "NSP", "DE", 5L)
        entityManager.persist(credentials1)
        entityManager.persist(credentials2)
        entityManager.persist(credentials3)
        assertThat(credentialRepository.findAll()).isEqualTo(listOf(credentials1, credentials2, credentials3))
        credentialRepository.deleteByOrganization(1L)
        assertThat(credentialRepository.findAll()).isEqualTo(listOf(credentials3))
    }

    /**
     *   EndpointRepository Tests
     */

    @Test
    fun `When findByOrganization then return endpoints`() {
        val org = OrganizationEntity()
        entityManager.persist(org)
        val foundOrg = orgRepository.findByAuth_TokenA(org.auth.tokenA)
        val orgID = foundOrg?.id!!
        val endpoints1 = EndpointEntity(orgID, "commands", InterfaceRole.CPO, "http://localhost:3000/commands")
        val endpoints2 = EndpointEntity(orgID, "cdrs", InterfaceRole.CPO, "http://localhost:3000/cdrs")
        val endpoints3 = EndpointEntity(orgID, "locations", InterfaceRole.CPO, "http://localhost:3000/locations")
        val endpoints4 = EndpointEntity(orgID, "tokens", InterfaceRole.MSP, "http://localhost:3000/tokens")
        val endpoints5 = EndpointEntity(3L, "tokens", InterfaceRole.CPO, "http://localhost:8000/tokens")
        entityManager.persist(endpoints1)
        entityManager.persist(endpoints2)
        entityManager.persist(endpoints3)
        entityManager.persist(endpoints4)
        entityManager.persist(endpoints5)
        entityManager.flush()
        val found = endpointRepository.findByOrganization(orgID)
        assertThat(found).isEqualTo(listOf(endpoints1, endpoints2, endpoints3, endpoints4))
    }

    @Test
    fun endpointRepository_deleteByOrganization() {
        val endpoints1 = EndpointEntity(2L, "commands", InterfaceRole.CPO, "http://localhost:3000/commands")
        val endpoints2 = EndpointEntity(2L, "cdrs", InterfaceRole.CPO, "http://localhost:3000/cdrs")
        val endpoints3 = EndpointEntity(2L, "commands", InterfaceRole.MSP, "http://localhost:3000/commands")
        entityManager.persist(endpoints1)
        entityManager.persist(endpoints2)
        entityManager.persist(endpoints3)
        entityManager.flush()
        assertThat(endpointRepository.findByOrganization(2L)).isEqualTo(listOf(endpoints1, endpoints2, endpoints3))
        endpointRepository.deleteByOrganization(2L)
        assertThat(endpointRepository.findByOrganization(2L)).isEqualTo(listOf<EndpointEntity>())
    }

}