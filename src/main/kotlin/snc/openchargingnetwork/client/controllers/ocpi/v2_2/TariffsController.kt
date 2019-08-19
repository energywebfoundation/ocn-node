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
class TariffsController(private val routingService: RoutingService,
                        private val httpService: HttpRequestService) {


    /**
     * SENDER INTERFACE
     */

    @GetMapping("/ocpi/sender/2.2/tariffs")
    fun getTariffsFromDataOwner(@RequestHeader("authorization") authorization: String,
                                @RequestHeader("X-Request-ID") requestID: String,
                                @RequestHeader("X-Correlation-ID") correlationID: String,
                                @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
                                @RequestHeader("OCPI-from-party-id") fromPartyID: String,
                                @RequestHeader("OCPI-to-country-code") toCountryCode: String,
                                @RequestHeader("OCPI-to-party-id") toPartyID: String,
                                @RequestParam("date_from", required = false) dateFrom: String?,
                                @RequestParam("date_to", required = false) dateTo: String?,
                                @RequestParam("offset", required = false) offset: Int?,
                                @RequestParam("limit", required = false) limit: Int?): ResponseEntity<OcpiResponse<Array<Tariff>>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        routingService.validateSender(authorization, sender)

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.TARIFFS,
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
                expectedResponseType = OcpiResponseDataType.TARIFF_ARRAY)

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
                request = requestVariables,
                responseHeaders = response.headers)

        return ResponseEntity
                .status(response.statusCode)
                .headers(headers)
                .body(response.body)
    }


    @GetMapping("/ocpi/sender/2.2/tariffs/page/{uid}")
    fun getTariffsPageFromDataOwner(@RequestHeader("authorization") authorization: String,
                                    @RequestHeader("X-Request-ID") requestID: String,
                                    @RequestHeader("X-Correlation-ID") correlationID: String,
                                    @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
                                    @RequestHeader("OCPI-from-party-id") fromPartyID: String,
                                    @RequestHeader("OCPI-to-country-code") toCountryCode: String,
                                    @RequestHeader("OCPI-to-party-id") toPartyID: String,
                                    @PathVariable uid: String): ResponseEntity<OcpiResponse<Array<Tariff>>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        routingService.validateSender(authorization, sender)

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.TARIFFS,
                interfaceRole = InterfaceRole.SENDER,
                method = HttpMethod.GET,
                requestID = requestID,
                correlationID = correlationID,
                sender = sender,
                receiver = receiver,
                urlPathVariables = uid,
                expectedResponseType = OcpiResponseDataType.TARIFF_ARRAY)

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
                    request = requestVariables,
                    responseHeaders = response.headers)

        }

        return ResponseEntity
                .status(response.statusCode)
                .headers(headers)
                .body(response.body)
    }


    /**
     * RECEIVER INTERFACE
     */

    @GetMapping("/ocpi/receiver/2.2/tariffs/{countryCode}/{partyID}/{tariffID}")
    fun getClientOwnedTariff(@RequestHeader("authorization") authorization: String,
                             @RequestHeader("X-Request-ID") requestID: String,
                             @RequestHeader("X-Correlation-ID") correlationID: String,
                             @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
                             @RequestHeader("OCPI-from-party-id") fromPartyID: String,
                             @RequestHeader("OCPI-to-country-code") toCountryCode: String,
                             @RequestHeader("OCPI-to-party-id") toPartyID: String,
                             @PathVariable countryCode: String,
                             @PathVariable partyID: String,
                             @PathVariable tariffID: String): ResponseEntity<OcpiResponse<Tariff>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        routingService.validateSender(authorization, sender)

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.TARIFFS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.GET,
                requestID = requestID,
                correlationID = correlationID,
                sender = sender,
                receiver = receiver,
                urlPathVariables = "/$countryCode/$partyID/$tariffID",
                expectedResponseType = OcpiResponseDataType.TARIFF)

        val response = when (routingService.validateReceiver(receiver)) {

            OcpiRequestType.LOCAL -> {

                val (url, headers) = routingService.prepareLocalPlatformRequest(requestVariables)

                httpService.makeRequest(
                        method = requestVariables.method,
                        url = url,
                        headers = headers,
                        expectedDataType = requestVariables.expectedResponseType)

            }

            OcpiRequestType.REMOTE -> {

                val (url, headers, ocnBody) = routingService.prepareRemotePlatformRequest(requestVariables)

                httpService.postClientMessage(url = url, headers = headers, body = ocnBody)

            }

        }

        return ResponseEntity.status(response.statusCode).body(response.body)


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
    }


    @PutMapping("/ocpi/receiver/2.2/tariffs/{countryCode}/{partyID}/{tariffID}")
    fun putClientOwnedTariff(@RequestHeader("authorization") authorization: String,
                             @RequestHeader("X-Request-ID") requestID: String,
                             @RequestHeader("X-Correlation-ID") correlationID: String,
                             @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
                             @RequestHeader("OCPI-from-party-id") fromPartyID: String,
                             @RequestHeader("OCPI-to-country-code") toCountryCode: String,
                             @RequestHeader("OCPI-to-party-id") toPartyID: String,
                             @PathVariable countryCode: String,
                             @PathVariable partyID: String,
                             @PathVariable tariffID: String,
                             @RequestBody body: Tariff): ResponseEntity<OcpiResponse<Nothing>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        routingService.validateSender(authorization, sender)

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.TARIFFS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.PUT,
                requestID = requestID,
                correlationID = correlationID,
                sender = sender,
                receiver = receiver,
                urlPathVariables = "/$countryCode/$partyID/$tariffID",
                body = body,
                expectedResponseType = OcpiResponseDataType.NOTHING)

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
    }


    @DeleteMapping("/ocpi/receiver/2.2/tariffs/{countryCode}/{partyID}/{tariffID}")
    fun deleteClientOwnedTariff(@RequestHeader("authorization") authorization: String,
                                @RequestHeader("X-Request-ID") requestID: String,
                                @RequestHeader("X-Correlation-ID") correlationID: String,
                                @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
                                @RequestHeader("OCPI-from-party-id") fromPartyID: String,
                                @RequestHeader("OCPI-to-country-code") toCountryCode: String,
                                @RequestHeader("OCPI-to-party-id") toPartyID: String,
                                @PathVariable countryCode: String,
                                @PathVariable partyID: String,
                                @PathVariable tariffID: String): ResponseEntity<OcpiResponse<Nothing>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        routingService.validateSender(authorization, sender)

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.TARIFFS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.DELETE,
                requestID = requestID,
                correlationID = correlationID,
                sender = sender,
                receiver = receiver,
                urlPathVariables = "/$countryCode/$partyID/$tariffID",
                expectedResponseType = OcpiResponseDataType.NOTHING)

        val response = when (routingService.validateReceiver(receiver)) {

            OcpiRequestType.LOCAL -> {

                val (url, headers) = routingService.prepareLocalPlatformRequest(requestVariables)

                httpService.makeRequest(
                        method = requestVariables.method,
                        url = url,
                        headers = headers,
                        expectedDataType = requestVariables.expectedResponseType)

            }

            OcpiRequestType.REMOTE -> {

                val (url, headers, ocnBody) = routingService.prepareRemotePlatformRequest(requestVariables)

                httpService.postClientMessage(url = url, headers = headers, body = ocnBody)

            }

        }

        return ResponseEntity.status(response.statusCode).body(response.body)

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
    }

}