/*
    Copyright 2019 Energy Web Foundation

    This file is part of Open Charging Network Client.

    Open Charging Network Client is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Open Charging Network Client is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Open Charging Network Client.  If not, see <https://www.gnu.org/licenses/>.
*/

package snc.openchargingnetwork.client.controllers

import org.springframework.data.repository.findByIdOrNull
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
import javax.servlet.http.HttpServletRequest


@RestController
@RequestMapping("/admin")
class AdminController(private val httpProtocol: HttpServletRequest,
                      private val platformRepo: PlatformRepository,
                      private val roleRepo: RoleRepository,
                      private val properties: Properties) {

    fun isAuthorized(authorization: String): Boolean {
        return authorization == "Token ${properties.apikey}"
    }

    @GetMapping("/connection-status/{countryCode}/{partyID}")
    fun getConnectionStatus(@RequestHeader("Authorization") authorization: String,
                            @PathVariable countryCode: String,
                            @PathVariable partyID: String): ResponseEntity<String> {

        // check admin is authorized
        if (!isAuthorized(authorization)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized")
        }

        val role = roleRepo.findByCountryCodeAndPartyIDAllIgnoreCase(countryCode, partyID)
                ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Role not found")

        val platform = platformRepo.findByIdOrNull(role.platformID)
                        ?: return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Could not find connection status")

        return ResponseEntity.ok().body(platform.status.toString())
    }

    @PostMapping("/generate-registration-token")
    @Transactional
    fun generateRegistrationToken(@RequestHeader("Authorization") authorization: String,
                                  @RequestBody body: Array<BasicRole>
    ): ResponseEntity<Any> {

        // check request protocol
        if(httpProtocol.scheme == "http"){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("request protocol should be https")
        }

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

        val responseBody = RegistrationInfo(tokenA, urlJoin(properties.url, "/ocpi/versions"))
        return ResponseEntity.ok().body(responseBody)
    }

}