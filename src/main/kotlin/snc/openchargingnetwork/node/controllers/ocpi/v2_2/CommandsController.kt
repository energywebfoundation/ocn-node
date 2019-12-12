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
import snc.openchargingnetwork.node.config.Properties
import snc.openchargingnetwork.node.models.*
import snc.openchargingnetwork.node.models.ocpi.*
import snc.openchargingnetwork.node.services.HttpService
import snc.openchargingnetwork.node.services.RoutingService
import snc.openchargingnetwork.node.tools.generateUUIDv4Token
import snc.openchargingnetwork.node.tools.urlJoin

@RestController
class CommandsController(private val routingService: RoutingService,
                         private val httpService: HttpService,
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
                          @RequestBody body: CommandResult): ResponseEntity<OcpiResponse<Unit>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        routingService.validateSender(authorization, sender)

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.COMMANDS,
                interfaceRole = InterfaceRole.SENDER,
                method = HttpMethod.POST,
                headers = OcpiRequestHeaders(
                        requestID = requestID,
                        correlationID = correlationID,
                        sender = sender,
                        receiver = receiver),
                urlPathVariables = uid,
                body = body)

        val response: HttpResponse<Unit> = when (routingService.validateReceiver(receiver)) {

            Receiver.LOCAL -> {

                val (url, headers) = routingService.prepareLocalPlatformRequest(requestVariables, proxied = true)

                httpService.makeOcpiRequest(url, headers, requestVariables)

            }

            Receiver.REMOTE -> {

                val (url, headers, ocnBody) = routingService.prepareRemotePlatformRequest(requestVariables, proxied = true)

                httpService.postOcnMessage(url, headers, ocnBody)

            }

        }

        return ResponseEntity.status(response.statusCode).body(response.body)
    }


    /**
     * RECEIVER INTERFACE
     */

