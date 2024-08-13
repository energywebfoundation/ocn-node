/*
    Copyright 2019-2020 eMobilify GmbH

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
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
import snc.openchargingnetwork.node.tools.toBs64String
import snc.openchargingnetwork.node.tools.urlJoin



@RestController
@RequestMapping("/admin")
class AdminController(private val platformRepo: PlatformRepository,
                      private val roleRepo: RoleRepository,
                      private val properties: NodeProperties) {

    fun isAuthorized(authorization: String): Boolean {
        return authorization == "Token ${properties.apikey}" || authorization == "Token ${properties.base64apiKey}"
    }

    @GetMapping("/connection-status/{countryCode}/{partyID}")
    fun getConnectionStatus(@RequestHeader("Authorization") authorization: String,
                            @PathVariable countryCode: String,
                            @PathVariable partyID: String): ResponseEntity<String> {

        // check admin is authorized
        if (!isAuthorized(authorization)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized")
        }

        val role = roleRepo.findAllByCountryCodeAndPartyIDAllIgnoreCase(countryCode, partyID).firstOrNull()
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
        //TODO: schedule deletion after 30 days if status still PLANNED (?)
        val tokenA = generateUUIDv4Token()
        val platform = PlatformEntity(auth = Auth(tokenA = tokenA.toBs64String()))
        platformRepo.save(platform)

        val responseBody = RegistrationInfo(tokenA, urlJoin(properties.url, "/ocpi/versions"))
        return ResponseEntity.ok().body(responseBody)
    }

}