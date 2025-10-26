/*
    Copyright 2019-2020 eMobility GmbH

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
class LocationsController(private val requestHandlerBuilder: OcpiRequestHandlerBuilder) {


    /**
     * SENDER INTERFACES
     */

    @GetMapping("/ocpi/sender/2.2.1/locations")
    fun getLocationListFromDataOwner(
        @RequestHeader("authorization") authorization: String,
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
        @RequestParam("limit", required = false) limit: Int?
    ): ResponseEntity<OcpiResponse<Array<Location>>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        val params =
            mapOf("date_from" to dateFrom, "date_to" to dateTo, "offset" to offset, "limit" to limit).filterNull()

        val requestVariables = OcpiRequestVariables(
            module = ModuleID.LOCATIONS,
            interfaceRole = InterfaceRole.SENDER,
            method = HttpMethod.GET,
            headers = OcnHeaders(authorization, signature, requestID, correlationID, sender, receiver),
            queryParams = params
        )

        return requestHandlerBuilder
            .build<Array<Location>>(requestVariables)
            .forwardDefault()
            .getResponseWithPaginationHeaders() // proxies Link response header
    }

    @GetMapping("/ocpi/sender/2.2.1/locations/page/{uid}")
    fun getLocationPageFromDataOwner(
        @RequestHeader("authorization") authorization: String,
        @RequestHeader("OCN-Signature") signature: String? = null,
        @RequestHeader("X-Request-ID") requestID: String,
        @RequestHeader("X-Correlation-ID") correlationID: String,
        @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
        @RequestHeader("OCPI-from-party-id") fromPartyID: String,
        @RequestHeader("OCPI-to-country-code") toCountryCode: String,
        @RequestHeader("OCPI-to-party-id") toPartyID: String,
        @PathVariable uid: String
    ): ResponseEntity<OcpiResponse<Array<Location>>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        val requestVariables = OcpiRequestVariables(
            module = ModuleID.LOCATIONS,
            interfaceRole = InterfaceRole.SENDER,
            method = HttpMethod.GET,
            headers = OcnHeaders(authorization, signature, requestID, correlationID, sender, receiver),
            urlPath = uid
        )

        return requestHandlerBuilder
            .build<Array<Location>>(requestVariables)
            .forwardDefault(proxied = true) // retrieves proxied Link response header
            .getResponseWithPaginationHeaders()
    }

    @GetMapping("/ocpi/sender/2.2.1/locations/{locationID}")
    fun getLocationObjectFromDataOwner(
        @RequestHeader("authorization") authorization: String,
        @RequestHeader("X-Request-ID") requestID: String,
        @RequestHeader("OCN-Signature") signature: String? = null,
        @RequestHeader("X-Correlation-ID") correlationID: String,
        @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
        @RequestHeader("OCPI-from-party-id") fromPartyID: String,
        @RequestHeader("OCPI-to-country-code") toCountryCode: String,
        @RequestHeader("OCPI-to-party-id") toPartyID: String,
        @PathVariable locationID: String
    ): ResponseEntity<OcpiResponse<Location>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        val requestVariables = OcpiRequestVariables(
            module = ModuleID.LOCATIONS,
            interfaceRole = InterfaceRole.SENDER,
            method = HttpMethod.GET,
            headers = OcnHeaders(authorization, signature, requestID, correlationID, sender, receiver),
            urlPath = locationID
        )

        return requestHandlerBuilder
            .build<Location>(requestVariables)
            .forwardDefault()
            .getResponse()
    }

    @GetMapping("/ocpi/sender/2.2.1/locations/{locationID}/{evseUID}")
    fun getEvseObjectFromDataOwner(
        @RequestHeader("authorization") authorization: String,
        @RequestHeader("OCN-Signature") signature: String? = null,
        @RequestHeader("X-Request-ID") requestID: String,
        @RequestHeader("X-Correlation-ID") correlationID: String,
        @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
        @RequestHeader("OCPI-from-party-id") fromPartyID: String,
        @RequestHeader("OCPI-to-country-code") toCountryCode: String,
        @RequestHeader("OCPI-to-party-id") toPartyID: String,
        @PathVariable locationID: String,
        @PathVariable evseUID: String
    ): ResponseEntity<OcpiResponse<Evse>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        val requestVariables = OcpiRequestVariables(
            module = ModuleID.LOCATIONS,
            interfaceRole = InterfaceRole.SENDER,
            method = HttpMethod.GET,
            headers = OcnHeaders(authorization, signature, requestID, correlationID, sender, receiver),
            urlPath = "/$locationID/$evseUID"
        )

        return requestHandlerBuilder
            .build<Evse>(requestVariables)
            .forwardDefault()
            .getResponse()
    }

    @GetMapping("/ocpi/sender/2.2.1/locations/{locationID}/{evseUID}/{connectorID}")
    fun getConnectorObjectFromDataOwner(
        @RequestHeader("authorization") authorization: String,
        @RequestHeader("OCN-Signature") signature: String? = null,
        @RequestHeader("X-Request-ID") requestID: String,
        @RequestHeader("X-Correlation-ID") correlationID: String,
        @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
        @RequestHeader("OCPI-from-party-id") fromPartyID: String,
        @RequestHeader("OCPI-to-country-code") toCountryCode: String,
        @RequestHeader("OCPI-to-party-id") toPartyID: String,
        @PathVariable locationID: String,
        @PathVariable evseUID: String,
        @PathVariable connectorID: String
    ): ResponseEntity<OcpiResponse<Connector>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        val requestVariables = OcpiRequestVariables(
            module = ModuleID.LOCATIONS,
            interfaceRole = InterfaceRole.SENDER,
            method = HttpMethod.GET,
            headers = OcnHeaders(authorization, signature, requestID, correlationID, sender, receiver),
            urlPath = "/$locationID/$evseUID/$connectorID"
        )

        return requestHandlerBuilder
            .build<Connector>(requestVariables)
            .forwardDefault()
            .getResponse()
    }


    /**
     * RECEIVER INTERFACES
     */

    @GetMapping("/ocpi/receiver/2.2.1/locations/{countryCode}/{partyID}/{locationID}")
    fun getClientOwnedLocation(
        @RequestHeader("authorization") authorization: String,
        @RequestHeader("OCN-Signature") signature: String? = null,
        @RequestHeader("X-Request-ID") requestID: String,
        @RequestHeader("X-Correlation-ID") correlationID: String,
        @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
        @RequestHeader("OCPI-from-party-id") fromPartyID: String,
        @RequestHeader("OCPI-to-country-code") toCountryCode: String,
        @RequestHeader("OCPI-to-party-id") toPartyID: String,
        @PathVariable countryCode: String,
        @PathVariable partyID: String,
        @PathVariable locationID: String
    ): ResponseEntity<OcpiResponse<Location>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        val requestVariables = OcpiRequestVariables(
            module = ModuleID.LOCATIONS,
            interfaceRole = InterfaceRole.RECEIVER,
            method = HttpMethod.GET,
            headers = OcnHeaders(authorization, signature, requestID, correlationID, sender, receiver),
            urlPath = "/$countryCode/$partyID/$locationID"
        )

        return requestHandlerBuilder
            .build<Location>(requestVariables)
            .forwardDefault()
            .getResponse()
    }

    @GetMapping("/ocpi/receiver/2.2.1/locations/{countryCode}/{partyID}/{locationID}/{evseUID}")
    fun getClientOwnedEvse(
        @RequestHeader("authorization") authorization: String,
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
        @PathVariable evseUID: String
    ): ResponseEntity<OcpiResponse<Evse>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        val requestVariables = OcpiRequestVariables(
            module = ModuleID.LOCATIONS,
            interfaceRole = InterfaceRole.RECEIVER,
            method = HttpMethod.GET,
            headers = OcnHeaders(authorization, signature, requestID, correlationID, sender, receiver),
            urlPath = "/$countryCode/$partyID/$locationID/$evseUID"
        )

        return requestHandlerBuilder
            .build<Evse>(requestVariables)
            .forwardDefault()
            .getResponse()
    }

    @GetMapping("/ocpi/receiver/2.2.1/locations/{countryCode}/{partyID}/{locationID}/{evseUID}/{connectorID}")
    fun getClientOwnedConnector(
        @RequestHeader("authorization") authorization: String,
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
        @PathVariable connectorID: String
    ): ResponseEntity<OcpiResponse<Connector>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        val requestVariables = OcpiRequestVariables(
            module = ModuleID.LOCATIONS,
            interfaceRole = InterfaceRole.RECEIVER,
            method = HttpMethod.GET,
            headers = OcnHeaders(authorization, signature, requestID, correlationID, sender, receiver),
            urlPath = "/$countryCode/$partyID/$locationID/$evseUID/$connectorID"
        )

        return requestHandlerBuilder
            .build<Connector>(requestVariables)
            .forwardDefault()
            .getResponse()
    }

    @PutMapping("/ocpi/receiver/2.2.1/locations/{countryCode}/{partyID}/{locationID}")
    fun putClientOwnedLocation(
        @RequestHeader("authorization") authorization: String,
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
        @RequestBody body: Location
    ): ResponseEntity<OcpiResponse<Unit>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        val requestVariables = OcpiRequestVariables(
            module = ModuleID.LOCATIONS,
            interfaceRole = InterfaceRole.RECEIVER,
            method = HttpMethod.PUT,
            headers = OcnHeaders(authorization, signature, requestID, correlationID, sender, receiver),
            urlPath = "/$countryCode/$partyID/$locationID",
            body = body
        )

        return requestHandlerBuilder
            .build<Unit>(requestVariables)
            .forwardDefault()
            .getResponse()
    }

    @PutMapping("/ocpi/receiver/2.2.1/locations/{countryCode}/{partyID}/{locationID}/{evseUID}")
    fun putClientOwnedEvse(
        @RequestHeader("authorization") authorization: String,
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
        @RequestBody body: Evse
    ): ResponseEntity<OcpiResponse<Unit>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        val requestVariables = OcpiRequestVariables(
            module = ModuleID.LOCATIONS,
            interfaceRole = InterfaceRole.RECEIVER,
            method = HttpMethod.PUT,
            headers = OcnHeaders(authorization, signature, requestID, correlationID, sender, receiver),
            urlPath = "/$countryCode/$partyID/$locationID/$evseUID",
            body = body
        )

        return requestHandlerBuilder
            .build<Unit>(requestVariables)
            .forwardDefault()
            .getResponse()
    }

    @PutMapping("/ocpi/receiver/2.2.1/locations/{countryCode}/{partyID}/{locationID}/{evseUID}/{connectorID}")
    fun putClientOwnedConnector(
        @RequestHeader("authorization") authorization: String,
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
        @RequestBody body: Connector
    ): ResponseEntity<OcpiResponse<Unit>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        val requestVariables = OcpiRequestVariables(
            module = ModuleID.LOCATIONS,
            interfaceRole = InterfaceRole.RECEIVER,
            method = HttpMethod.PUT,
            headers = OcnHeaders(authorization, signature, requestID, correlationID, sender, receiver),
            urlPath = "/$countryCode/$partyID/$locationID/$evseUID/$connectorID",
            body = body
        )

        return requestHandlerBuilder
            .build<Unit>(requestVariables)
            .forwardDefault()
            .getResponse()
    }

    @PatchMapping("/ocpi/receiver/2.2.1/locations/{countryCode}/{partyID}/{locationID}")
    fun patchClientOwnedLocation(
        @RequestHeader("authorization") authorization: String,
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
        @RequestBody body: Map<String, Any>
    ): ResponseEntity<OcpiResponse<Unit>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        val requestVariables = OcpiRequestVariables(
            module = ModuleID.LOCATIONS,
            interfaceRole = InterfaceRole.RECEIVER,
            method = HttpMethod.PATCH,
            headers = OcnHeaders(authorization, signature, requestID, correlationID, sender, receiver),
            urlPath = "/$countryCode/$partyID/$locationID",
            body = body
        )

        return requestHandlerBuilder
            .build<Unit>(requestVariables)
            .forwardDefault()
            .getResponse()
    }

    @PatchMapping("/ocpi/receiver/2.2.1/locations/{countryCode}/{partyID}/{locationID}/{evseUID}")
    fun patchClientOwnedEvse(
        @RequestHeader("authorization") authorization: String,
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
        @RequestBody body: Map<String, Any>
    ): ResponseEntity<OcpiResponse<Unit>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        val requestVariables = OcpiRequestVariables(
            module = ModuleID.LOCATIONS,
            interfaceRole = InterfaceRole.RECEIVER,
            method = HttpMethod.PATCH,
            headers = OcnHeaders(authorization, signature, requestID, correlationID, sender, receiver),
            urlPath = "/$countryCode/$partyID/$locationID/$evseUID",
            body = body
        )

        return requestHandlerBuilder
            .build<Unit>(requestVariables)
            .forwardDefault()
            .getResponse()
    }

    @PatchMapping("/ocpi/receiver/2.2.1/locations/{countryCode}/{partyID}/{locationID}/{evseUID}/{connectorID}")
    fun patchClientOwnedConnector(
        @RequestHeader("authorization") authorization: String,
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
        @RequestBody body: Map<String, Any>
    ): ResponseEntity<OcpiResponse<Unit>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        val requestVariables = OcpiRequestVariables(
            module = ModuleID.LOCATIONS,
            interfaceRole = InterfaceRole.RECEIVER,
            method = HttpMethod.PATCH,
            headers = OcnHeaders(authorization, signature, requestID, correlationID, sender, receiver),
            urlPath = "/$countryCode/$partyID/$locationID/$evseUID/$connectorID",
            body = body
        )

        return requestHandlerBuilder
            .build<Unit>(requestVariables)
            .forwardDefault()
            .getResponse()
    }

}