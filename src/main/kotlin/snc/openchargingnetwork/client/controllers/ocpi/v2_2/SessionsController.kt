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
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import snc.openchargingnetwork.client.models.OcpiRequestParameters
import snc.openchargingnetwork.client.models.OcpiRequestType
import snc.openchargingnetwork.client.models.OcpiRequestVariables
import snc.openchargingnetwork.client.models.OcpiResponseDataType
import snc.openchargingnetwork.client.models.ocpi.*
import snc.openchargingnetwork.client.services.HttpRequestService
import snc.openchargingnetwork.client.services.RoutingService
import snc.openchargingnetwork.client.tools.isOcpiSuccess

@RestController
class SessionsController(private val routingService: RoutingService,
                         private val httpService: HttpRequestService) {


    /**
     * SENDER INTERFACE
     */

    @GetMapping("/ocpi/sender/2.2/sessions")
    fun getSessionsFromDataOwner(@RequestHeader("authorization") authorization: String,
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

        routingService.validateSender(authorization, sender)

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.SESSIONS,
                interfaceRole = InterfaceRole.SENDER,
                method = HttpMethod.GET,
                requestID = requestID,
                correlationID = correlationID,
                sender = sender,
                receiver = receiver,
                urlEncodedParameters = OcpiRequestParameters(
                        dateFrom = dateFrom,
                        dateTo = dateTo,
                        offset = offset,
                        limit = limit),
                expectedResponseType = OcpiResponseDataType.SESSION_ARRAY)

        val response = when (routingService.validateReceiver(receiver)) {

            OcpiRequestType.LOCAL -> {

                val (url, headers) = routingService.prepareLocalPlatformRequest(requestVariables)

                httpService.makeRequest(
                        method = requestVariables.method,
                        url = url,
                        headers = headers,
                        params = requestVariables.urlEncodedParameters,
                        expectedDataType = requestVariables.expectedResponseType)
            }

            OcpiRequestType.REMOTE -> {

                val (url, headers, body) = routingService.prepareRemotePlatformRequest(requestVariables)

                httpService.postClientMessage(url = url, headers = headers, body = body)
            }

        }

        val headers = routingService.proxyPaginationHeaders(
                responseHeaders = response.headers,
                proxyEndpoint = "/ocpi/sender/2.2/sessions/page",
                sender = sender,
                receiver = receiver)

        return ResponseEntity
                .status(response.statusCode)
                .headers(headers)
                .body(response.body)
    }


    @GetMapping("/ocpi/sender/2.2/sessions/page/{uid}")
    fun getSessionsPageFromDataOwner(@RequestHeader("authorization") authorization: String,
                                     @RequestHeader("X-Request-ID") requestID: String,
                                     @RequestHeader("X-Correlation-ID") correlationID: String,
                                     @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
                                     @RequestHeader("OCPI-from-party-id") fromPartyID: String,
                                     @RequestHeader("OCPI-to-country-code") toCountryCode: String,
                                     @RequestHeader("OCPI-to-party-id") toPartyID: String,
                                     @PathVariable uid: String): ResponseEntity<OcpiResponse<Array<Session>>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        routingService.validateSender(authorization, sender)

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.SESSIONS,
                interfaceRole = InterfaceRole.SENDER,
                method = HttpMethod.GET,
                requestID = requestID,
                correlationID = correlationID,
                sender = sender,
                receiver = receiver,
                urlPathVariables = uid,
                expectedResponseType = OcpiResponseDataType.SESSION_ARRAY)

        val response = when (routingService.validateReceiver(receiver)) {

            OcpiRequestType.LOCAL -> {

                val (url, headers) = routingService.prepareLocalPlatformRequest(requestVariables, proxied = true)

                httpService.makeRequest(
                        method = requestVariables.method,
                        url = url,
                        headers = headers,
                        expectedDataType = requestVariables.expectedResponseType)

            }

            OcpiRequestType.REMOTE -> {

                val (url, headers, body) = routingService.prepareRemotePlatformRequest(requestVariables, proxied = true)

                httpService.postClientMessage(url = url, headers = headers, body = body)

            }

        }

        var headers = HttpHeaders()

        if (isOcpiSuccess(response)) {

            routingService.deleteProxyResource(uid)

            headers = routingService.proxyPaginationHeaders(
                    responseHeaders = response.headers,
                    proxyEndpoint = "/ocpi/sender/2.2/sessions/page",
                    sender = sender,
                    receiver = receiver)

        }

        return ResponseEntity
                .status(response.statusCode)
                .headers(headers)
                .body(response.body)
    }


    @PutMapping("/ocpi/sender/2.2/sessions/{sessionID}/charging_preferences")
    fun putChargingPreferences(@RequestHeader("authorization") authorization: String,
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

        routingService.validateSender(authorization, sender)

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.SESSIONS,
                interfaceRole = InterfaceRole.SENDER,
                method = HttpMethod.PUT,
                requestID = requestID,
                correlationID = correlationID,
                sender = sender,
                receiver = receiver,
                urlPathVariables = "/$sessionID/charging_preferences",
                body = body,
                expectedResponseType = OcpiResponseDataType.CHARGING_PREFERENCE_RESPONSE)

        val response = when (routingService.validateReceiver(receiver)) {

            OcpiRequestType.LOCAL -> {

                val (url, headers) = routingService.prepareLocalPlatformRequest(requestVariables)

                httpService.makeRequest(
                        method = requestVariables.method,
                        url = url,
                        headers = headers,
                        body = body,
                        expectedDataType = requestVariables.expectedResponseType)

            }

            OcpiRequestType.REMOTE -> {

                val (url, headers, ocnBody) = routingService.prepareRemotePlatformRequest(requestVariables)

                httpService.postClientMessage(url = url, headers = headers, body = ocnBody)

            }

        }

        return ResponseEntity.status(response.statusCode).body(response.body)
    }


    /**
     * RECEIVER INTERFACE
     */

