package snc.connect.broker

import org.springframework.data.repository.CrudRepository
import snc.connect.broker.models.entities.Party

interface PartyRepository: CrudRepository<Party, Long> {
    fun findByCountryCodeAndPartyID(countryCode: String, partyID: String): Party?
    fun findByAuth_TokenA(tokenA: String?): Party?
}