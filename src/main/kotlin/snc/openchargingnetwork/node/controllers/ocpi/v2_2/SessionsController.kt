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
import snc.openchargingnetwork.node.models.*
import snc.openchargingnetwork.node.models.ocpi.*
import snc.openchargingnetwork.node.components.OcpiRequestHandlerBuilder
import snc.openchargingnetwork.node.tools.filterNull


@RestController
class SessionsController(private val requestHandlerBuilder: OcpiRequestHandlerBuilder) {


    /**
     * SENDER INTERFACE
     */

    @GetMapping("/ocpi/sender/2.2/sessions")
    fun getSessionsFromDataOwner(@RequestHeader("authorization") authorization: String,
                                 @RequestHeader("OCN-Signature") signature: String? = null,
                                 @RequestHeader("X-Request-ID") requestID: String,
                                 @RequestHeader("X-Correlation-ID") correlationID: String,
                                 @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
                                 @RequestHeader("OCPI-from-party-id") fromPartyID: String,
                                 @RequestHeader("OCPI-to-country-code") toCountryCode: String,
                                 @RequestHeader("OCPI-to-party-id") toPartyID: String,
                                 @RequestParam("date_from", required = false) dateFrom: String?,
                                 @RequestParam("date_to", required = false) dateTo: String?,
                                 @RequestParam("offset", required = false) offset: Int?,
                                 @RequestParam("limit", required = false) limit: Int?): ResponseEntity<OcpiResponse<Array<Session>>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        val params = mapOf("date_from" to dateFrom, "date_to" to dateTo, "offset" to offset, "limit" to limit).filterNull()

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.SESSIONS,
                interfaceRole = InterfaceRole.SENDER,
                method = HttpMethod.GET,
                headers = OcnHeaders(authorization, signature, requestID, correlationID, sender, receiver),
                urlEncodedParams = params)

        return requestHandlerBuilder
                .build<Array<Session>>(requestVariables)
                .forward()
                .getResponseWithPaginationHeaders() // proxies Link response header
    }

    @GetMapping("/ocpi/sender/2.2/sessions/page/{uid}")
    fun getSessionsPageFromDataOwner(@RequestHeader("authorization") authorization: String,
                                     @RequestHeader("OCN-Signature") signature: String? = null,
                                     @RequestHeader("X-Request-ID") requestID: String,
                                     @RequestHeader("X-Correlation-ID") correlationID: String,
                                     @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
                                     @RequestHeader("OCPI-from-party-id") fromPartyID: String,
                                     @RequestHeader("OCPI-to-country-code") toCountryCode: String,
                                     @RequestHeader("OCPI-to-party-id") toPartyID: String,
                                     @PathVariable uid: String): ResponseEntity<OcpiResponse<Array<Session>>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.SESSIONS,
                interfaceRole = InterfaceRole.SENDER,
                method = HttpMethod.GET,
                headers = OcnHeaders(authorization, signature, requestID, correlationID, sender, receiver),
                urlPathVariables = uid)


        return requestHandlerBuilder
                .build<Array<Session>>(requestVariables)
                .forward(proxied = true) // retrieves proxied Link response header
                .getResponseWithPaginationHeaders()
    }

    @PutMapping("/ocpi/sender/2.2/sessions/{sessionID}/charging_preferences")
    fun putChargingPreferences(@RequestHeader("authorization") authorization: String,
                               @RequestHeader("OCN-Signature") signature: String? = null,
                               @RequestHeader("X-Request-ID") requestID: String,
                               @RequestHeader("X-Correlation-ID") correlationID: String,
                               @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
                               @RequestHeader("OCPI-from-party-id") fromPartyID: String,
                               @RequestHeader("OCPI-to-country-code") toCountryCode: String,
                               @RequestHeader("OCPI-to-party-id") toPartyID: String,
                               @PathVariable sessionID: String,
                               @RequestBody body: ChargingPreferences): ResponseEntity<OcpiResponse<ChargingPreferencesResponse>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.SESSIONS,
                interfaceRole = InterfaceRole.SENDER,
                method = HttpMethod.PUT,
                headers = OcnHeaders(authorization, signature, requestID, correlationID, sender, receiver),
                urlPathVariables = "/$sessionID/charging_preferences",
                body = body)

        return requestHandlerBuilder
                .build<ChargingPreferencesResponse>(requestVariables)
                .forward()
                .getResponse()
    }


    /**
     * RECEIVER INTERFACE
     */

    @GetMapping("/ocpi/receiver/2.2/sessions/{countryCode}/{partyID}/{sessionID}")
    fun getClientOwnedSession(@RequestHeader("authorization") authorization: String,
                              @RequestHeader("OCN-Signature") signature: String? = null,
                              @RequestHeader("X-Request-ID") requestID: String,
                              @RequestHeader("X-Correlation-ID") correlationID: String,
                              @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
                              @RequestHeader("OCPI-from-party-id") fromPartyID: String,
                              @RequestHeader("OCPI-to-country-code") toCountryCode: String,
                              @RequestHeader("OCPI-to-party-id") toPartyID: String,
                              @PathVariable countryCode: String,
                              @PathVariable partyID: String,
                              @PathVariable sessionID: String): ResponseEntity<OcpiResponse<Session>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.SESSIONS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.GET,
                headers = OcnHeaders(authorization, signature, requestID, correlationID, sender, receiver),
                urlPathVariables = "/$countryCode/$partyID/$sessionID")

        return requestHandlerBuilder
                .build<Session>(requestVariables)
                .forward()
                .getResponse()
    }

    @PutMapping("/ocpi/receiver/2.2/sessions/{countryCode}/{partyID}/{sessionID}")
    fun putClientOwnedSession(@RequestHeader("authorization") authorization: String,
                              @RequestHeader("OCN-Signature") signature: String? = null,
                              @RequestHeader("X-Request-ID") requestID: String,
                              @RequestHeader("X-Correlation-ID") correlationID: String,
                              @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
                              @RequestHeader("OCPI-from-party-id") fromPartyID: String,
                              @RequestHeader("OCPI-to-country-code") toCountryCode: String,
                              @RequestHeader("OCPI-to-party-id") toPartyID: String,
                              @PathVariable countryCode: String,
                              @PathVariable partyID: String,
                              @PathVariable sessionID: String,
                              @RequestBody body: Session): ResponseEntity<OcpiResponse<Unit>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.SESSIONS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.PUT,
                headers = OcnHeaders(authorization, signature, requestID, correlationID, sender, receiver),
                urlPathVariables = "/$countryCode/$partyID/$sessionID",
                body = body)

        return requestHandlerBuilder
                .build<Unit>(requestVariables)
                .forward()
                .getResponse()
    }

    @PatchMapping("/ocpi/receiver/2.2/sessions/{countryCode}/{partyID}/{sessionID}")
    fun patchClientOwnedSession(@RequestHeader("authorization") authorization: String,
                                @RequestHeader("OCN-Signature") signature: String? = null,
                                @RequestHeader("X-Request-ID") requestID: String,
                                @RequestHeader("X-Correlation-ID") correlationID: String,
                                @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
                                @RequestHeader("OCPI-from-party-id") fromPartyID: String,
                                @RequestHeader("OCPI-to-country-code") toCountryCode: String,
                                @RequestHeader("OCPI-to-party-id") toPartyID: String,
                                @PathVariable countryCode: String,
                                @PathVariable partyID: String,
                                @PathVariable sessionID: String,
                                @RequestBody body: Map<String, Any>): ResponseEntity<OcpiResponse<Unit>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.SESSIONS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.PATCH,
                headers = OcnHeaders(authorization, signature, requestID, correlationID, sender, receiver),
                urlPathVariables = "/$countryCode/$partyID/$sessionID",
                body = body)

        return requestHandlerBuilder
                .build<Unit>(requestVariables)
                .forward()
                .getResponse()
    }

}