//    @Transactional
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

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.COMMANDS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.POST,
                headers = OcpiRequestHeaders(
                        requestID = requestID,
                        correlationID = correlationID,
                        sender = sender,
                        receiver = receiver),
                urlPathVariables = "CANCEL_RESERVATION",
                body = body)

        val proxyPath = "/ocpi/sender/2.2/commands/${requestVariables.urlPathVariables!!}"

        val response: HttpResponse<CommandResponse> = when (routingService.validateReceiver(receiver)) {

            Receiver.LOCAL -> {

                val resourceID = routingService.setProxyResource(body.responseURL, receiver, sender)

                val proxyBody = body.copy(responseURL = urlJoin(properties.url, proxyPath, resourceID))

                val (url, headers) = routingService.prepareLocalPlatformRequest(requestVariables)

                httpService.makeOcpiRequest(url, headers, requestVariables.copy(body = proxyBody))

            }

            Receiver.REMOTE -> {

                val (url, headers, ocnBody) = routingService.prepareRemotePlatformRequest(requestVariables) {

                    val proxyUID = generateUUIDv4Token()

                    requestVariables.copy(
                            proxyUID = proxyUID,
                            proxyResource = body.responseURL,
                            body = body.copy(responseURL = urlJoin(it, proxyPath, proxyUID)))

                }

                httpService.postOcnMessage(url, headers, ocnBody)

            }

        }

        return ResponseEntity.status(response.statusCode).body(response.body)
    }


    @PostMapping("/ocpi/receiver/2.2/commands/RESERVE_NOW")
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

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.COMMANDS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.POST,
                headers = OcpiRequestHeaders(
                        requestID = requestID,
                        correlationID = correlationID,
                        sender = sender,
                        receiver = receiver),
                urlPathVariables = "RESERVE_NOW",
                body = body)

        val proxyPath = "/ocpi/sender/2.2/commands/${requestVariables.urlPathVariables!!}"

        val response: HttpResponse<CommandResponse> = when (routingService.validateReceiver(receiver)) {

            Receiver.LOCAL -> {

                val resourceID = routingService.setProxyResource(body.responseURL, receiver, sender)

                val proxyBody = body.copy(responseURL = urlJoin(properties.url, proxyPath, resourceID))

                val (url, headers) = routingService.prepareLocalPlatformRequest(requestVariables)

                httpService.makeOcpiRequest(url, headers, requestVariables.copy(body = proxyBody))

            }

            Receiver.REMOTE -> {

                val (url, headers, ocnBody) = routingService.prepareRemotePlatformRequest(requestVariables) {

                    val proxyUID = generateUUIDv4Token()

                    requestVariables.copy(
                            proxyUID = proxyUID,
                            proxyResource = body.responseURL,
                            body = body.copy(responseURL = urlJoin(it, proxyPath, proxyUID)))

                }

                httpService.postOcnMessage(url, headers, ocnBody)

            }

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

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.COMMANDS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.POST,
                headers = OcpiRequestHeaders(
                        requestID = requestID,
                        correlationID = correlationID,
                        sender = sender,
                        receiver = receiver),
                urlPathVariables = "START_SESSION",
                body = body)

        val proxyPath = "/ocpi/sender/2.2/commands/${requestVariables.urlPathVariables!!}"

        val response: HttpResponse<CommandResponse> = when (routingService.validateReceiver(receiver)) {

            Receiver.LOCAL -> {

                val resourceID = routingService.setProxyResource(body.responseURL, receiver, sender)

                val proxyBody = body.copy(responseURL = urlJoin(properties.url, proxyPath, resourceID))

                val (url, headers) = routingService.prepareLocalPlatformRequest(requestVariables)

                httpService.makeOcpiRequest(url, headers, requestVariables.copy(body = proxyBody))

            }

            Receiver.REMOTE -> {

                val (url, headers, ocnBody) = routingService.prepareRemotePlatformRequest(requestVariables) {

                    val proxyUID = generateUUIDv4Token()

                    requestVariables.copy(
                            proxyUID = proxyUID,
                            proxyResource = body.responseURL,
                            body = body.copy(responseURL = urlJoin(it, proxyPath, proxyUID)))

                }

                httpService.postOcnMessage(url, headers, ocnBody)

            }

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

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.COMMANDS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.POST,
                headers = OcpiRequestHeaders(
                        requestID = requestID,
                        correlationID = correlationID,
                        sender = sender,
                        receiver = receiver),
                urlPathVariables = "STOP_SESSION",
                body = body)

        val proxyPath = "/ocpi/sender/2.2/commands/${requestVariables.urlPathVariables!!}"

        val response: HttpResponse<CommandResponse> = when (routingService.validateReceiver(receiver)) {

            Receiver.LOCAL -> {

                val resourceID = routingService.setProxyResource(body.responseURL, receiver, sender)

                val proxyBody = body.copy(responseURL = urlJoin(properties.url, proxyPath, resourceID))

                val (url, headers) = routingService.prepareLocalPlatformRequest(requestVariables)

                httpService.makeOcpiRequest(url, headers, requestVariables.copy(body = proxyBody))

            }

            Receiver.REMOTE -> {

                val (url, headers, ocnBody) = routingService.prepareRemotePlatformRequest(requestVariables) {

                    val proxyUID = generateUUIDv4Token()

                    requestVariables.copy(
                            proxyUID = proxyUID,
                            proxyResource = body.responseURL,
                            body = body.copy(responseURL = urlJoin(it, proxyPath, proxyUID)))

                }

                httpService.postOcnMessage(url, headers, ocnBody)

            }

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

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.COMMANDS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.POST,
                headers = OcpiRequestHeaders(
                        requestID = requestID,
                        correlationID = correlationID,
                        sender = sender,
                        receiver = receiver),
                urlPathVariables = "UNLOCK_CONNECTOR",
                body = body)

        val proxyPath = "/ocpi/sender/2.2/commands/${requestVariables.urlPathVariables!!}"

        val response: HttpResponse<CommandResponse> = when (routingService.validateReceiver(receiver)) {

            Receiver.LOCAL -> {

                val resourceID = routingService.setProxyResource(body.responseURL, receiver, sender)

                val proxyBody = body.copy(responseURL = urlJoin(properties.url, proxyPath, resourceID))

                val (url, headers) = routingService.prepareLocalPlatformRequest(requestVariables)

                httpService.makeOcpiRequest(url, headers, requestVariables.copy(body = proxyBody))

            }

            Receiver.REMOTE -> {

                val (url, headers, ocnBody) = routingService.prepareRemotePlatformRequest(requestVariables) {

                    val proxyUID = generateUUIDv4Token()

                    requestVariables.copy(
                            proxyUID = proxyUID,
                            proxyResource = body.responseURL,
                            body = body.copy(responseURL = urlJoin(it, proxyPath, proxyUID)))

                }

                httpService.postOcnMessage(url, headers, ocnBody)

            }

        }

        return ResponseEntity.status(response.statusCode).body(response.body)
    }

}