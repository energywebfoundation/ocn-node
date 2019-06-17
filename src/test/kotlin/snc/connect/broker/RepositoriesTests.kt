package snc.connect.broker

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import snc.connect.broker.models.entities.Party

@DataJpaTest
class RepositoriesTests @Autowired constructor(
        val entityManager: TestEntityManager,
        val partyRepository: PartyRepository) {

    @Test
    fun `When findByCountryCodeAndPartyID then return Party`() {
        val party = Party(countryCode = "DE", partyID = "SNC")
        entityManager.persistAndFlush(party)
        val found = partyRepository.findByCountryCodeAndPartyID("DE", "SNC")
        assertThat(found).isEqualTo(party)
    }

    @Test
    fun `When findByAuth then return Party as iterable`() {
        val party = Party(countryCode = "NL", partyID = "XYZ")
        entityManager.persistAndFlush(party)
        val found = partyRepository.findByAuth_TokenA(party.auth.tokenA)
        assertThat(found).isEqualTo(listOf(party))
    }

    @Test
    fun `When findByAuth then return empty iterable`() {
        val party = Party(countryCode = "NL", partyID = "XYZ")
        val result = partyRepository.findByAuth_TokenA(party.auth.tokenA)
        assertThat(result).isEqualTo(listOf<Party>())
    }
}