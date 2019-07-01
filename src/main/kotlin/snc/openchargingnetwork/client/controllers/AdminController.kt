package snc.openchargingnetwork.client.controllers

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import snc.openchargingnetwork.client.repositories.RoleRepository
import snc.openchargingnetwork.client.repositories.PlatformRepository
import snc.openchargingnetwork.client.models.ocpi.BasicRole
import snc.openchargingnetwork.client.config.Properties
import snc.openchargingnetwork.client.models.entities.Auth
import snc.openchargingnetwork.client.models.entities.PlatformEntity
import snc.openchargingnetwork.client.models.ocpi.RegistrationInfo
import snc.openchargingnetwork.client.tools.generateUUIDv4Token
import snc.openchargingnetwork.client.tools.urlJoin

// TODO: do not apply OCPI exception handler to admin controller
@RestController
@RequestMapping("/admin")
class AdminController(private val platformRepo: PlatformRepository,
                      private val roleRepo: RoleRepository,
                      private val properties: Properties) {

    fun isAuthorized(authorization: String): Boolean {
        return authorization == "Token ${properties.apikey}"
    }

    @PostMapping("/generate-registration-token")
    @Transactional
    fun generateRegistrationToken(@RequestHeader("Authorization") authorization: String,
                                  @RequestBody body: Array<BasicRole>
    ): ResponseEntity<Any> {

        // check admin is authorized
        if (!isAuthorized(authorization)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized")
        }

        // check each role does not already exist
        for (role in body) {
            if (roleRepo.existsByCountryCodeAndPartyIDAllIgnoreCase(role.country, role.id)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Role $role already exists")
            }
        }

        // generate and store new platform with authorization token
        //TODO: schedule deletion after 30 days if status still PLANNED
        val tokenA = generateUUIDv4Token()
        val platform = PlatformEntity(auth = Auth(tokenA = tokenA))
        platformRepo.save(platform)

        val responseBody = RegistrationInfo(tokenA, urlJoin(properties.url, "/ocpi/hub/versions"))
        return ResponseEntity.ok().body(responseBody)
    }

}