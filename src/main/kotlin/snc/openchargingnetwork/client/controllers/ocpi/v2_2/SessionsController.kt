package snc.openchargingnetwork.client.controllers.ocpi.v2_2

import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import snc.openchargingnetwork.client.models.HubRequest
import snc.openchargingnetwork.client.models.HubRequestResponseType
import snc.openchargingnetwork.client.models.ocpi.*
import snc.openchargingnetwork.client.services.RoutingService
import snc.openchargingnetwork.client.tools.urlJoin

@RestController
class SessionsController(private val routingService: RoutingService) {

    /**
     * SENDER INTERFACE
     */

    @GetMapping("/ocpi/cpo/2.2/sessions")
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

        val params = PaginatedRequest(dateFrom, dateTo, offset, limit).encode()

        val response = if (routingService.isRoleKnown(receiver)) {
            val platformID = routingService.getPlatformID(receiver)
            val endpoint = routingService.getPlatformEndpoint(platformID, "sessions", InterfaceRole.CPO)
            val headers = routingService.makeHeaders(platformID, correlationID, sender, receiver)
            routingService.forwardRequest(
                    method = "GET",
                    url = endpoint.url,
                    headers = headers,
                    params = params,
                    expectedDataType = Array<Session>::class)
        } else {
            val url = routingService.findBrokerUrl(receiver)
            val headers = routingService.makeHeaders(correlationID, sender, receiver)
            routingService.forwardRequest(
                    method = "POST",
                    url = urlJoin(url, "/ocn/messages"),
                    headers = headers,
                    body = HubRequest(
                            method = "GET",
                            module = "sessions",
                            role = InterfaceRole.CPO,
                            params = params,
                            type = HubRequestResponseType.SESSION_ARRAY),
                    expectedDataType = Array<Session>::class)
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

    @PutMapping("/ocpi/cpo/2.2/sessions/{sessionID}/charging_preferences")
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

        val response = if (routingService.isRoleKnown(receiver)) {
            val platformID = routingService.getPlatformID(receiver)
            val endpoint = routingService.getPlatformEndpoint(platformID, "sessions", InterfaceRole.CPO)
            val headers = routingService.makeHeaders(platformID, correlationID, sender, receiver)
            routingService.forwardRequest(
                    method = "PUT",
                    url = urlJoin(endpoint.url, "/$sessionID/charging_preferences"),
                    headers = headers,
                    body = body,
                    expectedDataType = ChargingPreferencesResponse::class)
        } else {
            val url = routingService.findBrokerUrl(receiver)
            val headers = routingService.makeHeaders(correlationID, sender, receiver)
            routingService.forwardRequest(
                    method = "POST",
                    url = urlJoin(url, "/ocn/message"),
                    headers = headers,
                    body = HubRequest(
                            method = "PUT",
                            module = "sessions",
                            path = "/$sessionID/charging_preferences",
                            role = InterfaceRole.CPO,
                            body = body,
                            type = HubRequestResponseType.CHARGING_PREFERENCE_RESPONSE),
                    expectedDataType = ChargingPreferencesResponse::class)
        }

        return ResponseEntity.status(response.statusCode).body(response.body)
    }

    /**
     * RECEIVER INTERFACE
     */