//    @GetMapping("/ocpi/receiver/2.2/sessions/{countryCode}/{partyID}/{sessionID}")
//    fun getClientOwnedSession(@RequestHeader("authorization") authorization: String,
//                              @RequestHeader("X-Request-ID") requestID: String,
//                              @RequestHeader("X-Correlation-ID") correlationID: String,
//                              @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
//                              @RequestHeader("OCPI-from-party-id") fromPartyID: String,
//                              @RequestHeader("OCPI-to-country-code") toCountryCode: String,
//                              @RequestHeader("OCPI-to-party-id") toPartyID: String,
//                              @PathVariable countryCode: String,
//                              @PathVariable partyID: String,
//                              @PathVariable sessionID: String): ResponseEntity<OcpiResponse<Session>> {
//
//        val sender = BasicRole(fromPartyID, fromCountryCode)
//        val receiver = BasicRole(toPartyID, toCountryCode)
//        val objectOwner = BasicRole(partyID, countryCode)
//
//        routingService.validateSender(authorization, sender, objectOwner)
//
//        val response = if (routingService.isRoleKnown(receiver)) {
//            val platformID = routingService.getPlatformID(receiver)
//            val endpoint = routingService.getPlatformEndpoint(platformID, "sessions", InterfaceRole.RECEIVER)
//            val headers = routingService.makeHeaders(platformID, correlationID, sender, receiver)
//            routingService.forwardRequest(
//                    method = "GET",
//                    url = urlJoin(endpoint.url, "/$countryCode/$partyID/$sessionID"),
//                    headers = headers,
//                    expectedDataType = Session::class)
//        } else {
//            val url = routingService.findBrokerUrl(receiver)
//            val headers = routingService.makeHeaders(requestID, correlationID, sender, receiver)
//            val hubRequestBody = HubGenericRequest(
//                    method = "GET",
//                    module = "sessions",
//                    body = null,
//                    path = urlJoin(url, "/$countryCode/$partyID/$sessionID"),
//                    headers = headers,
//                    role = InterfaceRole.RECEIVER,
//                    expectedResponseType = HubRequestResponseType.SESSION)
//            routingService.forwardRequest(
//                    method = "POST",
//                    url = urlJoin(url, "/ocn/message"),
//                    headers = mapOf(
//                            "X-Request-ID" to generateUUIDv4Token(),
//                            "OCN-Signature" to routingService.signRequest(hubRequestBody)),
//                    body = hubRequestBody,
//                    expectedDataType = Session::class)
//        }
//
//        return ResponseEntity.status(response.statusCode).body(response.body)
//    }
//
//    @PutMapping("/ocpi/receiver/2.2/sessions/{countryCode}/{partyID}/{sessionID}")
//    fun putClientOwnedSession(@RequestHeader("authorization") authorization: String,
//                              @RequestHeader("X-Request-ID") requestID: String,
//                              @RequestHeader("X-Correlation-ID") correlationID: String,
//                              @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
//                              @RequestHeader("OCPI-from-party-id") fromPartyID: String,
//                              @RequestHeader("OCPI-to-country-code") toCountryCode: String,
//                              @RequestHeader("OCPI-to-party-id") toPartyID: String,
//                              @PathVariable countryCode: String,
//                              @PathVariable partyID: String,
//                              @PathVariable sessionID: String,
//                              @RequestBody body: Session): ResponseEntity<OcpiResponse<Nothing>> {
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
//            val endpoint = routingService.getPlatformEndpoint(platformID, "sessions", InterfaceRole.RECEIVER)
//            val headers = routingService.makeHeaders(platformID, correlationID, sender, receiver)
//            routingService.forwardRequest(
//                    method = "PUT",
//                    url = urlJoin(endpoint.url, "/$countryCode/$partyID/$sessionID"),
//                    headers = headers,
//                    body = body,
//                    expectedDataType = Nothing::class)
//        } else {
//            val url = routingService.findBrokerUrl(receiver)
//            val headers = routingService.makeHeaders(requestID, correlationID, sender, receiver)
//            val hubRequestBody = HubGenericRequest(
//                    method = "PUT",
//                    module = "sessions",
//                    path = urlJoin(url, "/$countryCode/$partyID/$sessionID"),
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
//    @PatchMapping("/ocpi/receiver/2.2/sessions/{countryCode}/{partyID}/{sessionID}")
//    fun patchClientOwnedSession(@RequestHeader("authorization") authorization: String,
//                                @RequestHeader("X-Request-ID") requestID: String,
//                                @RequestHeader("X-Correlation-ID") correlationID: String,
//                                @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
//                                @RequestHeader("OCPI-from-party-id") fromPartyID: String,
//                                @RequestHeader("OCPI-to-country-code") toCountryCode: String,
//                                @RequestHeader("OCPI-to-party-id") toPartyID: String,
//                                @PathVariable countryCode: String,
//                                @PathVariable partyID: String,
//                                @PathVariable sessionID: String,
//                                @RequestBody body: Map<String, Any>): ResponseEntity<OcpiResponse<Nothing>> {
//
//        val sender = BasicRole(fromPartyID, fromCountryCode)
//        val receiver = BasicRole(toPartyID, toCountryCode)
//        val objectOwner = BasicRole(partyID, countryCode)
//
//        routingService.validateSender(authorization, sender, objectOwner)
//
//        val response = if (routingService.isRoleKnown(receiver)) {
//            val platformID = routingService.getPlatformID(receiver)
//            val endpoint = routingService.getPlatformEndpoint(platformID, "sessions", InterfaceRole.RECEIVER)
//            val headers = routingService.makeHeaders(platformID, correlationID, sender, receiver)
//            routingService.forwardRequest(
//                    method = "PATCH",
//                    url = urlJoin(endpoint.url, "/$countryCode/$partyID/$sessionID"),
//                    headers = headers,
//                    body = body,
//                    expectedDataType = Nothing::class)
//        } else {
//            val url = routingService.findBrokerUrl(receiver)
//            val headers = routingService.makeHeaders(requestID, correlationID, sender, receiver)
//            val hubRequestBody = HubGenericRequest(
//                    method = "PATCH",
//                    module = "sessions",
//                    path = urlJoin(url, "/$countryCode/$partyID/$sessionID"),
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

}