/*
    Copyright 2019 Share&Charge Foundation

    This file is part of Open Charging Network Node.

    Open Charging Network Node is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Open Charging Network Node is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Open Charging Network Node.  If not, see <https://www.gnu.org/licenses/>.
*/

package snc.openchargingnetwork.node.controllers.ocpi.v2_2

import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import snc.openchargingnetwork.node.models.*
import snc.openchargingnetwork.node.models.ocpi.*
import snc.openchargingnetwork.node.services.RequestHandler
import snc.openchargingnetwork.node.services.RequestHandlerBuilder


@RestController
class LocationsController(private val requestHandlerBuilder: RequestHandlerBuilder) {


    /**
     * SENDER INTERFACES
     */

    @GetMapping("/ocpi/sender/2.2/locations")
    fun getLocationListFromDataOwner(@RequestHeader("authorization") authorization: String,
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
                                     @RequestParam("limit", required = false) limit: Int?): ResponseEntity<OcpiResponse<Array<Location>>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.LOCATIONS,
                interfaceRole = InterfaceRole.SENDER,
                method = HttpMethod.GET,
                headers = OcnHeaders(authorization, signature, requestID, correlationID, sender, receiver),
                urlEncodedParams = OcpiRequestParameters(
                        dateFrom = dateFrom,
                        dateTo = dateTo,
                        offset = offset,
                        limit = limit))

        val request: RequestHandler<Array<Location>> = requestHandlerBuilder.build(requestVariables)
        return request
                .validateSender()
                .forwardRequest()
                .getResponseWithPaginationHeaders()
    }

    @GetMapping("/ocpi/sender/2.2/locations/page/{uid}")
    fun getLocationPageFromDataOwner(@RequestHeader("authorization") authorization: String,
                                     @RequestHeader("OCN-Signature") signature: String? = null,
                                     @RequestHeader("X-Request-ID") requestID: String,
                                     @RequestHeader("X-Correlation-ID") correlationID: String,
                                     @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
                                     @RequestHeader("OCPI-from-party-id") fromPartyID: String,
                                     @RequestHeader("OCPI-to-country-code") toCountryCode: String,
                                     @RequestHeader("OCPI-to-party-id") toPartyID: String,
                                     @PathVariable uid: String): ResponseEntity<OcpiResponse<Array<Location>>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.LOCATIONS,
                interfaceRole = InterfaceRole.SENDER,
                method = HttpMethod.GET,
                headers = OcnHeaders(authorization, signature, requestID, correlationID, sender, receiver),
                urlPathVariables = uid)

        val request: RequestHandler<Array<Location>> = requestHandlerBuilder.build(requestVariables)
        return request
                .validateSender()
                .forwardRequest(proxied = true)
                .getResponseWithPaginationHeaders()
    }

    @GetMapping("/ocpi/sender/2.2/locations/{locationID}")
    fun getLocationObjectFromDataOwner(@RequestHeader("authorization") authorization: String,
                                       @RequestHeader("X-Request-ID") requestID: String,
                                       @RequestHeader("OCN-Signature") signature: String? = null,
                                       @RequestHeader("X-Correlation-ID") correlationID: String,
                                       @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
                                       @RequestHeader("OCPI-from-party-id") fromPartyID: String,
                                       @RequestHeader("OCPI-to-country-code") toCountryCode: String,
                                       @RequestHeader("OCPI-to-party-id") toPartyID: String,
                                       @PathVariable locationID: String): ResponseEntity<OcpiResponse<Location>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.LOCATIONS,
                interfaceRole = InterfaceRole.SENDER,
                method = HttpMethod.GET,
                headers = OcnHeaders(authorization, signature, requestID, correlationID, sender, receiver),
                urlPathVariables = locationID)

        val request: RequestHandler<Location> = requestHandlerBuilder.build(requestVariables)
        return request
                .validateSender()
                .forwardRequest()
                .getResponse()
    }

    @GetMapping("/ocpi/sender/2.2/locations/{locationID}/{evseUID}")
    fun getEvseObjectFromDataOwner(@RequestHeader("authorization") authorization: String,
                                   @RequestHeader("OCN-Signature") signature: String? = null,
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

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.LOCATIONS,
                interfaceRole = InterfaceRole.SENDER,
                method = HttpMethod.GET,
                headers = OcnHeaders(authorization, signature, requestID, correlationID, sender, receiver),
                urlPathVariables = "/$locationID/$evseUID")

        val request: RequestHandler<Evse> = requestHandlerBuilder.build(requestVariables)
        return request
                .validateSender()
                .forwardRequest()
                .getResponse()
    }

    @GetMapping("/ocpi/sender/2.2/locations/{locationID}/{evseUID}/{connectorID}")
    fun getConnectorObjectFromDataOwner(@RequestHeader("authorization") authorization: String,
                                        @RequestHeader("OCN-Signature") signature: String? = null,
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

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.LOCATIONS,
                interfaceRole = InterfaceRole.SENDER,
                method = HttpMethod.GET,
                headers = OcnHeaders(authorization, signature, requestID, correlationID, sender, receiver),
                urlPathVariables = "/$locationID/$evseUID/$connectorID")

        val request: RequestHandler<Connector> = requestHandlerBuilder.build(requestVariables)
        return request
                .validateSender()
                .forwardRequest()
                .getResponse()
    }


    /**
     * RECEIVER INTERFACES
     */

    @GetMapping("/ocpi/receiver/2.2/locations/{countryCode}/{partyID}/{locationID}")
    fun getClientOwnedLocation(@RequestHeader("authorization") authorization: String,
                               @RequestHeader("OCN-Signature") signature: String? = null,
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

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.LOCATIONS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.GET,
                headers = OcnHeaders(authorization, signature, requestID, correlationID, sender, receiver),
                urlPathVariables = "/$countryCode/$partyID/$locationID")

        val request: RequestHandler<Location> = requestHandlerBuilder.build(requestVariables)
        return request
                .validateSender()
                .forwardRequest()
                .getResponse()
    }

    @GetMapping("/ocpi/receiver/2.2/locations/{countryCode}/{partyID}/{locationID}/{evseUID}")
    fun getClientOwnedEvse(@RequestHeader("authorization") authorization: String,
                           @RequestHeader("OCN-Signature") signature: String? = null,
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

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.LOCATIONS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.GET,
                headers = OcnHeaders(authorization, signature, requestID, correlationID, sender, receiver),
                urlPathVariables = "/$countryCode/$partyID/$locationID/$evseUID")

        val request: RequestHandler<Evse> = requestHandlerBuilder.build(requestVariables)
        return request
                .validateSender()
                .forwardRequest()
                .getResponse()
    }

    @GetMapping("/ocpi/receiver/2.2/locations/{countryCode}/{partyID}/{locationID}/{evseUID}/{connectorID}")
    fun getClientOwnedConnector(@RequestHeader("authorization") authorization: String,
                                @RequestHeader("OCN-Signature") signature: String? = null,
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

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.LOCATIONS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.GET,
                headers = OcnHeaders(authorization, signature, requestID, correlationID, sender, receiver),
                urlPathVariables = "/$countryCode/$partyID/$locationID/$evseUID/$connectorID")

        val request: RequestHandler<Connector> = requestHandlerBuilder.build(requestVariables)
        return request
                .validateSender()
                .forwardRequest()
                .getResponse()
    }

    @PutMapping("/ocpi/receiver/2.2/locations/{countryCode}/{partyID}/{locationID}")
    fun putClientOwnedLocation(@RequestHeader("authorization") authorization: String,
                               @RequestHeader("OCN-Signature") signature: String? = null,
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

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.LOCATIONS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.PUT,
                headers = OcnHeaders(authorization, signature, requestID, correlationID, sender, receiver),
                urlPathVariables = "/$countryCode/$partyID/$locationID",
                body = body)

        val request: RequestHandler<Unit> = requestHandlerBuilder.build(requestVariables)
        return request
                .validateSender()
                .forwardRequest()
                .getResponse()
    }

    @PutMapping("/ocpi/receiver/2.2/locations/{countryCode}/{partyID}/{locationID}/{evseUID}")
    fun putClientOwnedEvse(@RequestHeader("authorization") authorization: String,
                           @RequestHeader("OCN-Signature") signature: String? = null,
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

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.LOCATIONS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.PUT,
                headers = OcnHeaders(authorization, signature, requestID, correlationID, sender, receiver),
                urlPathVariables = "/$countryCode/$partyID/$locationID/$evseUID",
                body = body)

        val request: RequestHandler<Unit> = requestHandlerBuilder.build(requestVariables)
        return request
                .validateSender()
                .forwardRequest()
                .getResponse()
    }

    @PutMapping("/ocpi/receiver/2.2/locations/{countryCode}/{partyID}/{locationID}/{evseUID}/{connectorID}")
    fun putClientOwnedConnector(@RequestHeader("authorization") authorization: String,
                                @RequestHeader("OCN-Signature") signature: String? = null,
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

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.LOCATIONS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.PUT,
                headers = OcnHeaders(authorization, signature, requestID, correlationID, sender, receiver),
                urlPathVariables = "/$countryCode/$partyID/$locationID/$evseUID/$connectorID",
                body = body)

        val request: RequestHandler<Unit> = requestHandlerBuilder.build(requestVariables)
        return request
                .validateSender()
                .forwardRequest()
                .getResponse()
    }

    @PatchMapping("/ocpi/receiver/2.2/locations/{countryCode}/{partyID}/{locationID}")
    fun patchClientOwnedLocation(@RequestHeader("authorization") authorization: String,
                                 @RequestHeader("OCN-Signature") signature: String? = null,
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

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.LOCATIONS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.PATCH,
                headers = OcnHeaders(authorization, signature, requestID, correlationID, sender, receiver),
                urlPathVariables = "/$countryCode/$partyID/$locationID",
                body = body)

        val request: RequestHandler<Unit> = requestHandlerBuilder.build(requestVariables)
        return request
                .validateSender()
                .forwardRequest()
                .getResponse()
    }

    @PatchMapping("/ocpi/receiver/2.2/locations/{countryCode}/{partyID}/{locationID}/{evseUID}")
    fun patchClientOwnedEvse(@RequestHeader("authorization") authorization: String,
                             @RequestHeader("OCN-Signature") signature: String? = null,
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

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.LOCATIONS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.PATCH,
                headers = OcnHeaders(authorization, signature, requestID, correlationID, sender, receiver),
                urlPathVariables = "/$countryCode/$partyID/$locationID/$evseUID",
                body = body)

        val request: RequestHandler<Unit> = requestHandlerBuilder.build(requestVariables)
        return request
                .validateSender()
                .forwardRequest()
                .getResponse()
    }

    @PatchMapping("/ocpi/receiver/2.2/locations/{countryCode}/{partyID}/{locationID}/{evseUID}/{connectorID}")
    fun patchClientOwnedConnector(@RequestHeader("authorization") authorization: String,
                                  @RequestHeader("OCN-Signature") signature: String? = null,
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

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.LOCATIONS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.PATCH,
                headers = OcnHeaders(authorization, signature, requestID, correlationID, sender, receiver),
                urlPathVariables = "/$countryCode/$partyID/$locationID/$evseUID/$connectorID",
                body = body)

        val request: RequestHandler<Unit> = requestHandlerBuilder.build(requestVariables)
        return request
                .validateSender()
                .forwardRequest()
                .getResponse()
    }

}