    @GetMapping("/ocpi/emsp/2.2/sessions/{countryCode}/{partyID}/{sessionID}")
    fun getClientOwnedSession(@RequestHeader("authorization") authorization: String,
                              @RequestHeader("X-Request-ID") requestID: String,
                              @RequestHeader("X-Correlation-ID") correlationID: String,
                              @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
                              @RequestHeader("OCPI-from-party-id") fromPartyID: String,
                              @RequestHeader("OCPI-to-country-code") toCountryCode: String,
                              @RequestHeader("OCPI-to-party-id") toPartyID: String,
                              @PathVariable countryCode: String,
                              @PathVariable partyID: String,
                              @PathVariable sessionID: String): ResponseEntity<OcpiResponse<Session>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)
        val objectOwner = BasicRole(partyID, countryCode)

        routingService.validateSender(authorization, sender, objectOwner)

        val response = if (routingService.isRoleKnown(receiver)) {
            val platformID = routingService.getPlatformID(receiver)
            val endpoint = routingService.getPlatformEndpoint(platformID, "sessions", InterfaceRole.MSP)
            val headers = routingService.makeHeaders(platformID, correlationID, sender, receiver)
            routingService.forwardRequest(
                    method = "GET",
                    url = urlJoin(endpoint.url, "/$countryCode/$partyID/$sessionID"),
                    headers = headers,
                    expectedDataType = Session::class)
        } else {
            val url = routingService.findBrokerUrl(receiver)
            val headers = routingService.makeHeaders(correlationID, sender, receiver)
            routingService.forwardRequest(
                    method = "POST",
                    url = urlJoin(url, "/ocn/message"),
                    headers = headers,
                    body = HubRequest(
                            method = "GET",
                            module = "sessions",
                            path = urlJoin(url, "/$countryCode/$partyID/$sessionID"),
                            role = InterfaceRole.MSP,
                            type = HubRequestResponseType.SESSION),
                    expectedDataType = Session::class)
        }

        return ResponseEntity.status(response.statusCode).body(response.body)
    }

    @PutMapping("/ocpi/emsp/2.2/sessions/{countryCode}/{partyID}/{sessionID}")
    fun putClientOwnedSession(@RequestHeader("authorization") authorization: String,
                              @RequestHeader("X-Request-ID") requestID: String,
                              @RequestHeader("X-Correlation-ID") correlationID: String,
                              @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
                              @RequestHeader("OCPI-from-party-id") fromPartyID: String,
                              @RequestHeader("OCPI-to-country-code") toCountryCode: String,
                              @RequestHeader("OCPI-to-party-id") toPartyID: String,
                              @PathVariable countryCode: String,
                              @PathVariable partyID: String,
                              @PathVariable sessionID: String,
                              @RequestBody body: Session): ResponseEntity<OcpiResponse<Nothing>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)
        val objectOwner = BasicRole(partyID, countryCode)
        val objectData = BasicRole(body.partyID, body.countryCode)

        routingService.validateSender(authorization, sender, objectOwner, objectData)

        val response = if (routingService.isRoleKnown(receiver)) {
            val platformID = routingService.getPlatformID(receiver)
            val endpoint = routingService.getPlatformEndpoint(platformID, "sessions", InterfaceRole.MSP)
            val headers = routingService.makeHeaders(platformID, correlationID, sender, receiver)
            routingService.forwardRequest(
                    method = "PUT",
                    url = urlJoin(endpoint.url, "/$countryCode/$partyID/$sessionID"),
                    headers = headers,
                    body = body,
                    expectedDataType = Nothing::class)
        } else {
            val url = routingService.findBrokerUrl(receiver)
            val headers = routingService.makeHeaders(correlationID, sender, receiver)
            routingService.forwardRequest(
                    method = "POST",
                    url = urlJoin(url, "/ocn/message"),
                    headers = headers,
                    body = HubRequest(
                            method = "PUT",
                            module = "sessions",
                            path = urlJoin(url, "/$countryCode/$partyID/$sessionID"),
                            role = InterfaceRole.MSP,
                            body = body),
                    expectedDataType = Nothing::class)
        }

        return ResponseEntity.status(response.statusCode).body(response.body)
    }

    @PatchMapping("/ocpi/emsp/2.2/sessions/{countryCode}/{partyID}/{sessionID}")
    fun patchClientOwnedSession(@RequestHeader("authorization") authorization: String,
                                @RequestHeader("X-Request-ID") requestID: String,
                                @RequestHeader("X-Correlation-ID") correlationID: String,
                                @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
                                @RequestHeader("OCPI-from-party-id") fromPartyID: String,
                                @RequestHeader("OCPI-to-country-code") toCountryCode: String,
                                @RequestHeader("OCPI-to-party-id") toPartyID: String,
                                @PathVariable countryCode: String,
                                @PathVariable partyID: String,
                                @PathVariable sessionID: String,
                                @RequestBody body: Map<String, Any>): ResponseEntity<OcpiResponse<Nothing>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)
        val objectOwner = BasicRole(partyID, countryCode)

        routingService.validateSender(authorization, sender, objectOwner)

        val response = if (routingService.isRoleKnown(receiver)) {
            val platformID = routingService.getPlatformID(receiver)
            val endpoint = routingService.getPlatformEndpoint(platformID, "sessions", InterfaceRole.MSP)
            val headers = routingService.makeHeaders(platformID, correlationID, sender, receiver)
            routingService.forwardRequest(
                    method = "PATCH",
                    url = urlJoin(endpoint.url, "/$countryCode/$partyID/$sessionID"),
                    headers = headers,
                    body = body,
                    expectedDataType = Nothing::class)
        } else {
            val url = routingService.findBrokerUrl(receiver)
            val headers = routingService.makeHeaders(correlationID, sender, receiver)
            routingService.forwardRequest(
                    method = "POST",
                    url = urlJoin(url, "/ocn/message"),
                    headers = headers,
                    body = HubRequest(
                            method = "PATCH",
                            module = "sessions",
                            path = urlJoin(url, "/$countryCode/$partyID/$sessionID"),
                            role = InterfaceRole.MSP,
                            body = body),
                    expectedDataType = Nothing::class)
        }

        return ResponseEntity.status(response.statusCode).body(response.body)
    }

}