/*
    Copyright 2019 Share&Charge Foundation

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
import snc.openchargingnetwork.client.services.HttpService
import snc.openchargingnetwork.client.services.RoutingService
import snc.openchargingnetwork.client.tools.isOcpiSuccess

@RestController
class LocationsController(private val routingService: RoutingService,
                          private val httpService: HttpService) {


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
                headers = OcpiRequestHeaders(
                        requestID = requestID,
                        correlationID = correlationID,
                        sender = sender,
                        receiver = receiver),
                urlEncodedParams = OcpiRequestParameters(
                        dateFrom = dateFrom,
                        dateTo = dateTo,
                        offset = offset,
                        limit = limit))

        val response: HttpResponse<Array<Location>> = when (routingService.validateReceiver(receiver)) {

            Receiver.LOCAL -> {

                val (url, headers) = routingService.prepareLocalPlatformRequest(requestVariables)

                httpService.makeOcpiRequest(url, headers, requestVariables)
            }

            Receiver.REMOTE -> {

                val (url, headers, body) = routingService.prepareRemotePlatformRequest(requestVariables)

                httpService.postOcnMessage(url, headers, body)
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


    @GetMapping("/ocpi/sender/2.2/locations/page/{uid}")
    fun getLocationPageFromDataOwner(@RequestHeader("authorization") authorization: String,
                                     @RequestHeader("X-Request-ID") requestID: String,
                                     @RequestHeader("X-Correlation-ID") correlationID: String,
                                     @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
                                     @RequestHeader("OCPI-from-party-id") fromPartyID: String,
                                     @RequestHeader("OCPI-to-country-code") toCountryCode: String,
                                     @RequestHeader("OCPI-to-party-id") toPartyID: String,
                                     @PathVariable uid: String): ResponseEntity<OcpiResponse<Array<Location>>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        routingService.validateSender(authorization, sender)

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.LOCATIONS,
                interfaceRole = InterfaceRole.SENDER,
                method = HttpMethod.GET,
                headers = OcpiRequestHeaders(
                        requestID = requestID,
                        correlationID = correlationID,
                        sender = sender,
                        receiver = receiver),
                urlPathVariables = uid)

        val response: HttpResponse<Array<Location>> = when (routingService.validateReceiver(receiver)) {

            Receiver.LOCAL -> {

                val (url, headers) = routingService.prepareLocalPlatformRequest(requestVariables, proxied = true)

                httpService.makeOcpiRequest(url, headers, requestVariables)

            }

            Receiver.REMOTE -> {

                val (url, headers, body) = routingService.prepareRemotePlatformRequest(requestVariables, proxied = true)

                httpService.postOcnMessage(url, headers, body)

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
                headers = OcpiRequestHeaders(
                        requestID = requestID,
                        correlationID = correlationID,
                        sender = sender,
                        receiver = receiver),
                urlPathVariables = locationID)

        val response: HttpResponse<Location> = when (routingService.validateReceiver(receiver)) {

            Receiver.LOCAL -> {

                val (url, headers) = routingService.prepareLocalPlatformRequest(requestVariables)

                httpService.makeOcpiRequest(url, headers, requestVariables)
            }

            Receiver.REMOTE -> {

                val (url, headers, body) = routingService.prepareRemotePlatformRequest(requestVariables)

                httpService.postOcnMessage(url, headers, body)
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
                headers = OcpiRequestHeaders(
                        requestID = requestID,
                        correlationID = correlationID,
                        sender = sender,
                        receiver = receiver),
                urlPathVariables = "/$locationID/$evseUID")

        val response: HttpResponse<Evse> = when (routingService.validateReceiver(receiver)) {

            Receiver.LOCAL -> {

                val (url, headers) = routingService.prepareLocalPlatformRequest(requestVariables)

                httpService.makeOcpiRequest(url, headers, requestVariables)
            }

            Receiver.REMOTE -> {

                val (url, headers, body) = routingService.prepareRemotePlatformRequest(requestVariables)

                httpService.postOcnMessage(url, headers, body)
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
                headers = OcpiRequestHeaders(
                        requestID = requestID,
                        correlationID = correlationID,
                        sender = sender,
                        receiver = receiver),
                urlPathVariables = "/$locationID/$evseUID/$connectorID")

        val response: HttpResponse<Connector> = when (routingService.validateReceiver(receiver)) {

            Receiver.LOCAL -> {

                val (url, headers) = routingService.prepareLocalPlatformRequest(requestVariables)

                httpService.makeOcpiRequest(url, headers, requestVariables)
            }

            Receiver.REMOTE -> {

                val (url, headers, body) = routingService.prepareRemotePlatformRequest(requestVariables)

                httpService.postOcnMessage(url, headers, body)
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
                headers = OcpiRequestHeaders(
                        requestID = requestID,
                        correlationID = correlationID,
                        sender = sender,
                        receiver = receiver),
                urlPathVariables = "/$countryCode/$partyID/$locationID")

        val response: HttpResponse<Location> = when (routingService.validateReceiver(receiver)) {

            Receiver.LOCAL -> {

                val (url, headers) = routingService.prepareLocalPlatformRequest(requestVariables)

                httpService.makeOcpiRequest(url, headers, requestVariables)
            }

            Receiver.REMOTE -> {

                val (url, headers, body) = routingService.prepareRemotePlatformRequest(requestVariables)

                httpService.postOcnMessage(url, headers, body)
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
                headers = OcpiRequestHeaders(
                        requestID = requestID,
                        correlationID = correlationID,
                        sender = sender,
                        receiver = receiver),
                urlPathVariables = "/$countryCode/$partyID/$locationID/$evseUID")

        val response: HttpResponse<Evse> = when (routingService.validateReceiver(receiver)) {

            Receiver.LOCAL -> {

                val (url, headers) = routingService.prepareLocalPlatformRequest(requestVariables)

                httpService.makeOcpiRequest(url, headers, requestVariables)
            }

            Receiver.REMOTE -> {

                val (url, headers, body) = routingService.prepareRemotePlatformRequest(requestVariables)

                httpService.postOcnMessage(url, headers, body)
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
                headers = OcpiRequestHeaders(
                        requestID = requestID,
                        correlationID = correlationID,
                        sender = sender,
                        receiver = receiver),
                urlPathVariables = "/$countryCode/$partyID/$locationID/$evseUID/$connectorID")

        val response: HttpResponse<Connector> = when (routingService.validateReceiver(receiver)) {

            Receiver.LOCAL -> {

                val (url, headers) = routingService.prepareLocalPlatformRequest(requestVariables)

                httpService.makeOcpiRequest(url, headers, requestVariables)
            }

            Receiver.REMOTE -> {

                val (url, headers, body) = routingService.prepareRemotePlatformRequest(requestVariables)

                httpService.postOcnMessage(url, headers, body)
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
                               @RequestBody body: Location): ResponseEntity<OcpiResponse<Unit>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        routingService.validateSender(authorization, sender)

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.LOCATIONS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.PUT,
                headers = OcpiRequestHeaders(
                        requestID = requestID,
                        correlationID = correlationID,
                        sender = sender,
                        receiver = receiver),
                urlPathVariables = "/$countryCode/$partyID/$locationID",
                body = body)

        val response: HttpResponse<Unit> = when (routingService.validateReceiver(receiver)) {

            Receiver.LOCAL -> {

                val (url, headers) = routingService.prepareLocalPlatformRequest(requestVariables)

                httpService.makeOcpiRequest(url, headers, requestVariables)
            }

            Receiver.REMOTE -> {

                val (url, headers, ocnBody) = routingService.prepareRemotePlatformRequest(requestVariables)

                httpService.postOcnMessage(url, headers, ocnBody)
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
                           @RequestBody body: Evse): ResponseEntity<OcpiResponse<Unit>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        routingService.validateSender(authorization, sender)

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.LOCATIONS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.PUT,
                headers = OcpiRequestHeaders(
                        requestID = requestID,
                        correlationID = correlationID,
                        sender = sender,
                        receiver = receiver),
                urlPathVariables = "/$countryCode/$partyID/$locationID/$evseUID",
                body = body)

        val response: HttpResponse<Unit> = when (routingService.validateReceiver(receiver)) {

            Receiver.LOCAL -> {

                val (url, headers) = routingService.prepareLocalPlatformRequest(requestVariables)

                httpService.makeOcpiRequest(url, headers, requestVariables)
            }

            Receiver.REMOTE -> {

                val (url, headers, ocnBody) = routingService.prepareRemotePlatformRequest(requestVariables)

                httpService.postOcnMessage(url, headers, ocnBody)
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
                                @RequestBody body: Connector): ResponseEntity<OcpiResponse<Unit>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        routingService.validateSender(authorization, sender)

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.LOCATIONS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.PUT,
                headers = OcpiRequestHeaders(
                        requestID = requestID,
                        correlationID = correlationID,
                        sender = sender,
                        receiver = receiver),
                urlPathVariables = "/$countryCode/$partyID/$locationID/$evseUID/$connectorID",
                body = body)

        val response: HttpResponse<Unit> = when (routingService.validateReceiver(receiver)) {

            Receiver.LOCAL -> {

                val (url, headers) = routingService.prepareLocalPlatformRequest(requestVariables)

                httpService.makeOcpiRequest(url, headers, requestVariables)
            }

            Receiver.REMOTE -> {

                val (url, headers, ocnBody) = routingService.prepareRemotePlatformRequest(requestVariables)

                httpService.postOcnMessage(url, headers, ocnBody)
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
                                 @RequestBody body: Map<String, Any>): ResponseEntity<OcpiResponse<Unit>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        routingService.validateSender(authorization, sender)

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.LOCATIONS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.PATCH,
                headers = OcpiRequestHeaders(
                        requestID = requestID,
                        correlationID = correlationID,
                        sender = sender,
                        receiver = receiver),
                urlPathVariables = "/$countryCode/$partyID/$locationID",
                body = body)

        val response: HttpResponse<Unit> = when (routingService.validateReceiver(receiver)) {

            Receiver.LOCAL -> {

                val (url, headers) = routingService.prepareLocalPlatformRequest(requestVariables)

                httpService.makeOcpiRequest(url, headers, requestVariables)
            }

            Receiver.REMOTE -> {

                val (url, headers, ocnBody) = routingService.prepareRemotePlatformRequest(requestVariables)

                httpService.postOcnMessage(url, headers, ocnBody)
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
                             @RequestBody body: Map<String, Any>): ResponseEntity<OcpiResponse<Unit>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        routingService.validateSender(authorization, sender)

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.LOCATIONS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.PATCH,
                headers = OcpiRequestHeaders(
                        requestID = requestID,
                        correlationID = correlationID,
                        sender = sender,
                        receiver = receiver),
                urlPathVariables = "/$countryCode/$partyID/$locationID/$evseUID",
                body = body)

        val response: HttpResponse<Unit> = when (routingService.validateReceiver(receiver)) {

            Receiver.LOCAL -> {

                val (url, headers) = routingService.prepareLocalPlatformRequest(requestVariables)

                httpService.makeOcpiRequest(url, headers, requestVariables)
            }

            Receiver.REMOTE -> {

                val (url, headers, ocnBody) = routingService.prepareRemotePlatformRequest(requestVariables)

                httpService.postOcnMessage(url, headers, ocnBody)
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
                                  @RequestBody body: Map<String, Any>): ResponseEntity<OcpiResponse<Unit>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        routingService.validateSender(authorization, sender)

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.LOCATIONS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.PATCH,
                headers = OcpiRequestHeaders(
                        requestID = requestID,
                        correlationID = correlationID,
                        sender = sender,
                        receiver = receiver),
                urlPathVariables = "/$countryCode/$partyID/$locationID/$evseUID/$connectorID",
                body = body)

        val response: HttpResponse<Unit> = when (routingService.validateReceiver(receiver)) {

            Receiver.LOCAL -> {

                val (url, headers) = routingService.prepareLocalPlatformRequest(requestVariables)

                httpService.makeOcpiRequest(url, headers, requestVariables)
            }

            Receiver.REMOTE -> {

                val (url, headers, ocnBody) = routingService.prepareRemotePlatformRequest(requestVariables)

                httpService.postOcnMessage(url, headers, ocnBody)
            }

        }

        return ResponseEntity.status(response.statusCode).body(response.body)
    }

}