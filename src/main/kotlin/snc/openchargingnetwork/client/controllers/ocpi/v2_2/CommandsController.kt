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

import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import snc.openchargingnetwork.client.config.Properties
import snc.openchargingnetwork.client.models.HubCommandsRequest
import snc.openchargingnetwork.client.models.HubGenericRequest
import snc.openchargingnetwork.client.models.ocpi.*
import snc.openchargingnetwork.client.services.RoutingService
import snc.openchargingnetwork.client.tools.urlJoin

@RestController
class CommandsController(private val routingService: RoutingService,
                         private val properties: Properties) {

    /**
     * SENDER INTERFACE
     */

    @PostMapping("/ocpi/sender/2.2/commands/{command}/{uid}")
    fun postAsyncResponse(@RequestHeader("authorization") authorization: String,
                          @RequestHeader("X-Request-ID") requestID: String,
                          @RequestHeader("X-Correlation-ID") correlationID: String,
                          @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
                          @RequestHeader("OCPI-from-party-id") fromPartyID: String,
                          @RequestHeader("OCPI-to-country-code") toCountryCode: String,
                          @RequestHeader("OCPI-to-party-id") toPartyID: String,
                          @PathVariable("command") command: CommandType,
                          @PathVariable("uid") uid: String,
                          @RequestBody body: CommandResult): ResponseEntity<OcpiResponse<Nothing>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        routingService.validateSender(authorization, sender)

        val response = if (routingService.isRoleKnown(receiver)) {
            val platformID = routingService.getPlatformID(receiver)
            val headers = routingService.makeHeaders(platformID, correlationID, sender, receiver)
            val responseURL = routingService.findResponseURL(command, uid, sender, receiver)
            routingService.forwardRequest(
                    method = "POST",
                    url = responseURL,
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
                    body = HubGenericRequest(
                            method = "POST",
                            module = "commands",
                            path = "/$command",
                            body = body,
                            role = InterfaceRole.SENDER),
                    expectedDataType = Nothing::class)
        }

        return ResponseEntity.status(response.statusCode).body(response.body)

    }

    /**
     * RECEIVER INTERFACE
     */

    @Transactional
    @PostMapping("/ocpi/receiver/2.2/commands/CANCEL_RESERVATION")
    fun postCancelReservation(@RequestHeader("authorization") authorization: String,
                              @RequestHeader("X-Request-ID") requestID: String,
                              @RequestHeader("X-Correlation-ID") correlationID: String,
                              @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
                              @RequestHeader("OCPI-from-party-id") fromPartyID: String,
                              @RequestHeader("OCPI-to-country-code") toCountryCode: String,
                              @RequestHeader("OCPI-to-party-id") toPartyID: String,
                              @RequestBody body: CancelReservation): ResponseEntity<OcpiResponse<CommandResponse>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        routingService.validateSender(authorization, sender)

        val response = if (routingService.isRoleKnown(receiver)) {
            val platformID = routingService.getPlatformID(receiver)
            val endpoint = routingService.getPlatformEndpoint(platformID, "commands", InterfaceRole.RECEIVER)
            val headers = routingService.makeHeaders(platformID, correlationID, sender, receiver)

            // intercept response_url and replace with broker-readable URL (async post mapping above)
            val uid = routingService.saveResponseURL(body.responseURL, CommandType.CANCEL_RESERVATION, sender, receiver)
            body.responseURL = urlJoin(properties.url, "/ocpi/sender/2.2/commands/CANCEL_RESERVATION/$uid")

            routingService.forwardRequest(
                    method = "POST",
                    url = urlJoin(endpoint.url, "/CANCEL_RESERVATION"),
                    headers = headers,
                    body = body,
                    expectedDataType = CommandResponse::class)
        } else {
            val url = routingService.findBrokerUrl(receiver)
            val headers = routingService.makeHeaders(correlationID, sender, receiver)
            routingService.forwardRequest(
                    method = "POST",
                    url = urlJoin(url, "/ocn/message/command"),
                    headers = headers,
                    body = HubCommandsRequest(
                            type = CommandType.CANCEL_RESERVATION,
                            body = routingService.stringify(body)),
                    expectedDataType = CommandResponse::class)
        }

        return ResponseEntity.status(response.statusCode).body(response.body)

    }

    @PostMapping("/ocpi/receiver/2.2/comamnds/RESERVE_NOW")
    fun postReserveNow(@RequestHeader("authorization") authorization: String,
                       @RequestHeader("X-Request-ID") requestID: String,
                       @RequestHeader("X-Correlation-ID") correlationID: String,
                       @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
                       @RequestHeader("OCPI-from-party-id") fromPartyID: String,
                       @RequestHeader("OCPI-to-country-code") toCountryCode: String,
                       @RequestHeader("OCPI-to-party-id") toPartyID: String,
                       @RequestBody body: ReserveNow): ResponseEntity<OcpiResponse<CommandResponse>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        routingService.validateSender(authorization, sender)

        val response = if (routingService.isRoleKnown(receiver)) {
            val platformID = routingService.getPlatformID(receiver)
            val endpoint = routingService.getPlatformEndpoint(platformID, "commands", InterfaceRole.RECEIVER)
            val headers = routingService.makeHeaders(platformID, correlationID, sender, receiver)

            // intercept response_url and replace with broker-readable URL (async post mapping above)
            val uid = routingService.saveResponseURL(body.responseURL, CommandType.RESERVE_NOW, sender, receiver)
            body.responseURL = urlJoin(properties.url, "/ocpi/sender/2.2/commands/RESERVE_NOW/$uid")

            routingService.forwardRequest(
                    method = "POST",
                    url = urlJoin(endpoint.url, "/RESERVE_NOW"),
                    headers = headers,
                    body = body,
                    expectedDataType = CommandResponse::class)
        } else {
            val url = routingService.findBrokerUrl(receiver)
            val headers = routingService.makeHeaders(correlationID, sender, receiver)
            routingService.forwardRequest(
                    method = "POST",
                    url = urlJoin(url, "/ocn/message"),
                    headers = headers,
                    body = HubCommandsRequest(
                            type = CommandType.RESERVE_NOW,
                            body = routingService.stringify(body)),
                    expectedDataType = CommandResponse::class)
        }

        return ResponseEntity.status(response.statusCode).body(response.body)


    }

    @PostMapping("/ocpi/receiver/2.2/commands/START_SESSION")
    fun postStartSession(@RequestHeader("authorization") authorization: String,
                         @RequestHeader("X-Request-ID") requestID: String,
                         @RequestHeader("X-Correlation-ID") correlationID: String,
                         @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
                         @RequestHeader("OCPI-from-party-id") fromPartyID: String,
                         @RequestHeader("OCPI-to-country-code") toCountryCode: String,
                         @RequestHeader("OCPI-to-party-id") toPartyID: String,
                         @RequestBody body: StartSession): ResponseEntity<OcpiResponse<CommandResponse>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        routingService.validateSender(authorization, sender)

        val response = if (routingService.isRoleKnown(receiver)) {
            val platformID = routingService.getPlatformID(receiver)
            val endpoint = routingService.getPlatformEndpoint(platformID, "commands", InterfaceRole.RECEIVER)
            val headers = routingService.makeHeaders(platformID, correlationID, sender, receiver)

            // intercept response_url and replace with broker-readable URL (async post mapping above)
            val uid = routingService.saveResponseURL(body.responseURL, CommandType.START_SESSION, sender, receiver)
            body.responseURL = urlJoin(properties.url, "/ocpi/sender/2.2/commands/START_SESSION/$uid")

            routingService.forwardRequest(
                    method = "POST",
                    url = urlJoin(endpoint.url, "/START_SESSION"),
                    headers = headers,
                    body = body,
                    expectedDataType = CommandResponse::class)
        } else {
            val url = routingService.findBrokerUrl(receiver)
            val headers = routingService.makeHeaders(correlationID, sender, receiver)
            routingService.forwardRequest(
                    method = "POST",
                    url = urlJoin(url, "/ocn/message"),
                    headers = headers,
                    body = HubCommandsRequest(
                            type = CommandType.START_SESSION,
                            body = routingService.stringify(body)),
                    expectedDataType = CommandResponse::class)
        }

        return ResponseEntity.status(response.statusCode).body(response.body)


    }

    @PostMapping("/ocpi/receiver/2.2/commands/STOP_SESSION")
    fun postStopSession(@RequestHeader("authorization") authorization: String,
                        @RequestHeader("X-Request-ID") requestID: String,
                        @RequestHeader("X-Correlation-ID") correlationID: String,
                        @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
                        @RequestHeader("OCPI-from-party-id") fromPartyID: String,
                        @RequestHeader("OCPI-to-country-code") toCountryCode: String,
                        @RequestHeader("OCPI-to-party-id") toPartyID: String,
                        @RequestBody body: StopSession): ResponseEntity<OcpiResponse<CommandResponse>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        routingService.validateSender(authorization, sender)

        val response = if (routingService.isRoleKnown(receiver)) {
            val platformID = routingService.getPlatformID(receiver)
            val endpoint = routingService.getPlatformEndpoint(platformID, "commands", InterfaceRole.RECEIVER)
            val headers = routingService.makeHeaders(platformID, correlationID, sender, receiver)

            // intercept response_url and replace with broker-readable URL (async post mapping above)
            val uid = routingService.saveResponseURL(body.responseURL, CommandType.STOP_SESSION, sender, receiver)
            body.responseURL = urlJoin(properties.url, "/ocpi/sender/2.2/commands/STOP_SESSION/$uid")

            routingService.forwardRequest(
                    method = "POST",
                    url = urlJoin(endpoint.url, "/STOP_SESSION"),
                    headers = headers,
                    body = body,
                    expectedDataType = CommandResponse::class)
        } else {
            val url = routingService.findBrokerUrl(receiver)
            val headers = routingService.makeHeaders(correlationID, sender, receiver)
            routingService.forwardRequest(
                    method = "POST",
                    url = urlJoin(url, "/ocn/message"),
                    headers = headers,
                    body = HubCommandsRequest(
                            type = CommandType.STOP_SESSION,
                            body = routingService.stringify(body)),
                    expectedDataType = CommandResponse::class)
        }

        return ResponseEntity.status(response.statusCode).body(response.body)


    }

    @PostMapping("/ocpi/receiver/2.2/commands/UNLOCK_CONNECTOR")
    fun postUnlockConnector(@RequestHeader("authorization") authorization: String,
                            @RequestHeader("X-Request-ID") requestID: String,
                            @RequestHeader("X-Correlation-ID") correlationID: String,
                            @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
                            @RequestHeader("OCPI-from-party-id") fromPartyID: String,
                            @RequestHeader("OCPI-to-country-code") toCountryCode: String,
                            @RequestHeader("OCPI-to-party-id") toPartyID: String,
                            @RequestBody body: UnlockConnector): ResponseEntity<OcpiResponse<CommandResponse>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        routingService.validateSender(authorization, sender)

        val response = if (routingService.isRoleKnown(receiver)) {
            val platformID = routingService.getPlatformID(receiver)
            val endpoint = routingService.getPlatformEndpoint(platformID, "commands", InterfaceRole.RECEIVER)
            val headers = routingService.makeHeaders(platformID, correlationID, sender, receiver)

            // intercept response_url and replace with broker-readable URL (async post mapping above)
            val uid = routingService.saveResponseURL(body.responseURL, CommandType.UNLOCK_CONNECTOR, sender, receiver)
            body.responseURL = urlJoin(properties.url, "/ocpi/sender/2.2/commands/UNLOCK_CONNECTOR/$uid")

            routingService.forwardRequest(
                    method = "POST",
                    url = urlJoin(endpoint.url, "/UNLOCK_CONNECTOR"),
                    headers = headers,
                    body = body,
                    expectedDataType = CommandResponse::class)
        } else {
            val url = routingService.findBrokerUrl(receiver)
            val headers = routingService.makeHeaders(correlationID, sender, receiver)
            routingService.forwardRequest(
                    method = "POST",
                    url = urlJoin(url, "/ocn/message"),
                    headers = headers,
                    body = HubCommandsRequest(
                            type = CommandType.UNLOCK_CONNECTOR,
                            body = routingService.stringify(body)),
                    expectedDataType = CommandResponse::class)
        }

        return ResponseEntity.status(response.statusCode).body(response.body)


    }

}