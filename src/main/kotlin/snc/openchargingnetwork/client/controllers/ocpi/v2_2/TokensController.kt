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

package snc.openchargingnetwork.client.controllers.ocpi.v2_2

import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import snc.openchargingnetwork.client.models.HubGenericRequest
import snc.openchargingnetwork.client.models.HubRequestResponseType
import snc.openchargingnetwork.client.models.ocpi.*
import snc.openchargingnetwork.client.services.RoutingService
import snc.openchargingnetwork.client.tools.urlJoin

@RestController
class TokensController(private val routingService: RoutingService) {

    /**
     * SENDER INTERFACE
     */

    @GetMapping("/ocpi/sender/2.2/tokens")
    fun getTokensFromDataOwner(@RequestHeader("authorization") authorization: String,
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

        routingService.validateSender(authorization, sender)

        val params = PaginatedRequest(dateFrom, dateTo, offset, limit).encode()

        val response = if (routingService.isRoleKnown(receiver)) {
            val platformID = routingService.getPlatformID(receiver)
            val endpoint = routingService.getPlatformEndpoint(platformID, "tokens", InterfaceRole.SENDER)
            val headers = routingService.makeHeaders(platformID, correlationID, sender, receiver)
            routingService.forwardRequest(
                    method = "GET",
                    url = endpoint.url,
                    headers = headers,
                    params = params,
                    expectedDataType = Array<Token>::class)
        } else {
            val url = routingService.findBrokerUrl(receiver)
            val headers = routingService.makeHeaders(correlationID, sender, receiver)
            routingService.forwardRequest(
                    method = "POST",
                    url = urlJoin(url, "/ocn/message"),
                    headers = headers,
                    body = HubGenericRequest(
                            method = "GET",
                            module = "tokens",
                            role = InterfaceRole.SENDER,
                            params = params,
                            body = null,
                            expectedResponseType = HubRequestResponseType.TOKEN_ARRAY),
                    expectedDataType = Array<Token>::class)
        }

        val headers = HttpHeaders()
        response.headers["Link"]?.let { headers.add("Link", "<RESPONSE_URL>; rel=\"next\"")}
        response.headers["X-Total-Count"]?.let { headers.add("X-Total-Count", it) }
        response.headers["X-Limit"]?.let { headers.add("X-Limit", it) }

        return ResponseEntity
                .status(response.statusCode)
                .headers(headers)
                .body(response.body)
    }

    @PutMapping("/ocpi/sender/2.2/sessions/{tokenUID}/authorize")
    fun postRealTimeTokenAuthorization(@RequestHeader("authorization") authorization: String,
                                       @RequestHeader("X-Request-ID") requestID: String,
                                       @RequestHeader("X-Correlation-ID") correlationID: String,
                                       @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
                                       @RequestHeader("OCPI-from-party-id") fromPartyID: String,
                                       @RequestHeader("OCPI-to-country-code") toCountryCode: String,
                                       @RequestHeader("OCPI-to-party-id") toPartyID: String,
                                       @PathVariable tokenUID: String,
                                       @RequestParam("type", required = false) type: TokenType = TokenType.RFID,
                                       @RequestBody body: LocationReferences? = null): ResponseEntity<OcpiResponse<AuthorizationInfo>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        routingService.validateSender(authorization, sender)

