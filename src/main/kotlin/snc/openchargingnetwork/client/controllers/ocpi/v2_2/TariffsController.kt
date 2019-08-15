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

import org.springframework.web.bind.annotation.*
import snc.openchargingnetwork.client.services.RoutingService

@RestController
class TariffsController(val routingService: RoutingService) {

    /**
     * SENDER INTERFACE
     */

//    @GetMapping("/ocpi/sender/2.2/tariffs")
//    fun getTariffsFromDataOwner(@RequestHeader("authorization") authorization: String,
//                                @RequestHeader("X-Request-ID") requestID: String,
//                                @RequestHeader("X-Correlation-ID") correlationID: String,
//                                @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
//                                @RequestHeader("OCPI-from-party-id") fromPartyID: String,
//                                @RequestHeader("OCPI-to-country-code") toCountryCode: String,
//                                @RequestHeader("OCPI-to-party-id") toPartyID: String,
//                                @RequestParam("date_from", required = false) dateFrom: String?,
//                                @RequestParam("date_to", required = false) dateTo: String?,
//                                @RequestParam("offset", required = false) offset: Int?,
//                                @RequestParam("limit", required = false) limit: Int?): ResponseEntity<OcpiResponse<Array<Tariff>>> {
//
//        val sender = BasicRole(fromPartyID, fromCountryCode)
//        val receiver = BasicRole(toPartyID, toCountryCode)
//
//        routingService.validateSender(authorization, sender)
//
//        val params = HubRequestParameters(dateFrom = dateFrom, dateTo = dateTo, offset = offset, limit = limit)
//
//        val response = if (routingService.isRoleKnown(receiver)) {
//            val platformID = routingService.getPlatformID(receiver)
//            val endpoint = routingService.getPlatformEndpoint(platformID, "tariffs", InterfaceRole.SENDER)
//            val headers = routingService.makeHeaders(platformID, correlationID, sender, receiver)
//            routingService.forwardRequest(
//                    method = "GET",
//                    url = endpoint.url,
//                    headers = headers,
//                    params = params.encode(),
//                    expectedDataType = Array<Tariff>::class)
//        } else {
//            val url = routingService.findBrokerUrl(receiver)
//            val headers = routingService.makeHeaders(requestID, correlationID, sender, receiver)
//            val hubRequestBody = HubGenericRequest(
//                    method = "GET",
//                    module = "tariffs",
//                    role = InterfaceRole.SENDER,
//                    params = params,
//                    headers = headers,
//                    body = null,
//                    expectedResponseType = HubRequestResponseType.TARIFF_ARRAY)
//            routingService.forwardRequest(
//                    method = "POST",
//                    url = urlJoin(url, "/ocn/message"),
//                    headers = mapOf(
//                            "X-Request-ID" to generateUUIDv4Token(),
//                            "OCN-Signature" to routingService.signRequest(hubRequestBody)),
//                    body = hubRequestBody,
//                    expectedDataType = Array<Tariff>::class)
//        }
//
//        val headers = HttpHeaders()
//        response.headers["Link"]?.let { headers.add("Link", "<RESPONSE_URL>; rel=\"next\"") }
//        response.headers["X-Total-Count"]?.let { headers.add("X-Total-Count", it) }
//        response.headers["X-Limit"]?.let { headers.add("X-Limit", it) }
//
//        return ResponseEntity
//                .status(response.statusCode)
//                .headers(headers)
//                .body(response.body)
//    }

