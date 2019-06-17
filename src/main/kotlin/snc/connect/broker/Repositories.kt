package snc.connect.broker

import org.springframework.data.repository.CrudRepository
import snc.connect.broker.models.entities.Party

interface PartyRepository: CrudRepository<Party, Long> {
    // country code / party id combinations should be unique
    fun findByCountryCodeAndPartyID(countryCode: String, partyID: String): Party?
    // the same token could be used by multiple parties under the same organization with a single OCPI connection
    fun findByAuth_TokenA(tokenA: String?): Iterable<Party>
}