/*
    Copyright 2019 Share&Charge Foundation

    This file is part of Open Charging Network Node.

    Open Charging Network Node is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Open Charging Network Node is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Open Charging Network Node.  If not, see <https://www.gnu.org/licenses/>.
*/

package snc.openchargingnetwork.node.controllers

import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import snc.openchargingnetwork.node.repositories.RoleRepository
import snc.openchargingnetwork.node.repositories.PlatformRepository
import snc.openchargingnetwork.node.models.ocpi.BasicRole
import snc.openchargingnetwork.node.config.NodeProperties
import snc.openchargingnetwork.node.models.entities.Auth
import snc.openchargingnetwork.node.models.entities.PlatformEntity
import snc.openchargingnetwork.node.models.ocpi.RegistrationInfo
import snc.openchargingnetwork.node.tools.generateUUIDv4Token
import snc.openchargingnetwork.node.tools.urlJoin



@RestController
@RequestMapping("/admin")
class AdminController(private val platformRepo: PlatformRepository,
                      private val roleRepo: RoleRepository,
                      private val properties: NodeProperties) {

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