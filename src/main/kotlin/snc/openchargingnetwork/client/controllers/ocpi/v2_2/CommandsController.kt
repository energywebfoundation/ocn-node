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
import snc.openchargingnetwork.client.config.Properties
import snc.openchargingnetwork.client.models.*
import snc.openchargingnetwork.client.models.ocpi.*
import snc.openchargingnetwork.client.services.HttpRequestService
import snc.openchargingnetwork.client.services.RoutingService
import snc.openchargingnetwork.client.tools.urlJoin

@RestController
class CommandsController(private val routingService: RoutingService,
                         private val httpService: HttpRequestService,
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

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.Commands,
                interfaceRole = InterfaceRole.SENDER,
                method = HttpMethod.POST,
                requestID = requestID,
                correlationID = correlationID,
                sender = sender,
                receiver = receiver,
                urlPathVariables = uid,
                body = body,
                expectedResponseType = OcpiResponseDataType.NOTHING)

        val response = when (routingService.validateReceiver(receiver)) {

            OcpiRequestType.LOCAL -> {

                val (url, headers) = routingService.prepareLocalPlatformRequest(requestVariables, proxied = true)

                httpService.makeRequest(
                        method = requestVariables.method,
                        url = url,
                        headers = headers,
                        body = body,
                        expectedDataType = requestVariables.expectedResponseType)

            }

            OcpiRequestType.REMOTE -> {

                val (url, headers, ocnBody) = routingService.prepareRemotePlatformRequest(requestVariables, proxied = true)

                httpService.postClientMessage(url = url, headers = headers, body = ocnBody)

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
                module = ModuleID.Commands,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.POST,
                requestID = requestID,
                correlationID = correlationID,
                sender = sender,
                receiver = receiver,
                urlPathVariables = "CANCEL_RESERVATION",
                body = body,
                expectedResponseType = OcpiResponseDataType.COMMAND_RESPONSE)

        val response = when (routingService.validateReceiver(receiver)) {

            OcpiRequestType.LOCAL -> {

                val resourceID = routingService.setProxyResource(body.responseURL, sender, receiver)

                val proxyBody = body.copy(responseURL = urlJoin(
                        properties.url,
                        "/ocpi/sender/2.2/commands",
                        requestVariables.urlPathVariables!!,
                        resourceID.toString()))

                val (url, headers) = routingService.prepareLocalPlatformRequest(requestVariables)

                httpService.makeRequest(
                        method = requestVariables.method,
                        url = url,
                        headers = headers,
                        body = proxyBody,
                        expectedDataType = requestVariables.expectedResponseType)

            }

            OcpiRequestType.REMOTE -> {

                val (url, headers, ocnBody) = routingService.prepareRemotePlatformRequest(requestVariables)

                httpService.postClientMessage(url = url, headers = headers, body = ocnBody)

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
                module = ModuleID.Commands,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.POST,
                requestID = requestID,
                correlationID = correlationID,
                sender = sender,
                receiver = receiver,
                urlPathVariables = "RESERVE_NOW",
                body = body,
                expectedResponseType = OcpiResponseDataType.COMMAND_RESPONSE)

        val response = when (routingService.validateReceiver(receiver)) {

            OcpiRequestType.LOCAL -> {

                val resourceID = routingService.setProxyResource(body.responseURL, sender, receiver)

                val proxyBody = body.copy(responseURL = urlJoin(
                        properties.url,
                        "/ocpi/sender/2.2/commands",
                        requestVariables.urlPathVariables!!,
                        resourceID.toString()))

                val (url, headers) = routingService.prepareLocalPlatformRequest(requestVariables)

                httpService.makeRequest(
                        method = requestVariables.method,
                        url = url,
                        headers = headers,
                        body = proxyBody,
                        expectedDataType = requestVariables.expectedResponseType)

            }

            OcpiRequestType.REMOTE -> {

                val (url, headers, ocnBody) = routingService.prepareRemotePlatformRequest(requestVariables)

                httpService.postClientMessage(url = url, headers = headers, body = ocnBody)

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
                module = ModuleID.Commands,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.POST,
                requestID = requestID,
                correlationID = correlationID,
                sender = sender,
                receiver = receiver,
                urlPathVariables = "START_SESSION",
                body = body,
                expectedResponseType = OcpiResponseDataType.COMMAND_RESPONSE)

        val response = when (routingService.validateReceiver(receiver)) {

            OcpiRequestType.LOCAL -> {

                val resourceID = routingService.setProxyResource(body.responseURL, sender, receiver)

                val proxyBody = body.copy(responseURL = urlJoin(
                        properties.url,
                        "/ocpi/sender/2.2/commands",
                        requestVariables.urlPathVariables!!,
                        resourceID.toString()))

                val (url, headers) = routingService.prepareLocalPlatformRequest(requestVariables)

                httpService.makeRequest(
                        method = requestVariables.method,
                        url = url,
                        headers = headers,
                        body = proxyBody,
                        expectedDataType = requestVariables.expectedResponseType)

            }

            OcpiRequestType.REMOTE -> {

                val (url, headers, ocnBody) = routingService.prepareRemotePlatformRequest(requestVariables)

                httpService.postClientMessage(url = url, headers = headers, body = ocnBody)

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
                module = ModuleID.Commands,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.POST,
                requestID = requestID,
                correlationID = correlationID,
                sender = sender,
                receiver = receiver,
                urlPathVariables = "STOP_SESSION",
                body = body,
                expectedResponseType = OcpiResponseDataType.COMMAND_RESPONSE)

        val response = when (routingService.validateReceiver(receiver)) {

            OcpiRequestType.LOCAL -> {

                val resourceID = routingService.setProxyResource(body.responseURL, sender, receiver)

                val proxyBody = body.copy(responseURL = urlJoin(
                        properties.url,
                        "/ocpi/sender/2.2/commands",
                        requestVariables.urlPathVariables!!,
                        resourceID.toString()))

                val (url, headers) = routingService.prepareLocalPlatformRequest(requestVariables)

                httpService.makeRequest(
                        method = requestVariables.method,
                        url = url,
                        headers = headers,
                        body = proxyBody,
                        expectedDataType = requestVariables.expectedResponseType)

            }

            OcpiRequestType.REMOTE -> {

                val (url, headers, ocnBody) = routingService.prepareRemotePlatformRequest(requestVariables)

                httpService.postClientMessage(url = url, headers = headers, body = ocnBody)

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
                module = ModuleID.Commands,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.POST,
                requestID = requestID,
                correlationID = correlationID,
                sender = sender,
                receiver = receiver,
                urlPathVariables = "UNLOCK_CONNECTOR",
                body = body,
                expectedResponseType = OcpiResponseDataType.COMMAND_RESPONSE)

        val response = when (routingService.validateReceiver(receiver)) {

            OcpiRequestType.LOCAL -> {

                val resourceID = routingService.setProxyResource(body.responseURL, sender, receiver)

                val proxyBody = body.copy(responseURL = urlJoin(
                        properties.url,
                        "/ocpi/sender/2.2/commands",
                        requestVariables.urlPathVariables!!,
                        resourceID.toString()))

                val (url, headers) = routingService.prepareLocalPlatformRequest(requestVariables)

                httpService.makeRequest(
                        method = requestVariables.method,
                        url = url,
                        headers = headers,
                        body = proxyBody,
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