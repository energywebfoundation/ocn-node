/*
    Copyright 2019-2020 eMobility GmbH

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

package snc.openchargingnetwork.node.controllers.ocpi.v2_2

import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import snc.openchargingnetwork.node.models.OcnRules
import snc.openchargingnetwork.node.models.OcnRulesListParty
import snc.openchargingnetwork.node.models.ocpi.BasicRole
import snc.openchargingnetwork.node.models.ocpi.OcpiResponse
import snc.openchargingnetwork.node.services.OcnRulesService

@RequestMapping("\${ocn.node.apiPrefix}")
@RestController
class OcnRulesController(private val ocnRulesService: OcnRulesService) {

    @GetMapping("/ocpi/receiver/2.2.1/ocnrules")
    fun getRules(@RequestHeader("authorization") authorization: String): ResponseEntity<OcpiResponse<OcnRules>> {
        return ResponseEntity.ok(
            OcpiResponse(
                statusCode = 1000,
                data = ocnRulesService.getRules(authorization)
            )
        )
    }

    @Transactional
    @PostMapping("/ocpi/receiver/2.2.1/ocnrules/whitelist")
    fun appendToWhitelist(
        @RequestHeader("authorization") authorization: String,
        @RequestBody body: OcnRulesListParty
    ): ResponseEntity<OcpiResponse<Unit>> {

        ocnRulesService.appendToWhitelist(authorization, body)
        return ResponseEntity.ok(OcpiResponse(statusCode = 1000))
    }

    @Transactional
    @PostMapping("/ocpi/receiver/2.2.1/ocnrules/blacklist")
    fun appendToBlacklist(
        @RequestHeader("authorization") authorization: String,
        @RequestBody body: OcnRulesListParty
    ): ResponseEntity<OcpiResponse<Unit>> {

        ocnRulesService.appendToBlacklist(authorization, body)
        return ResponseEntity.ok(OcpiResponse(statusCode = 1000))
    }

    @Transactional
    @PutMapping("/ocpi/receiver/2.2.1/ocnrules/signatures")
    fun updateSignatures(@RequestHeader("authorization") authorization: String): ResponseEntity<OcpiResponse<Unit>> {

        ocnRulesService.updateSignatures(authorization)
        return ResponseEntity.ok(OcpiResponse(statusCode = 1000))
    }

    @Transactional
    @PutMapping("/ocpi/receiver/2.2.1/ocnrules/whitelist")
    fun updateWhitelist(
        @RequestHeader("authorization") authorization: String,
        @RequestBody body: List<OcnRulesListParty>
    ): ResponseEntity<OcpiResponse<Unit>> {

        ocnRulesService.updateWhitelist(authorization, body)
        return ResponseEntity.ok(OcpiResponse(statusCode = 1000))
    }

    @Transactional
    @PutMapping("/ocpi/receiver/2.2.1/ocnrules/blacklist")
    fun updateBlacklist(
        @RequestHeader("authorization") authorization: String,
        @RequestBody body: List<OcnRulesListParty>
    ): ResponseEntity<OcpiResponse<Unit>> {

        ocnRulesService.updateBlacklist(authorization, body)
        return ResponseEntity.ok(OcpiResponse(statusCode = 1000))
    }

    @Transactional
    @PutMapping("/ocpi/receiver/2.2.1/ocnrules/block-all")
    fun blockAll(@RequestHeader("authorization") authorization: String): ResponseEntity<OcpiResponse<Unit>> {

        ocnRulesService.blockAll(authorization)
        return ResponseEntity.ok(OcpiResponse(statusCode = 1000))
    }

    @Transactional
    @DeleteMapping("ocpi/receiver/2.2.1/ocnrules/whitelist/{countryCode}/{partyID}")
    fun deleteFromWhitelist(
        @RequestHeader("authorization") authorization: String,
        @PathVariable countryCode: String,
        @PathVariable partyID: String
    ): ResponseEntity<OcpiResponse<Unit>> {

        val party = BasicRole(country = countryCode, id = partyID).uppercase()
        ocnRulesService.deleteFromWhitelist(authorization, party)
        return ResponseEntity.ok(OcpiResponse(statusCode = 1000))
    }

    @Transactional
    @DeleteMapping("ocpi/receiver/2.2.1/ocnrules/blacklist/{countryCode}/{partyID}")
    fun deleteFromBlacklist(
        @RequestHeader("authorization") authorization: String,
        @PathVariable countryCode: String,
        @PathVariable partyID: String
    ): ResponseEntity<OcpiResponse<Unit>> {

        val party = BasicRole(country = countryCode, id = partyID).uppercase()
        ocnRulesService.deleteFromBlacklist(authorization, party)
        return ResponseEntity.ok(OcpiResponse(statusCode = 1000))
    }
}