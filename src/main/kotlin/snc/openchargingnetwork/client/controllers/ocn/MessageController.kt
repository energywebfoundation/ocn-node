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

package snc.openchargingnetwork.client.controllers.ocn

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import snc.openchargingnetwork.client.config.Properties
import snc.openchargingnetwork.client.models.HubCommandsRequest
import snc.openchargingnetwork.client.models.HubGenericRequest
import snc.openchargingnetwork.client.models.HubRequestResponseType
import snc.openchargingnetwork.client.models.exceptions.OcpiClientInvalidParametersException
import snc.openchargingnetwork.client.models.exceptions.OcpiHubUnknownReceiverException
import snc.openchargingnetwork.client.models.ocpi.*
import snc.openchargingnetwork.client.services.RoutingService
import snc.openchargingnetwork.client.tools.urlJoin

@RestController
@RequestMapping("/ocn/message")
class MessageController(val routingService: RoutingService,
                        val properties: Properties) {

    @PostMapping
    fun postMessage(@RequestHeader("X-Request-ID") requestID: String,
                    @RequestHeader("X-Correlation-ID") correlationID: String,
                    @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
                    @RequestHeader("OCPI-from-party-id") fromPartyID: String,
                    @RequestHeader("OCPI-to-country-code") toCountryCode: String,
                    @RequestHeader("OCPI-to-party-id") toPartyID: String,
                    @RequestBody body: HubGenericRequest<Any>): ResponseEntity<OcpiResponse<out Any>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        // check sender has been registered on network
        val response = if (routingService.isRoleKnownOnNetwork(sender)) {

            // check receiver known to client
            if (routingService.isRoleKnown(receiver)) {

                // forward message
                val platformID = routingService.getPlatformID(receiver)
                val endpoint = routingService.getPlatformEndpoint(platformID, body.module, body.role)
                val headers = routingService.makeHeaders(platformID, correlationID, sender, receiver)

                routingService.forwardRequest(
                        method = body.method,
                        url = if (body.path != null) { urlJoin(endpoint.url, body.path) } else { endpoint.url },
                        headers = headers,
                        params = body.params,
                        body = body.body,
                        expectedDataType = when (body.expectedResponseType) {
                            HubRequestResponseType.LOCATION -> Location::class
                            HubRequestResponseType.LOCATION_ARRAY -> Array<Location>::class
                            HubRequestResponseType.EVSE -> Evse::class
                            HubRequestResponseType.CONNECTOR -> Connector::class
                            HubRequestResponseType.SESSION -> Session::class
                            HubRequestResponseType.SESSION_ARRAY -> Array<Session>::class
                            HubRequestResponseType.CHARGING_PREFERENCE_RESPONSE -> ChargingPreferencesResponse::class
                            HubRequestResponseType.CDR -> CDR::class
                            HubRequestResponseType.CDR_ARRAY -> Array<CDR>::class
                            HubRequestResponseType.TARIFF -> Tariff::class
                            HubRequestResponseType.TARIFF_ARRAY -> Array<Tariff>::class
                            HubRequestResponseType.TOKEN -> Token::class
                            HubRequestResponseType.TOKEN_ARRAY -> Array<Token>::class
                            HubRequestResponseType.AUTHORIZATION_INFO -> AuthorizationInfo::class
                            HubRequestResponseType.NOTHING -> Nothing::class
                            HubRequestResponseType.COMMAND_RESPONSE -> CommandResponse::class
                        })

            } else {
                throw OcpiHubUnknownReceiverException()
            }

        } else {
            throw OcpiHubUnknownReceiverException("Sending party not registered on Open Charging Network")
        }

        val headers = HttpHeaders()
        response.headers["location"]?.let { headers.add("Location", it) }
        response.headers["Link"]?.let { headers.add("Link", it) }
        response.headers["X-Total-Count"]?.let { headers.add("X-Total-Count", it) }
        response.headers["X-Limit"]?.let { headers.add("X-Limit", it) }

        return ResponseEntity.status(response.statusCode).headers(headers).body(response.body)
    }

    @PostMapping("/command")
    fun postCommand(@RequestHeader("X-Request-ID") requestID: String,
                    @RequestHeader("X-Correlation-ID") correlationID: String,
                    @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
                    @RequestHeader("OCPI-from-party-id") fromPartyID: String,
                    @RequestHeader("OCPI-to-country-code") toCountryCode: String,
                    @RequestHeader("OCPI-to-party-id") toPartyID: String,
                    @RequestBody requestBody: HubCommandsRequest): ResponseEntity<OcpiResponse<CommandResponse>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        // check sender has been registered on network
        val response = if (routingService.isRoleKnownOnNetwork(sender)) {

            // check receiver known to client
            if (routingService.isRoleKnown(receiver)) {

                // forward message
                val platformID = routingService.getPlatformID(receiver)
                val endpoint = routingService.getPlatformEndpoint(platformID, "commands", InterfaceRole.SENDER)
                val headers = routingService.makeHeaders(platformID, correlationID, sender, receiver)

                val commandBody: MutableMap<String, Any> = jacksonObjectMapper().readValue(requestBody.body)
                val originalResponseURL = commandBody["response_url"] ?: throw OcpiClientInvalidParametersException("No response_url found")
                val uid = routingService.saveResponseURL(originalResponseURL.toString(), requestBody.type, sender, receiver)
                commandBody["response_url"] = urlJoin(properties.url, "/ocpi/emsp/2.2/commands/${requestBody.type}/$uid")

                routingService.forwardRequest(
                        method = "POST",
                        url = urlJoin(endpoint.url, "/${requestBody.type}"),
                        headers = headers,
                        body = commandBody,
                        expectedDataType = CommandResponse::class)

            } else {
                throw OcpiHubUnknownReceiverException()
            }

        } else {
            throw OcpiHubUnknownReceiverException("Sending party not registered on Open Charging Network")
        }

        return ResponseEntity.status(response.statusCode).body(response.body)
    }

}