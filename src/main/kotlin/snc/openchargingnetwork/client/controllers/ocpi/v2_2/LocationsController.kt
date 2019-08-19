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

@RestController
class LocationsController(private val routingService: RoutingService,
                          private val httpService: HttpRequestService) {


    /**
     * SENDER INTERFACES
     */

    @GetMapping("/ocpi/sender/2.2/locations")
    fun getLocationListFromDataOwner(@RequestHeader("authorization") authorization: String,
                                     @RequestHeader("X-Request-ID") requestID: String,
                                     @RequestHeader("X-Correlation-ID") correlationID: String,
                                     @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
                                     @RequestHeader("OCPI-from-party-id") fromPartyID: String,
                                     @RequestHeader("OCPI-to-country-code") toCountryCode: String,
                                     @RequestHeader("OCPI-to-party-id") toPartyID: String,
                                     @RequestParam("date_from", required = false) dateFrom: String?,
                                     @RequestParam("date_to", required = false) dateTo: String?,
                                     @RequestParam("offset", required = false) offset: Int?,
                                     @RequestParam("limit", required = false) limit: Int?): ResponseEntity<OcpiResponse<Array<Location>>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        routingService.validateSender(authorization, sender)

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.LOCATIONS,
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
                expectedResponseType = OcpiResponseDataType.LOCATION_ARRAY)

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
                proxyEndpoint = "/ocpi/sender/2.2/locations",
                sender = sender,
                receiver = receiver)

        return ResponseEntity
                .status(response.statusCode)
                .headers(headers)
                .body(response.body)
    }


    @GetMapping("/ocpi/sender/2.2/locations/{locationID}")
    fun getLocationObjectFromDataOwner(@RequestHeader("authorization") authorization: String,
                                       @RequestHeader("X-Request-ID") requestID: String,
                                       @RequestHeader("X-Correlation-ID") correlationID: String,
                                       @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
                                       @RequestHeader("OCPI-from-party-id") fromPartyID: String,
                                       @RequestHeader("OCPI-to-country-code") toCountryCode: String,
                                       @RequestHeader("OCPI-to-party-id") toPartyID: String,
                                       @PathVariable locationID: String): ResponseEntity<OcpiResponse<Location>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        routingService.validateSender(authorization, sender)

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.LOCATIONS,
                interfaceRole = InterfaceRole.SENDER,
                method = HttpMethod.GET,
                requestID = requestID,
                correlationID = correlationID,
                sender = sender,
                receiver = receiver,
                urlPathVariables = locationID,
                expectedResponseType = OcpiResponseDataType.LOCATION)

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

                val (url, headers, body) = routingService.prepareRemotePlatformRequest(requestVariables)

                httpService.postClientMessage(url = url, headers = headers, body = body)
            }

        }

        return ResponseEntity.status(response.statusCode).body(response.body)
    }


    @GetMapping("/ocpi/sender/2.2/locations/{locationID}/{evseUID}")
    fun getEvseObjectFromDataOwner(@RequestHeader("authorization") authorization: String,
                                   @RequestHeader("X-Request-ID") requestID: String,
                                   @RequestHeader("X-Correlation-ID") correlationID: String,
                                   @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
                                   @RequestHeader("OCPI-from-party-id") fromPartyID: String,
                                   @RequestHeader("OCPI-to-country-code") toCountryCode: String,
                                   @RequestHeader("OCPI-to-party-id") toPartyID: String,
                                   @PathVariable locationID: String,
                                   @PathVariable evseUID: String): ResponseEntity<OcpiResponse<Evse>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        routingService.validateSender(authorization, sender)

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.LOCATIONS,
                interfaceRole = InterfaceRole.SENDER,
                method = HttpMethod.GET,
                requestID = requestID,
                correlationID = correlationID,
                sender = sender,
                receiver = receiver,
                urlPathVariables = "/$locationID/$evseUID",
                expectedResponseType = OcpiResponseDataType.EVSE)

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

                val (url, headers, body) = routingService.prepareRemotePlatformRequest(requestVariables)

                httpService.postClientMessage(url = url, headers = headers, body = body)
            }

        }

        return ResponseEntity.status(response.statusCode).body(response.body)
    }


    @GetMapping("/ocpi/sender/2.2/locations/{locationID}/{evseUID}/{connectorID}")
    fun getConnectorObjectFromDataOwner(@RequestHeader("authorization") authorization: String,
                                        @RequestHeader("X-Request-ID") requestID: String,
                                        @RequestHeader("X-Correlation-ID") correlationID: String,
                                        @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
                                        @RequestHeader("OCPI-from-party-id") fromPartyID: String,
                                        @RequestHeader("OCPI-to-country-code") toCountryCode: String,
                                        @RequestHeader("OCPI-to-party-id") toPartyID: String,
                                        @PathVariable locationID: String,
                                        @PathVariable evseUID: String,
                                        @PathVariable connectorID: String): ResponseEntity<OcpiResponse<Connector>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        routingService.validateSender(authorization, sender)

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.LOCATIONS,
                interfaceRole = InterfaceRole.SENDER,
                method = HttpMethod.GET,
                requestID = requestID,
                correlationID = correlationID,
                sender = sender,
                receiver = receiver,
                urlPathVariables = "/$locationID/$evseUID/$connectorID",
                expectedResponseType = OcpiResponseDataType.CONNECTOR)

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

                val (url, headers, body) = routingService.prepareRemotePlatformRequest(requestVariables)

                httpService.postClientMessage(url = url, headers = headers, body = body)
            }

        }

        return ResponseEntity.status(response.statusCode).body(response.body)
    }


    /**
     * RECEIVER INTERFACES
     */

    @GetMapping("/ocpi/receiver/2.2/locations/{countryCode}/{partyID}/{locationID}")
    fun getClientOwnedLocation(@RequestHeader("authorization") authorization: String,
                               @RequestHeader("X-Request-ID") requestID: String,
                               @RequestHeader("X-Correlation-ID") correlationID: String,
                               @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
                               @RequestHeader("OCPI-from-party-id") fromPartyID: String,
                               @RequestHeader("OCPI-to-country-code") toCountryCode: String,
                               @RequestHeader("OCPI-to-party-id") toPartyID: String,
                               @PathVariable countryCode: String,
                               @PathVariable partyID: String,
                               @PathVariable locationID: String): ResponseEntity<OcpiResponse<Location>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        routingService.validateSender(authorization, sender)

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.LOCATIONS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.GET,
                requestID = requestID,
                correlationID = correlationID,
                sender = sender,
                receiver = receiver,
                urlPathVariables = "/$countryCode/$partyID/$locationID",
                expectedResponseType = OcpiResponseDataType.LOCATION)

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

                val (url, headers, body) = routingService.prepareRemotePlatformRequest(requestVariables)

                httpService.postClientMessage(url = url, headers = headers, body = body)
            }

        }

        return ResponseEntity.status(response.statusCode).body(response.body)
    }


    @GetMapping("/ocpi/receiver/2.2/locations/{countryCode}/{partyID}/{locationID}/{evseUID}")
    fun getClientOwnedEvse(@RequestHeader("authorization") authorization: String,
                           @RequestHeader("X-Request-ID") requestID: String,
                           @RequestHeader("X-Correlation-ID") correlationID: String,
                           @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
                           @RequestHeader("OCPI-from-party-id") fromPartyID: String,
                           @RequestHeader("OCPI-to-country-code") toCountryCode: String,
                           @RequestHeader("OCPI-to-party-id") toPartyID: String,
                           @PathVariable countryCode: String,
                           @PathVariable partyID: String,
                           @PathVariable locationID: String,
                           @PathVariable evseUID: String): ResponseEntity<OcpiResponse<Evse>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        routingService.validateSender(authorization, sender)

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.LOCATIONS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.GET,
                requestID = requestID,
                correlationID = correlationID,
                sender = sender,
                receiver = receiver,
                urlPathVariables = "/$countryCode/$partyID/$locationID/$evseUID",
                expectedResponseType = OcpiResponseDataType.EVSE)

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

                val (url, headers, body) = routingService.prepareRemotePlatformRequest(requestVariables)

                httpService.postClientMessage(url = url, headers = headers, body = body)
            }

        }

        return ResponseEntity.status(response.statusCode).body(response.body)
    }


    @GetMapping("/ocpi/receiver/2.2/locations/{countryCode}/{partyID}/{locationID}/{evseUID}/{connectorID}")
    fun getClientOwnedConnector(@RequestHeader("authorization") authorization: String,
                                @RequestHeader("X-Request-ID") requestID: String,
                                @RequestHeader("X-Correlation-ID") correlationID: String,
                                @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
                                @RequestHeader("OCPI-from-party-id") fromPartyID: String,
                                @RequestHeader("OCPI-to-country-code") toCountryCode: String,
                                @RequestHeader("OCPI-to-party-id") toPartyID: String,
                                @PathVariable countryCode: String,
                                @PathVariable partyID: String,
                                @PathVariable locationID: String,
                                @PathVariable evseUID: String,
                                @PathVariable connectorID: String): ResponseEntity<OcpiResponse<Connector>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        routingService.validateSender(authorization, sender)

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.LOCATIONS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.GET,
                requestID = requestID,
                correlationID = correlationID,
                sender = sender,
                receiver = receiver,
                urlPathVariables = "/$countryCode/$partyID/$locationID/$evseUID/$connectorID",
                expectedResponseType = OcpiResponseDataType.CONNECTOR)

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

                val (url, headers, body) = routingService.prepareRemotePlatformRequest(requestVariables)

                httpService.postClientMessage(url = url, headers = headers, body = body)
            }

        }

        return ResponseEntity.status(response.statusCode).body(response.body)
    }


    @PutMapping("/ocpi/receiver/2.2/locations/{countryCode}/{partyID}/{locationID}")
    fun putClientOwnedLocation(@RequestHeader("authorization") authorization: String,
                               @RequestHeader("X-Request-ID") requestID: String,
                               @RequestHeader("X-Correlation-ID") correlationID: String,
                               @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
                               @RequestHeader("OCPI-from-party-id") fromPartyID: String,
                               @RequestHeader("OCPI-to-country-code") toCountryCode: String,
                               @RequestHeader("OCPI-to-party-id") toPartyID: String,
                               @PathVariable countryCode: String,
                               @PathVariable partyID: String,
                               @PathVariable locationID: String,
                               @RequestBody body: Location): ResponseEntity<OcpiResponse<Nothing>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        routingService.validateSender(authorization, sender)

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.LOCATIONS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.PUT,
                requestID = requestID,
                correlationID = correlationID,
                sender = sender,
                receiver = receiver,
                urlPathVariables = "/$countryCode/$partyID/$locationID",
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
    }


    @PutMapping("/ocpi/receiver/2.2/locations/{countryCode}/{partyID}/{locationID}/{evseUID}")
    fun putClientOwnedEvse(@RequestHeader("authorization") authorization: String,
                           @RequestHeader("X-Request-ID") requestID: String,
                           @RequestHeader("X-Correlation-ID") correlationID: String,
                           @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
                           @RequestHeader("OCPI-from-party-id") fromPartyID: String,
                           @RequestHeader("OCPI-to-country-code") toCountryCode: String,
                           @RequestHeader("OCPI-to-party-id") toPartyID: String,
                           @PathVariable countryCode: String,
                           @PathVariable partyID: String,
                           @PathVariable locationID: String,
                           @PathVariable evseUID: String,
                           @RequestBody body: Evse): ResponseEntity<OcpiResponse<Nothing>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        routingService.validateSender(authorization, sender)

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.LOCATIONS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.PUT,
                requestID = requestID,
                correlationID = correlationID,
                sender = sender,
                receiver = receiver,
                urlPathVariables = "/$countryCode/$partyID/$locationID/$evseUID",
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
    }


    @PutMapping("/ocpi/receiver/2.2/locations/{countryCode}/{partyID}/{locationID}/{evseUID}/{connectorID}")
    fun putClientOwnedConnector(@RequestHeader("authorization") authorization: String,
                                @RequestHeader("X-Request-ID") requestID: String,
                                @RequestHeader("X-Correlation-ID") correlationID: String,
                                @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
                                @RequestHeader("OCPI-from-party-id") fromPartyID: String,
                                @RequestHeader("OCPI-to-country-code") toCountryCode: String,
                                @RequestHeader("OCPI-to-party-id") toPartyID: String,
                                @PathVariable countryCode: String,
                                @PathVariable partyID: String,
                                @PathVariable locationID: String,
                                @PathVariable evseUID: String,
                                @PathVariable connectorID: String,
                                @RequestBody body: Connector): ResponseEntity<OcpiResponse<Nothing>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        routingService.validateSender(authorization, sender)

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.LOCATIONS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.PUT,
                requestID = requestID,
                correlationID = correlationID,
                sender = sender,
                receiver = receiver,
                urlPathVariables = "/$countryCode/$partyID/$locationID/$evseUID/$connectorID",
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
    }


    @PatchMapping("/ocpi/receiver/2.2/locations/{countryCode}/{partyID}/{locationID}")
    fun patchClientOwnedLocation(@RequestHeader("authorization") authorization: String,
                                 @RequestHeader("X-Request-ID") requestID: String,
                                 @RequestHeader("X-Correlation-ID") correlationID: String,
                                 @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
                                 @RequestHeader("OCPI-from-party-id") fromPartyID: String,
                                 @RequestHeader("OCPI-to-country-code") toCountryCode: String,
                                 @RequestHeader("OCPI-to-party-id") toPartyID: String,
                                 @PathVariable countryCode: String,
                                 @PathVariable partyID: String,
                                 @PathVariable locationID: String,
                                 @RequestBody body: Map<String, Any>): ResponseEntity<OcpiResponse<Nothing>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        routingService.validateSender(authorization, sender)

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.LOCATIONS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.PATCH,
                requestID = requestID,
                correlationID = correlationID,
                sender = sender,
                receiver = receiver,
                urlPathVariables = "/$countryCode/$partyID/$locationID",
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
    }


    @PatchMapping("/ocpi/receiver/2.2/locations/{countryCode}/{partyID}/{locationID}/{evseUID}")
    fun patchClientOwnedEvse(@RequestHeader("authorization") authorization: String,
                             @RequestHeader("X-Request-ID") requestID: String,
                             @RequestHeader("X-Correlation-ID") correlationID: String,
                             @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
                             @RequestHeader("OCPI-from-party-id") fromPartyID: String,
                             @RequestHeader("OCPI-to-country-code") toCountryCode: String,
                             @RequestHeader("OCPI-to-party-id") toPartyID: String,
                             @PathVariable countryCode: String,
                             @PathVariable partyID: String,
                             @PathVariable locationID: String,
                             @PathVariable evseUID: String,
                             @RequestBody body: Map<String, Any>): ResponseEntity<OcpiResponse<Nothing>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        routingService.validateSender(authorization, sender)

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.LOCATIONS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.PATCH,
                requestID = requestID,
                correlationID = correlationID,
                sender = sender,
                receiver = receiver,
                urlPathVariables = "/$countryCode/$partyID/$locationID/$evseUID",
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
    }


    @PatchMapping("/ocpi/receiver/2.2/locations/{countryCode}/{partyID}/{locationID}/{evseUID}/{connectorID}")
    fun patchClientOwnedConnector(@RequestHeader("authorization") authorization: String,
                                  @RequestHeader("X-Request-ID") requestID: String,
                                  @RequestHeader("X-Correlation-ID") correlationID: String,
                                  @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
                                  @RequestHeader("OCPI-from-party-id") fromPartyID: String,
                                  @RequestHeader("OCPI-to-country-code") toCountryCode: String,
                                  @RequestHeader("OCPI-to-party-id") toPartyID: String,
                                  @PathVariable countryCode: String,
                                  @PathVariable partyID: String,
                                  @PathVariable locationID: String,
                                  @PathVariable evseUID: String,
                                  @PathVariable connectorID: String,
                                  @RequestBody body: Map<String, Any>): ResponseEntity<OcpiResponse<Nothing>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        routingService.validateSender(authorization, sender)

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.LOCATIONS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.PATCH,
                requestID = requestID,
                correlationID = correlationID,
                sender = sender,
                receiver = receiver,
                urlPathVariables = "/$countryCode/$partyID/$locationID/$evseUID/$connectorID",
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
    }

}