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

package snc.openchargingnetwork.node.controllers.ocpi.v2_2

import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import snc.openchargingnetwork.node.components.OcpiRequestHandlerBuilder
import snc.openchargingnetwork.node.models.OcnHeaders
import snc.openchargingnetwork.node.models.ocpi.*

@RequestMapping("\${ocn.node.apiPrefix}")
@RestController
class ChargingProfilesController(private val requestHandlerBuilder: OcpiRequestHandlerBuilder) {

    /**
     * SENDER INTERFACE
     */

    @PostMapping("/ocpi/2.2/sender/chargingprofiles/result/{uid}")
    fun postGenericChargingProfileResult(@RequestHeader("Authorization") authorization: String,
                                         @RequestHeader("OCN-Signature") signature: String? = null,
                                         @RequestHeader("X-Request-ID") requestID: String,
                                         @RequestHeader("X-Correlation-ID") correlationID: String,
                                         @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
                                         @RequestHeader("OCPI-from-party-id") fromPartyID: String,
                                         @RequestHeader("OCPI-to-country-code") toCountryCode: String,
                                         @RequestHeader("OCPI-to-party-id") toPartyID: String,
                                         @PathVariable uid: String,
                                         @RequestBody body: GenericChargingProfileResult): ResponseEntity<OcpiResponse<Unit>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.CHARGING_PROFILES,
                interfaceRole = InterfaceRole.SENDER,
                method = HttpMethod.POST,
                headers = OcnHeaders(authorization, signature, requestID, correlationID, sender, receiver),
                urlPath = uid,
                body = body)

        return requestHandlerBuilder
                .build<Unit>(requestVariables)
                .forwardDefault(proxied = true) // retrieves proxied response_url
                .getResponse()
    }

    @PutMapping("/ocpi/2.2/sender/chargingprofiles/{sessionId}")
    fun putSenderChargingProfile(@RequestHeader("Authorization") authorization: String,
                                 @RequestHeader("OCN-Signature") signature: String? = null,
                                 @RequestHeader("X-Request-ID") requestID: String,
                                 @RequestHeader("X-Correlation-ID") correlationID: String,
                                 @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
                                 @RequestHeader("OCPI-from-party-id") fromPartyID: String,
                                 @RequestHeader("OCPI-to-country-code") toCountryCode: String,
                                 @RequestHeader("OCPI-to-party-id") toPartyID: String,
                                 @PathVariable sessionId: String,
                                 @RequestBody body: ActiveChargingProfile): ResponseEntity<OcpiResponse<Unit>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.CHARGING_PROFILES,
                interfaceRole = InterfaceRole.SENDER,
                method = HttpMethod.PUT,
                headers = OcnHeaders(authorization, signature, requestID, correlationID, sender, receiver),
                urlPath = sessionId,
                body = body)

        return requestHandlerBuilder
                .build<Unit>(requestVariables)
                .forwardDefault()
                .getResponse()
    }


    /**
     * RECEIVER INTERFACE
     */

    @GetMapping("/ocpi/2.2/receiver/chargingprofiles/{sessionId}")
    fun getReceiverChargingProfile(@RequestHeader("Authorization") authorization: String,
                                   @RequestHeader("OCN-Signature") signature: String? = null,
                                   @RequestHeader("X-Request-ID") requestID: String,
                                   @RequestHeader("X-Correlation-ID") correlationID: String,
                                   @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
                                   @RequestHeader("OCPI-from-party-id") fromPartyID: String,
                                   @RequestHeader("OCPI-to-country-code") toCountryCode: String,
                                   @RequestHeader("OCPI-to-party-id") toPartyID: String,
                                   @PathVariable sessionId: String,
                                   @RequestParam duration: Int,
                                   @RequestParam("response_url") responseUrl: String): ResponseEntity<OcpiResponse<ChargingProfileResponse>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.CHARGING_PROFILES,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.GET,
                headers = OcnHeaders(authorization, signature, requestID, correlationID, sender, receiver),
                urlPath = sessionId,
                queryParams = mapOf("duration" to duration, "response_url" to responseUrl))

        return requestHandlerBuilder
                .build<ChargingProfileResponse>(requestVariables)
                .forwardAsync(responseUrl) {
                    requestVariables.copy(queryParams = mapOf("duration" to duration, "response_url" to it))
                }
                .getResponse()
    }

    @PutMapping("/ocpi/2.2/receiver/chargingprofiles/{sessionId}")
    fun putReceiverChargingProfile(@RequestHeader("Authorization") authorization: String,
                                   @RequestHeader("OCN-Signature") signature: String? = null,
                                   @RequestHeader("X-Request-ID") requestID: String,
                                   @RequestHeader("X-Correlation-ID") correlationID: String,
                                   @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
                                   @RequestHeader("OCPI-from-party-id") fromPartyID: String,
                                   @RequestHeader("OCPI-to-country-code") toCountryCode: String,
                                   @RequestHeader("OCPI-to-party-id") toPartyID: String,
                                   @PathVariable sessionId: String,
                                   @RequestBody body: SetChargingProfile): ResponseEntity<OcpiResponse<ChargingProfileResponse>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.CHARGING_PROFILES,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.PUT,
                headers = OcnHeaders(authorization, signature, requestID, correlationID, sender, receiver),
                urlPath = sessionId,
                body = body)

        return requestHandlerBuilder
                .build<ChargingProfileResponse>(requestVariables)
                .forwardAsync(body.responseUrl) {
                    requestVariables.copy(body = body.copy(responseUrl = it))
                }
                .getResponse()
    }

    @DeleteMapping("/ocpi/2.2/receiver/chargingprofiles/{sessionId}")
    fun deleteReceiverChargingProfile(@RequestHeader("Authorization") authorization: String,
                                      @RequestHeader("OCN-Signature") signature: String? = null,
                                      @RequestHeader("X-Request-ID") requestID: String,
                                      @RequestHeader("X-Correlation-ID") correlationID: String,
                                      @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
                                      @RequestHeader("OCPI-from-party-id") fromPartyID: String,
                                      @RequestHeader("OCPI-to-country-code") toCountryCode: String,
                                      @RequestHeader("OCPI-to-party-id") toPartyID: String,
                                      @PathVariable sessionId: String,
                                      @RequestParam("response_url") responseUrl: String): ResponseEntity<OcpiResponse<ChargingProfileResponse>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.CHARGING_PROFILES,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.DELETE,
                headers = OcnHeaders(authorization, signature, requestID, correlationID, sender, receiver),
                urlPath = sessionId,
                queryParams = mapOf("response_url" to responseUrl))

        return requestHandlerBuilder
                .build<ChargingProfileResponse>(requestVariables)
                .forwardAsync(responseUrl) {
                    requestVariables.copy(queryParams = mapOf("response_url" to it))
                }
                .getResponse()
    }


}