    /**
     * RECEIVER INTERFACE
     */

//    @GetMapping("/ocpi/receiver/2.2/tariffs/{countryCode}/{partyID}/{tariffID}")
//    fun getClientOwnedTariff(@RequestHeader("authorization") authorization: String,
//                             @RequestHeader("X-Request-ID") requestID: String,
//                             @RequestHeader("X-Correlation-ID") correlationID: String,
//                             @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
//                             @RequestHeader("OCPI-from-party-id") fromPartyID: String,
//                             @RequestHeader("OCPI-to-country-code") toCountryCode: String,
//                             @RequestHeader("OCPI-to-party-id") toPartyID: String,
//                             @PathVariable countryCode: String,
//                             @PathVariable partyID: String,
//                             @PathVariable tariffID: String): ResponseEntity<OcpiResponse<Tariff>> {
//
//        val sender = BasicRole(fromPartyID, fromCountryCode)
//        val receiver = BasicRole(toPartyID, toCountryCode)
//        val objectOwner = BasicRole(partyID, countryCode)
//
//        routingService.validateSender(authorization, sender, objectOwner)
//
//        val response = if (routingService.isRoleKnown(receiver)) {
//            val platformID = routingService.getPlatformID(receiver)
//            val endpoint = routingService.getPlatformEndpoint(platformID, "tariffs", InterfaceRole.RECEIVER)
//            val headers = routingService.makeHeaders(platformID, correlationID, sender, receiver)
//            routingService.forwardRequest(
//                    method = "GET",
//                    url = urlJoin(endpoint.url, "/$countryCode/$partyID/$tariffID"),
//                    headers = headers,
//                    expectedDataType = Tariff::class)
//        } else {
//            val url = routingService.findBrokerUrl(receiver)
//            val headers = routingService.makeHeaders(requestID, correlationID, sender, receiver)
//            val hubRequestBody = HubGenericRequest(
//                    method = "GET",
//                    module = "tariffs",
//                    path = urlJoin(url, "/$countryCode/$partyID/$tariffID"),
//                    headers = headers,
//                    body = null,
//                    role = InterfaceRole.RECEIVER,
//                    expectedResponseType = HubRequestResponseType.TARIFF)
//            routingService.forwardRequest(
//                    method = "POST",
//                    url = urlJoin(url, "/ocn/message"),
//                    headers = mapOf(
//                            "X-Request-ID" to generateUUIDv4Token(),
//                            "OCN-Signature" to routingService.signRequest(hubRequestBody)),
//                    body = hubRequestBody,
//                    expectedDataType = Tariff::class)
//        }
//
//        return ResponseEntity.status(response.statusCode).body(response.body)
//    }
//
//    @PutMapping("/ocpi/receiver/2.2/tariffs/{countryCode}/{partyID}/{tariffID}")
//    fun putClientOwnedTariff(@RequestHeader("authorization") authorization: String,
//                             @RequestHeader("X-Request-ID") requestID: String,
//                             @RequestHeader("X-Correlation-ID") correlationID: String,
//                             @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
//                             @RequestHeader("OCPI-from-party-id") fromPartyID: String,
//                             @RequestHeader("OCPI-to-country-code") toCountryCode: String,
//                             @RequestHeader("OCPI-to-party-id") toPartyID: String,
//                             @PathVariable countryCode: String,
//                             @PathVariable partyID: String,
//                             @PathVariable tariffID: String,
//                             @RequestBody body: Tariff): ResponseEntity<OcpiResponse<Nothing>> {
//
//        val sender = BasicRole(fromPartyID, fromCountryCode)
//        val receiver = BasicRole(toPartyID, toCountryCode)
//        val objectOwner = BasicRole(partyID, countryCode)
//        val objectData = BasicRole(body.partyID, body.countryCode)
//
//        routingService.validateSender(authorization, sender, objectOwner, objectData)
//
//        val response = if (routingService.isRoleKnown(receiver)) {
//            val platformID = routingService.getPlatformID(receiver)
//            val endpoint = routingService.getPlatformEndpoint(platformID, "tariffs", InterfaceRole.RECEIVER)
//            val headers = routingService.makeHeaders(platformID, correlationID, sender, receiver)
//            routingService.forwardRequest(
//                    method = "PUT",
//                    url = urlJoin(endpoint.url, "/$countryCode/$partyID/$tariffID"),
//                    headers = headers,
//                    body = body,
//                    expectedDataType = Nothing::class)
//        } else {
//            val url = routingService.findBrokerUrl(receiver)
//            val headers = routingService.makeHeaders(requestID, correlationID, sender, receiver)
//            val hubRequestBody = HubGenericRequest(
//                    method = "PUT",
//                    module = "tariffs",
//                    path = urlJoin(url, "/$countryCode/$partyID/$tariffID"),
//                    headers = headers,
//                    role = InterfaceRole.RECEIVER,
//                    body = body)
//            routingService.forwardRequest(
//                    method = "POST",
//                    url = urlJoin(url, "/ocn/message"),
//                    headers = mapOf(
//                            "X-Request-ID" to generateUUIDv4Token(),
//                            "OCN-Signature" to routingService.signRequest(hubRequestBody)),
//                    body = hubRequestBody,
//                    expectedDataType = Nothing::class)
//        }
//
//        return ResponseEntity.status(response.statusCode).body(response.body)
//    }
//
//    @DeleteMapping("/ocpi/receiver/2.2/tariffs/{countryCode}/{partyID}/{tariffID}")
//    fun deleteClientOwnedTariff(@RequestHeader("authorization") authorization: String,
//                                @RequestHeader("X-Request-ID") requestID: String,
//                                @RequestHeader("X-Correlation-ID") correlationID: String,
//                                @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
//                                @RequestHeader("OCPI-from-party-id") fromPartyID: String,
//                                @RequestHeader("OCPI-to-country-code") toCountryCode: String,
//                                @RequestHeader("OCPI-to-party-id") toPartyID: String,
//                                @PathVariable countryCode: String,
//                                @PathVariable partyID: String,
//                                @PathVariable tariffID: String): ResponseEntity<OcpiResponse<Nothing>> {
//
//        val sender = BasicRole(fromPartyID, fromCountryCode)
//        val receiver = BasicRole(toPartyID, toCountryCode)
//        val objectOwner = BasicRole(partyID, countryCode)
//
//        routingService.validateSender(authorization, sender, objectOwner)
//
//        val response = if (routingService.isRoleKnown(receiver)) {
//            val platformID = routingService.getPlatformID(receiver)
//            val endpoint = routingService.getPlatformEndpoint(platformID, "tariffs", InterfaceRole.RECEIVER)
//            val headers = routingService.makeHeaders(platformID, correlationID, sender, receiver)
//            routingService.forwardRequest(
//                    method = "DELETE",
//                    url = urlJoin(endpoint.url, "/$countryCode/$partyID/$tariffID"),
//                    headers = headers,
//                    expectedDataType = Nothing::class)
//        } else {
//            val url = routingService.findBrokerUrl(receiver)
//            val headers = routingService.makeHeaders(requestID, correlationID, sender, receiver)
//            val hubRequestBody = HubGenericRequest(
//                    method = "DELETE",
//                    module = "tariffs",
//                    path = urlJoin(url, "/$countryCode/$partyID/$tariffID"),
//                    headers = headers,
//                    body = null,
//                    role = InterfaceRole.RECEIVER)
//            routingService.forwardRequest(
//                    method = "POST",
//                    url = urlJoin(url, "/ocn/message"),
//                    headers = mapOf(
//                            "X-Request-ID" to generateUUIDv4Token(),
//                            "OCN-Signature" to routingService.signRequest(hubRequestBody)),
//                    body = hubRequestBody,
//                    expectedDataType = Nothing::class)
//        }
//
//        return ResponseEntity.status(response.statusCode).body(response.body)
//    }

}