        val response = if (routingService.isRoleKnown(receiver)) {
            val platformID = routingService.getPlatformID(receiver)
            val endpoint = routingService.getPlatformEndpoint(platformID, "tokens", InterfaceRole.SENDER)
            val headers = routingService.makeHeaders(platformID, correlationID, sender, receiver)
            routingService.forwardRequest(
                    method = "POST",
                    url = urlJoin(endpoint.url, "/$tokenUID/authorize"),
                    headers = headers,
                    params = mapOf("type" to type.toString()),
                    body = body,
                    expectedDataType = AuthorizationInfo::class)
        } else {
            val url = routingService.findBrokerUrl(receiver)
            val headers = routingService.makeHeaders(correlationID, sender, receiver)
            routingService.forwardRequest(
                    method = "POST",
                    url = urlJoin(url, "/ocn/message"),
                    headers = headers,
                    body = HubGenericRequest(
                            method = "POST",
                            module = "tokens",
                            role = InterfaceRole.SENDER,
                            path = "/$tokenUID/authorization",
                            params = mapOf("type" to type.toString()),
                            body = body,
                            expectedResponseType = HubRequestResponseType.AUTHORIZATION_INFO),
                    expectedDataType = AuthorizationInfo::class)
        }

        return ResponseEntity.status(response.statusCode).body(response.body)
    }

    /**
     * RECEIVER INTERFACE
     */

    @GetMapping("/ocpi/receiver/2.2/tokens/{countryCode}/{partyID}/{tokenUID}")
    fun getClientOwnedToken(@RequestHeader("authorization") authorization: String,
                            @RequestHeader("X-Request-ID") requestID: String,
                            @RequestHeader("X-Correlation-ID") correlationID: String,
                            @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
                            @RequestHeader("OCPI-from-party-id") fromPartyID: String,
                            @RequestHeader("OCPI-to-country-code") toCountryCode: String,
                            @RequestHeader("OCPI-to-party-id") toPartyID: String,
                            @PathVariable countryCode: String,
                            @PathVariable partyID: String,
                            @PathVariable tokenUID: String,
                            @RequestParam("type", required = false) type: TokenType = TokenType.RFID): ResponseEntity<OcpiResponse<Token>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)
        val objectOwner = BasicRole(partyID, countryCode)

        routingService.validateSender(authorization, sender, objectOwner)

        val response = if (routingService.isRoleKnown(receiver)) {
            val platformID = routingService.getPlatformID(receiver)
            val endpoint = routingService.getPlatformEndpoint(platformID, "tokens", InterfaceRole.RECEIVER)
            val headers = routingService.makeHeaders(platformID, correlationID, sender, receiver)
            routingService.forwardRequest(
                    method = "GET",
                    url = urlJoin(endpoint.url, "/$countryCode/$partyID/$tokenUID"),
                    headers = headers,
                    params = mapOf("type" to type.toString()),
                    expectedDataType = Token::class)
        } else {
            val url = routingService.findBrokerUrl(receiver)
            val headers = routingService.makeHeaders(correlationID, sender, receiver)
            routingService.forwardRequest(
                    method = "POST",
                    url = urlJoin(url, "/ocn/message"),
                    headers = headers,
                    body = HubGenericRequest(
                            method = "GET",
                            module = "tokens",
                            path = urlJoin(url, "/$countryCode/$partyID/$tokenUID"),
                            params = mapOf("type" to type.toString()),
                            body = null,
                            role = InterfaceRole.RECEIVER,
                            expectedResponseType = HubRequestResponseType.TOKEN),
                    expectedDataType = Token::class)
        }

        return ResponseEntity.status(response.statusCode).body(response.body)
    }

    @PutMapping("/ocpi/receiver/2.2/tokens/{countryCode}/{partyID}/{tokenUID}")
    fun putClientOwnedToken(@RequestHeader("authorization") authorization: String,
                            @RequestHeader("X-Request-ID") requestID: String,
                            @RequestHeader("X-Correlation-ID") correlationID: String,
                            @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
                            @RequestHeader("OCPI-from-party-id") fromPartyID: String,
                            @RequestHeader("OCPI-to-country-code") toCountryCode: String,
                            @RequestHeader("OCPI-to-party-id") toPartyID: String,
                            @PathVariable countryCode: String,
                            @PathVariable partyID: String,
                            @PathVariable tokenUID: String,
                            @RequestParam("type") type: TokenType = TokenType.RFID,
                            @RequestBody body: Token): ResponseEntity<OcpiResponse<Nothing>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)
        val objectOwner = BasicRole(partyID, countryCode)
        val objectData = BasicRole(body.partyID, body.countryCode)

        routingService.validateSender(authorization, sender, objectOwner, objectData)

        val response = if (routingService.isRoleKnown(receiver)) {
            val platformID = routingService.getPlatformID(receiver)
            val endpoint = routingService.getPlatformEndpoint(platformID, "tokens", InterfaceRole.RECEIVER)
            val headers = routingService.makeHeaders(platformID, correlationID, sender, receiver)
            routingService.forwardRequest(
                    method = "PUT",
                    url = urlJoin(endpoint.url, "/$countryCode/$partyID/$tokenUID"),
                    headers = headers,
                    params = mapOf("type" to type.toString()),
                    body = body,
                    expectedDataType = Nothing::class)
        } else {
            val url = routingService.findBrokerUrl(receiver)
            val headers = routingService.makeHeaders(correlationID, sender, receiver)
            routingService.forwardRequest(
                    method = "POST",
                    url = urlJoin(url, "/ocn/message"),
                    headers = headers,
                    body = HubGenericRequest(
                            method = "PUT",
                            module = "tokens",
                            path = urlJoin(url, "/$countryCode/$partyID/$tokenUID"),
                            params = mapOf("type" to type.toString()),
                            role = InterfaceRole.RECEIVER,
                            body = body),
                    expectedDataType = Nothing::class)
        }

        return ResponseEntity.status(response.statusCode).body(response.body)
    }

    @PatchMapping("/ocpi/receiver/2.2/tokens/{countryCode}/{partyID}/{tokenUID}")
    fun patchClientOwnedToken(@RequestHeader("authorization") authorization: String,
                               @RequestHeader("X-Request-ID") requestID: String,
                               @RequestHeader("X-Correlation-ID") correlationID: String,
                               @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
                               @RequestHeader("OCPI-from-party-id") fromPartyID: String,
                               @RequestHeader("OCPI-to-country-code") toCountryCode: String,
                               @RequestHeader("OCPI-to-party-id") toPartyID: String,
                               @PathVariable countryCode: String,
                               @PathVariable partyID: String,
                               @PathVariable tokenUID: String,
                               @RequestParam("type") type: TokenType = TokenType.RFID,
                               @RequestBody body: Map<String, Any>): ResponseEntity<OcpiResponse<Nothing>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)
        val objectOwner = BasicRole(partyID, countryCode)

        routingService.validateSender(authorization, sender, objectOwner)

        val response = if (routingService.isRoleKnown(receiver)) {
            val platformID = routingService.getPlatformID(receiver)
            val endpoint = routingService.getPlatformEndpoint(platformID, "tokens", InterfaceRole.RECEIVER)
            val headers = routingService.makeHeaders(platformID, correlationID, sender, receiver)
            routingService.forwardRequest(
                    method = "PATCH",
                    url = urlJoin(endpoint.url, "/$countryCode/$partyID/$tokenUID"),
                    headers = headers,
                    params = mapOf("type" to type.toString()),
                    body = body,
                    expectedDataType = Nothing::class)
        } else {
            val url = routingService.findBrokerUrl(receiver)
            val headers = routingService.makeHeaders(correlationID, sender, receiver)
            routingService.forwardRequest(
                    method = "POST",
                    url = urlJoin(url, "/ocn/message"),
                    headers = headers,
                    body = HubGenericRequest(
                            method = "PATCH",
                            module = "tokens",
                            path = urlJoin(url, "/$countryCode/$partyID/$tokenUID"),
                            params = mapOf("type" to type.toString()),
                            role = InterfaceRole.RECEIVER,
                            body = body),
                    expectedDataType = Nothing::class)
        }

        return ResponseEntity.status(response.statusCode).body(response.body)
    }


}