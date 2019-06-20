package snc.connect.broker.controllers

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import snc.connect.broker.repositories.CredentialRepository
import snc.connect.broker.repositories.OrganizationRepository
import snc.connect.broker.models.ocpi.BasicParty
import snc.connect.broker.Properties
import snc.connect.broker.models.entities.Auth
import snc.connect.broker.models.entities.OrganizationEntity
import snc.connect.broker.models.ocpi.RegistrationInformation
import snc.connect.broker.tools.generateUUIDv4Token
import snc.connect.broker.tools.urlJoin

@RestController
@RequestMapping("/admin")
class AdminController(private val orgRepo: OrganizationRepository,
                      private val credentialRepo: CredentialRepository,
                      private val properties: Properties) {

    fun isAuthorized(authorization: String): Boolean {
        return authorization == "Token ${properties.apikey}"
    }

    @PostMapping("/generate-registration-token")
    fun generateRegistrationToken(@RequestHeader("Authorization") authorization: String,
                                  @RequestBody body: Array<BasicParty>
    ): ResponseEntity<Any> {

        // check admin is authorized
        if (!isAuthorized(authorization)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized")
        }

        // check each party does not already exist
        for (basicParty in body) {
            if (credentialRepo.existsByCountryCodeAndPartyID(basicParty.country, basicParty.id)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Party $basicParty already exists")
            }
        }

        // generate and store new organization with authorization token
        //TODO: schedule deletion after 30 days if status still PLANNED
        val tokenA = generateUUIDv4Token()
        val org = OrganizationEntity(auth = Auth(tokenA = tokenA))
        orgRepo.save(org)

        val responseBody = RegistrationInformation(tokenA, urlJoin(properties.host, "/ocpi/hub/versions"))
        return ResponseEntity.ok().body(responseBody)
    }

}