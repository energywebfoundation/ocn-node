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
import snc.openchargingnetwork.node.tools.filterNull

@RestController
@RequestMapping("\${ocn.node.apiPrefix}")
class TokensController(private val requestHandlerBuilder: OcpiRequestHandlerBuilder) {


    /**
     * SENDER INTERFACE
     */

    @GetMapping("/ocpi/sender/2.2/tokens")
    fun getTokensFromDataOwner(@RequestHeader("authorization") authorization: String,
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
                               @RequestParam("limit", required = false) limit: Int?): ResponseEntity<OcpiResponse<Array<Token>>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        val params = mapOf("date_from" to dateFrom, "date_to" to dateTo, "offset" to offset, "limit" to limit).filterNull()

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.TOKENS,
                interfaceRole = InterfaceRole.SENDER,
                method = HttpMethod.GET,
                headers = OcnHeaders(authorization, signature, requestID, correlationID, sender, receiver),
                queryParams = params)

        return requestHandlerBuilder
                .build<Array<Token>>(requestVariables)
                .forwardDefault()
                .getResponseWithPaginationHeaders() // proxies Link response header
    }

    @GetMapping("/ocpi/sender/2.2/tokens/page/{uid}")
    fun getTokensPageFromDataOwner(@RequestHeader("authorization") authorization: String,
                                   @RequestHeader("OCN-Signature") signature: String? = null,
                                   @RequestHeader("X-Request-ID") requestID: String,
                                   @RequestHeader("X-Correlation-ID") correlationID: String,
                                   @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
                                   @RequestHeader("OCPI-from-party-id") fromPartyID: String,
                                   @RequestHeader("OCPI-to-country-code") toCountryCode: String,
                                   @RequestHeader("OCPI-to-party-id") toPartyID: String,
                                   @PathVariable uid: String): ResponseEntity<OcpiResponse<Array<Token>>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.TOKENS,
                interfaceRole = InterfaceRole.SENDER,
                method = HttpMethod.GET,
                headers = OcnHeaders(authorization, signature, requestID, correlationID, sender, receiver),
                urlPath = uid)

        return requestHandlerBuilder
                .build<Array<Token>>(requestVariables)
                .forwardDefault(proxied = true) // retrieves proxied Link response header
                .getResponseWithPaginationHeaders()
    }

    @PostMapping("/ocpi/sender/2.2/tokens/{tokenUID}/authorize")
    fun postRealTimeTokenAuthorization(@RequestHeader("authorization") authorization: String,
                                       @RequestHeader("OCN-Signature") signature: String? = null,
                                       @RequestHeader("X-Request-ID") requestID: String,
                                       @RequestHeader("X-Correlation-ID") correlationID: String,
                                       @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
                                       @RequestHeader("OCPI-from-party-id") fromPartyID: String,
                                       @RequestHeader("OCPI-to-country-code") toCountryCode: String,
                                       @RequestHeader("OCPI-to-party-id") toPartyID: String,
                                       @PathVariable tokenUID: String,
                                       @RequestParam("type", required = false, defaultValue = "RFID") type: TokenType? = null,
                                       @RequestBody body: LocationReferences? = null): ResponseEntity<OcpiResponse<AuthorizationInfo>> {


        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.TOKENS,
                interfaceRole = InterfaceRole.SENDER,
                method = HttpMethod.POST,
                headers = OcnHeaders(authorization, signature, requestID, correlationID, sender, receiver),
                urlPath = "$tokenUID/authorize",
                queryParams = mapOf("type" to type),
                body = body)

        return requestHandlerBuilder
                .build<AuthorizationInfo>(requestVariables)
                .forwardDefault()
                .getResponse()
    }


    /**
     * RECEIVER INTERFACE
     */

    @GetMapping("/ocpi/receiver/2.2/tokens/{countryCode}/{partyID}/{tokenUID}")
    fun getClientOwnedToken(@RequestHeader("authorization") authorization: String,
                            @RequestHeader("OCN-Signature") signature: String? = null,
                            @RequestHeader("X-Request-ID") requestID: String,
                            @RequestHeader("X-Correlation-ID") correlationID: String,
                            @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
                            @RequestHeader("OCPI-from-party-id") fromPartyID: String,
                            @RequestHeader("OCPI-to-country-code") toCountryCode: String,
                            @RequestHeader("OCPI-to-party-id") toPartyID: String,
                            @PathVariable countryCode: String,
                            @PathVariable partyID: String,
                            @PathVariable tokenUID: String,
                            @RequestParam("type", required = false, defaultValue = "RFID") type: TokenType? = null): ResponseEntity<OcpiResponse<Token>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.TOKENS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.GET,
                headers = OcnHeaders(authorization, signature, requestID, correlationID, sender, receiver),
                urlPath = "/$countryCode/$partyID/$tokenUID",
                queryParams = mapOf("type" to type))

        return requestHandlerBuilder
                .build<Token>(requestVariables)
                .forwardDefault()
                .getResponse()
    }

    @PutMapping("/ocpi/receiver/2.2/tokens/{countryCode}/{partyID}/{tokenUID}")
    fun putClientOwnedToken(@RequestHeader("authorization") authorization: String,
                            @RequestHeader("OCN-Signature") signature: String? = null,
                            @RequestHeader("X-Request-ID") requestID: String,
                            @RequestHeader("X-Correlation-ID") correlationID: String,
                            @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
                            @RequestHeader("OCPI-from-party-id") fromPartyID: String,
                            @RequestHeader("OCPI-to-country-code") toCountryCode: String,
                            @RequestHeader("OCPI-to-party-id") toPartyID: String,
                            @PathVariable countryCode: String,
                            @PathVariable partyID: String,
                            @PathVariable tokenUID: String,
                            @RequestParam("type", required = false, defaultValue = "RFID") type: TokenType? = null,
                            @RequestBody body: Token): ResponseEntity<OcpiResponse<Unit>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.TOKENS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.PUT,
                headers = OcnHeaders(authorization, signature, requestID, correlationID, sender, receiver),
                urlPath = "/$countryCode/$partyID/$tokenUID",
                queryParams = mapOf("type" to type),
                body = body)

        return requestHandlerBuilder
                .build<Unit>(requestVariables)
                .forwardDefault()
                .getResponse()
    }

    @PatchMapping("/ocpi/receiver/2.2/tokens/{countryCode}/{partyID}/{tokenUID}")
    fun patchClientOwnedToken(@RequestHeader("authorization") authorization: String,
                              @RequestHeader("OCN-Signature") signature: String? = null,
                               @RequestHeader("X-Request-ID") requestID: String,
                               @RequestHeader("X-Correlation-ID") correlationID: String,
                               @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
                               @RequestHeader("OCPI-from-party-id") fromPartyID: String,
                               @RequestHeader("OCPI-to-country-code") toCountryCode: String,
                               @RequestHeader("OCPI-to-party-id") toPartyID: String,
                               @PathVariable countryCode: String,
                               @PathVariable partyID: String,
                               @PathVariable tokenUID: String,
                               @RequestParam("type", required = false, defaultValue = "RFID") type: TokenType? = null,
                               @RequestBody body: Map<String, Any>): ResponseEntity<OcpiResponse<Unit>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.TOKENS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.PATCH,
                headers = OcnHeaders(authorization, signature, requestID, correlationID, sender, receiver),
                urlPath = "/$countryCode/$partyID/$tokenUID",
                queryParams = mapOf("type" to type),
                body = body)

        return requestHandlerBuilder
                .build<Unit>(requestVariables)
                .forwardDefault()
                .getResponse()
    }

}