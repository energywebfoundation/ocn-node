package snc.openchargingnetwork.node.repositories

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import snc.openchargingnetwork.node.models.ocpi.InterfaceRole
import snc.openchargingnetwork.node.models.ocpi.Role
import snc.openchargingnetwork.node.models.entities.*
import snc.openchargingnetwork.node.models.ocpi.BasicRole
import snc.openchargingnetwork.node.models.ocpi.BusinessDetails
import snc.openchargingnetwork.node.tools.generateUUIDv4Token

@DataJpaTest
class RepositoriesTests @Autowired constructor(
        val entityManager: TestEntityManager,
        val platformRepository: PlatformRepository,
        val roleRepository: RoleRepository,
        val endpointRepository: EndpointRepository,
        val proxyResourceRepository: ProxyResourceRepository) {


    /**
     *   PlatformRepository Tests
     */

    @Test
    fun platformRepository_existsByAuth_TokenA() {
        val platform1 = PlatformEntity()
        val platform2 = PlatformEntity()
        entityManager.persist(platform1)
        entityManager.persist(platform2)
        entityManager.flush()
        val exists = platformRepository.existsByAuth_TokenA(platform2.auth.tokenA)
        assertThat(exists).isEqualTo(true)
    }

    @Test
    fun platformRepository_existsByAuth_TokenC() {
        val exists1 = platformRepository.existsByAuth_TokenC("secret")
        assertThat(exists1).isEqualTo(false)
        val platform = PlatformEntity(auth = Auth(tokenC = "secret"))
        entityManager.persistAndFlush(platform)
        val exists2 = platformRepository.existsByAuth_TokenC("secret")
        assertThat(exists2).isEqualTo(true)
    }

    @Test
    fun platformRepository_findByAuth_TokenA() {
        val platform1 = PlatformEntity()
        val platform2 = PlatformEntity()
        entityManager.persist(platform1)
        entityManager.persist(platform2)
        entityManager.flush()
        val found = platformRepository.findByAuth_TokenA(platform2.auth.tokenA)
        assertThat(found).isEqualTo(platform2)
    }

    @Test
    fun platformRepository_findByAuth_TokenC() {
        val platform1 = PlatformEntity()
        val platform2 = PlatformEntity(auth = Auth(tokenA = null, tokenB = "123", tokenC = "456"))
        entityManager.persist(platform1)
        entityManager.persist(platform2)
        entityManager.flush()
        val found = platformRepository.findByAuth_TokenC(platform2.auth.tokenC)
        assertThat(found).isEqualTo(platform2)
    }


    /**
     *   RoleRepository Tests
     */

    @Test
    fun roleRepository_existsByCountryCodeAndPartyIDAllIgnoreCase() {
        val role = RoleEntity(1L, Role.CPO, BusinessDetails("S&C"), "SNC", "DE")
        entityManager.persistAndFlush(role)
        // find by exact case as entered (uppercase)
        val exists = roleRepository.existsByPlatformIDAndCountryCodeAndPartyIDAllIgnoreCase(1L, role.countryCode, role.partyID)
        assertThat(exists).isEqualTo(true)
        val exists2 = roleRepository.existsByPlatformIDAndCountryCodeAndPartyIDAllIgnoreCase(2L, role.countryCode, role.partyID)
        assertThat(exists2).isEqualTo(false)
        // find by ignore case (lowercase)
        val alsoExists = roleRepository.existsByPlatformIDAndCountryCodeAndPartyIDAllIgnoreCase(1L, role.countryCode.toLowerCase(), role.partyID.toLowerCase())
        assertThat(alsoExists).isEqualTo(true)
    }

    @Test
    fun roleRepository_findAllByCountryCodeAndPartyIDAllIgnoreCase() {
        val role = RoleEntity(1L, Role.CPO, BusinessDetails("S&C"), "SNC", "DE")
        entityManager.persistAndFlush(role)
        // find by exact case as entered (uppercase)
        val exists = roleRepository.findAllByCountryCodeAndPartyIDAllIgnoreCase(role.countryCode, role.partyID)
        assertThat(exists).isEqualTo(listOf(role))
        // find by ignore case (lowercase)
        val alsoExists = roleRepository.findAllByCountryCodeAndPartyIDAllIgnoreCase(role.countryCode.toLowerCase(), role.partyID.toLowerCase())
        assertThat(alsoExists).isEqualTo(listOf(role))
    }

    @Test
    fun roleRepository_deleteByOrgID() {
        val role1 = RoleEntity(1L, Role.CPO, BusinessDetails("S&C"), "SNC", "DE")
        val role2 = RoleEntity(1L, Role.EMSP, BusinessDetails("eMobilify"), "EMO", "DE")
        val role3 = RoleEntity(5L, Role.NSP, BusinessDetails("NSP"), "NSP", "DE")
        entityManager.persist(role1)
        entityManager.persist(role2)
        entityManager.persist(role3)
        entityManager.flush()
        assertThat(roleRepository.findAll()).isEqualTo(listOf(role1, role2, role3))
        roleRepository.deleteByPlatformID(1L)
        assertThat(roleRepository.findAll()).isEqualTo(listOf(role3))
    }


    /**
     *   EndpointRepository Tests
     */

    @Test
    fun endpointRepository_findByOrgID() {
        val endpoint1 = EndpointEntity(2L, "commands", InterfaceRole.SENDER, "http://localhost:3000/commands")
        val endpoint2 = EndpointEntity(2L, "cdrs", InterfaceRole.SENDER, "http://localhost:3000/cdrs")
        val endpoint3 = EndpointEntity(2L, "locations", InterfaceRole.SENDER, "http://localhost:3000/locations")
        val endpoint4 = EndpointEntity(2L, "tokens", InterfaceRole.RECEIVER, "http://localhost:3000/tokens")
        val endpoint5 = EndpointEntity(3L, "tokens", InterfaceRole.SENDER, "http://localhost:8000/tokens")
        entityManager.persist(endpoint1)
        entityManager.persist(endpoint2)
        entityManager.persist(endpoint3)
        entityManager.persist(endpoint4)
        entityManager.persist(endpoint5)
        entityManager.flush()
        val found = endpointRepository.findByPlatformID(2L)
        assertThat(found).isEqualTo(listOf(endpoint1, endpoint2, endpoint3, endpoint4))
    }

    @Test
    fun endpointRepository_findByOrgIDAndIdentifierAndRole() {
        val endpoint1 = EndpointEntity(2L, "commands", InterfaceRole.SENDER, "http://localhost:3000/commands")
        val endpoint2 = EndpointEntity(2L, "cdrs", InterfaceRole.SENDER, "http://localhost:3000/cdrs")
        entityManager.persist(endpoint1)
        entityManager.persist(endpoint2)
        entityManager.flush()
        val endpoint = endpointRepository.findByPlatformIDAndIdentifierAndRole(2L, "cdrs", InterfaceRole.SENDER)
        assertThat(endpoint).isEqualTo(endpoint2)
    }

    @Test
    fun endpointRepository_deleteByOrganization() {
        val endpoints1 = EndpointEntity(2L, "commands", InterfaceRole.SENDER, "http://localhost:3000/commands")
        val endpoints2 = EndpointEntity(2L, "cdrs", InterfaceRole.SENDER, "http://localhost:3000/cdrs")
        val endpoints3 = EndpointEntity(2L, "commands", InterfaceRole.RECEIVER, "http://localhost:3000/commands")
        entityManager.persist(endpoints1)
        entityManager.persist(endpoints2)
        entityManager.persist(endpoints3)
        entityManager.flush()
        assertThat(endpointRepository.findByPlatformID(2L)).isEqualTo(listOf(endpoints1, endpoints2, endpoints3))
        endpointRepository.deleteByPlatformID(2L)
        assertThat(endpointRepository.findByPlatformID(2L)).isEqualTo(listOf<EndpointEntity>())
    }


    /**
     * ProxyResourceRepository Tests
     */

    @Test
    fun proxyResourceRepository_findByIdAndSenderAndReceiver() {
        val sender = BasicRole("SNC", "DE")
        val receiver = BasicRole("ABC", "PL")
        val proxyResource = ProxyResourceEntity(sender = sender, receiver = receiver, resource = "https://resource.io")
        val id = entityManager.persistAndGetId(proxyResource)
        entityManager.flush()
        val foundResource = proxyResourceRepository.findByIdAndSenderAndReceiver(id.toString().toLong(), sender, receiver)
        assertThat(foundResource?.resource).isEqualTo(proxyResource.resource)
    }

    @Test
    fun proxyResourceRepository_findByAlternativeUIDAndSenderAndReceiver() {
        val sender = BasicRole("SNC", "DE")
        val receiver = BasicRole("ABC", "PL")
        val uid = generateUUIDv4Token()
        val proxyResource = ProxyResourceEntity(sender, receiver, "https://resource.io", uid)
        entityManager.persistAndFlush(proxyResource)
        val foundResource = proxyResourceRepository.findByAlternativeUIDAndSenderAndReceiver(uid, sender, receiver)
        assertThat(foundResource?.resource).isEqualTo(proxyResource.resource)
    }

}