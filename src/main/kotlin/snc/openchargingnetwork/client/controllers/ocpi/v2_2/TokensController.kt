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
import snc.openchargingnetwork.client.models.*
import snc.openchargingnetwork.client.models.ocpi.*
import snc.openchargingnetwork.client.services.HttpRequestService
import snc.openchargingnetwork.client.services.RoutingService
import snc.openchargingnetwork.client.tools.isOcpiSuccess

@RestController
class TokensController(private val routingService: RoutingService,
                       private val httpService: HttpRequestService) {


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

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.TOKENS,
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
                expectedResponseType = OcpiResponseDataType.TOKEN_ARRAY)

        val response: HttpResponse<Array<Token>> = when (routingService.validateReceiver(receiver)) {

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


    @GetMapping("/ocpi/sender/2.2/tokens/page/{uid}")
    fun getTokensPageFromDataOwner(@RequestHeader("authorization") authorization: String,
                                   @RequestHeader("X-Request-ID") requestID: String,
                                   @RequestHeader("X-Correlation-ID") correlationID: String,
                                   @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
                                   @RequestHeader("OCPI-from-party-id") fromPartyID: String,
                                   @RequestHeader("OCPI-to-country-code") toCountryCode: String,
                                   @RequestHeader("OCPI-to-party-id") toPartyID: String,
                                   @PathVariable uid: String): ResponseEntity<OcpiResponse<Array<Token>>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        routingService.validateSender(authorization, sender)

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.TOKENS,
                interfaceRole = InterfaceRole.SENDER,
                method = HttpMethod.GET,
                requestID = requestID,
                correlationID = correlationID,
                sender = sender,
                receiver = receiver,
                urlPathVariables = uid,
                expectedResponseType = OcpiResponseDataType.TOKEN_ARRAY)

        val response: HttpResponse<Array<Token>> = when (routingService.validateReceiver(receiver)) {

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


    @PostMapping("/ocpi/sender/2.2/tokens/{tokenUID}/authorize")
    fun postRealTimeTokenAuthorization(@RequestHeader("authorization") authorization: String,
                                       @RequestHeader("X-Request-ID") requestID: String,
                                       @RequestHeader("X-Correlation-ID") correlationID: String,
                                       @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
                                       @RequestHeader("OCPI-from-party-id") fromPartyID: String,
                                       @RequestHeader("OCPI-to-country-code") toCountryCode: String,
                                       @RequestHeader("OCPI-to-party-id") toPartyID: String,
                                       @PathVariable tokenUID: String,
                                       @RequestParam("type", required = false) type: TokenType? = null,
                                       @RequestBody body: LocationReferences? = null): ResponseEntity<OcpiResponse<AuthorizationInfo>> {


        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        routingService.validateSender(authorization, sender)

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.TOKENS,
                interfaceRole = InterfaceRole.SENDER,
                method = HttpMethod.POST,
                requestID = requestID,
                correlationID = correlationID,
                sender = sender,
                receiver = receiver,
                urlPathVariables = "$tokenUID/authorize",
                urlEncodedParameters = OcpiRequestParameters(type = type ?: TokenType.RFID),
                body = body,
                expectedResponseType = OcpiResponseDataType.AUTHORIZATION_INFO)

        val response: HttpResponse<AuthorizationInfo> = when (routingService.validateReceiver(receiver)) {

            OcpiRequestType.LOCAL -> {

                val (url, headers) = routingService.prepareLocalPlatformRequest(requestVariables)

                httpService.makeRequest(
                        method = requestVariables.method,
                        url = url,
                        headers = headers,
                        params = requestVariables.urlEncodedParameters,
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
                            @RequestParam("type", required = false) type: TokenType? = null): ResponseEntity<OcpiResponse<Token>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        routingService.validateSender(authorization, sender)

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.TOKENS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.GET,
                requestID = requestID,
                correlationID = correlationID,
                sender = sender,
                receiver = receiver,
                urlPathVariables = "/$countryCode/$partyID/$tokenUID",
                urlEncodedParameters = OcpiRequestParameters(type = type ?: TokenType.RFID),
                expectedResponseType = OcpiResponseDataType.TOKEN)

        val response: HttpResponse<Token> = when (routingService.validateReceiver(receiver)) {

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

                val (url, headers, ocnBody) = routingService.prepareRemotePlatformRequest(requestVariables)

                httpService.postClientMessage(url = url, headers = headers, body = ocnBody)

            }

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
                            @RequestParam("type", required = false) type: TokenType? = null,
                            @RequestBody body: Token): ResponseEntity<OcpiResponse<Nothing>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        routingService.validateSender(authorization, sender)

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.TOKENS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.PUT,
                requestID = requestID,
                correlationID = correlationID,
                sender = sender,
                receiver = receiver,
                urlPathVariables = "/$countryCode/$partyID/$tokenUID",
                urlEncodedParameters = OcpiRequestParameters(type = type ?: TokenType.RFID),
                body = body,
                expectedResponseType = OcpiResponseDataType.NOTHING)

        val response: HttpResponse<Nothing> = when (routingService.validateReceiver(receiver)) {

            OcpiRequestType.LOCAL -> {

                val (url, headers) = routingService.prepareLocalPlatformRequest(requestVariables)

                httpService.makeRequest(
                        method = requestVariables.method,
                        url = url,
                        headers = headers,
                        params = requestVariables.urlEncodedParameters,
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
                               @RequestParam("type", required = false) type: TokenType? = null,
                               @RequestBody body: Map<String, Any>): ResponseEntity<OcpiResponse<Nothing>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        routingService.validateSender(authorization, sender)

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.TOKENS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.PATCH,
                requestID = requestID,
                correlationID = correlationID,
                sender = sender,
                receiver = receiver,
                urlPathVariables = "/$countryCode/$partyID/$tokenUID",
                urlEncodedParameters = OcpiRequestParameters(type = type ?: TokenType.RFID),
                body = body,
                expectedResponseType = OcpiResponseDataType.NOTHING)

        val response: HttpResponse<Nothing> = when (routingService.validateReceiver(receiver)) {

            OcpiRequestType.LOCAL -> {

                val (url, headers) = routingService.prepareLocalPlatformRequest(requestVariables)

                httpService.makeRequest(
                        method = requestVariables.method,
                        url = url,
                        headers = headers,
                        params = requestVariables.urlEncodedParameters,
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


}