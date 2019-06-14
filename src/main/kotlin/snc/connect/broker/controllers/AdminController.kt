package snc.connect.broker.controllers

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import snc.connect.broker.PartyRepository
import snc.connect.broker.models.entities.Party
import snc.connect.broker.models.ocpi.BasicParty
import snc.connect.broker.Properties
import snc.connect.broker.models.entities.Auth
import snc.connect.broker.models.ocpi.RegistrationInformation
import snc.connect.broker.tools.generateUUIDv4Token
import snc.connect.broker.tools.urlJoin

@RestController
@RequestMapping("/admin")
class AdminController(private val repository: PartyRepository,
                      private val properties: Properties) {

    fun isAuthorized(authorization: String): Boolean {
        return authorization == "Token ${properties.apikey}"
    }

    @PostMapping("/generate-registration-token")
    fun generateRegistrationToken(@RequestHeader("Authorization") authorization: String,
                                  @RequestBody body: Array<BasicParty>
    ): ResponseEntity<RegistrationInformation> {

        // check admin is authorized
        if (!isAuthorized(authorization)) {
            return ResponseEntity(HttpStatus.UNAUTHORIZED)
        }

        // check party has no token already
        for (basicParty in body) {
            repository.findByCountryCodeAndPartyID(basicParty.country, basicParty.id)?.let {
                return ResponseEntity(HttpStatus.BAD_REQUEST)
            }
        }

        // store registration token for multiple parties under the same connection
        //TODO: remove tokenA after given time? (party must re-register)
        val tokenA = generateUUIDv4Token()
        for (basicParty in body) {
            repository.save(Party(
                    countryCode = basicParty.country,
                    partyID = basicParty.id,
                    auth = Auth(tokenA = tokenA)))
        }

        val responseBody = RegistrationInformation(tokenA, urlJoin(properties.host, "/ocpi/hub/versions"))
        return ResponseEntity.ok().body(responseBody)
    }

}