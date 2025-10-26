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

@RequestMapping("\${ocn.node.apiPrefix}")
@RestController
class CommandsController(private val requestHandlerBuilder: OcpiRequestHandlerBuilder) {


    /**
     * SENDER INTERFACE
     */

    @PostMapping("/ocpi/sender/2.2.1/commands/{command}/{uid}")
    fun postAsyncResponse(
        @RequestHeader("authorization") authorization: String,
        @RequestHeader("OCN-Signature") signature: String? = null,
        @RequestHeader("X-Request-ID") requestID: String,
        @RequestHeader("X-Correlation-ID") correlationID: String,
        @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
        @RequestHeader("OCPI-from-party-id") fromPartyID: String,
        @RequestHeader("OCPI-to-country-code") toCountryCode: String,
        @RequestHeader("OCPI-to-party-id") toPartyID: String,
        @PathVariable("command") command: CommandType,
        @PathVariable("uid") uid: String,
        @RequestBody body: CommandResult
    ): ResponseEntity<OcpiResponse<Unit>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        val requestVariables = OcpiRequestVariables(
            module = ModuleID.COMMANDS,
            interfaceRole = InterfaceRole.SENDER,
            method = HttpMethod.POST,
            headers = OcnHeaders(authorization, signature, requestID, correlationID, sender, receiver),
            urlPath = uid,
            body = body
        )

        return requestHandlerBuilder
            .build<Unit>(requestVariables)
            .forwardDefault(proxied = true) // retrieves proxied response_url
            .getResponse()
    }


    /**
     * RECEIVER INTERFACE
     */

//    @Transactional
    @PostMapping("/ocpi/receiver/2.2.1/commands/CANCEL_RESERVATION")
    fun postCancelReservation(
        @RequestHeader("authorization") authorization: String,
        @RequestHeader("OCN-Signature") signature: String? = null,
        @RequestHeader("X-Request-ID") requestID: String,
        @RequestHeader("X-Correlation-ID") correlationID: String,
        @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
        @RequestHeader("OCPI-from-party-id") fromPartyID: String,
        @RequestHeader("OCPI-to-country-code") toCountryCode: String,
        @RequestHeader("OCPI-to-party-id") toPartyID: String,
        @RequestBody body: CancelReservation
    ): ResponseEntity<OcpiResponse<CommandResponse>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        val requestVariables = OcpiRequestVariables(
            module = ModuleID.COMMANDS,
            interfaceRole = InterfaceRole.RECEIVER,
            method = HttpMethod.POST,
            headers = OcnHeaders(authorization, signature, requestID, correlationID, sender, receiver),
            urlPath = "CANCEL_RESERVATION",
            body = body
        )


        return requestHandlerBuilder
            .build<CommandResponse>(requestVariables)
            .forwardHaasAsync()
            .forwardAsync(body.responseURL) {
                requestVariables.copy(body = body.copy(responseURL = it))
            }
            .getResponse()
    }


    @PostMapping("/ocpi/receiver/2.2.1/commands/RESERVE_NOW")
    fun postReserveNow(
        @RequestHeader("authorization") authorization: String,
        @RequestHeader("OCN-Signature") signature: String? = null,
        @RequestHeader("X-Request-ID") requestID: String,
        @RequestHeader("X-Correlation-ID") correlationID: String,
        @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
        @RequestHeader("OCPI-from-party-id") fromPartyID: String,
        @RequestHeader("OCPI-to-country-code") toCountryCode: String,
        @RequestHeader("OCPI-to-party-id") toPartyID: String,
        @RequestBody body: ReserveNow
    ): ResponseEntity<OcpiResponse<CommandResponse>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        val requestVariables = OcpiRequestVariables(
            module = ModuleID.COMMANDS,
            interfaceRole = InterfaceRole.RECEIVER,
            method = HttpMethod.POST,
            headers = OcnHeaders(authorization, signature, requestID, correlationID, sender, receiver),
            urlPath = "RESERVE_NOW",
            body = body
        )

        return requestHandlerBuilder
            .build<CommandResponse>(requestVariables)
            .forwardHaasAsync()
            .forwardAsync(body.responseURL) {
                requestVariables.copy(body = body.copy(responseURL = it))
            }
            .getResponse()
    }


    @PostMapping("/ocpi/receiver/2.2.1/commands/START_SESSION")
    fun postStartSession(
        @RequestHeader("authorization") authorization: String,
        @RequestHeader("OCN-Signature") signature: String? = null,
        @RequestHeader("X-Request-ID") requestID: String,
        @RequestHeader("X-Correlation-ID") correlationID: String,
        @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
        @RequestHeader("OCPI-from-party-id") fromPartyID: String,
        @RequestHeader("OCPI-to-country-code") toCountryCode: String,
        @RequestHeader("OCPI-to-party-id") toPartyID: String,
        @RequestBody body: StartSession
    ): ResponseEntity<OcpiResponse<CommandResponse>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        val requestVariables = OcpiRequestVariables(
            module = ModuleID.COMMANDS,
            interfaceRole = InterfaceRole.RECEIVER,
            method = HttpMethod.POST,
            headers = OcnHeaders(authorization, signature, requestID, correlationID, sender, receiver),
            urlPath = "START_SESSION",
            body = body
        )

        return requestHandlerBuilder
            .build<CommandResponse>(requestVariables)
            .forwardHaasAsync()
            .forwardAsync(body.responseURL) {
                requestVariables.copy(body = body.copy(responseURL = it))
            }
            .getResponse()
    }


    @PostMapping("/ocpi/receiver/2.2.1/commands/STOP_SESSION")
    fun postStopSession(
        @RequestHeader("authorization") authorization: String,
        @RequestHeader("OCN-Signature") signature: String? = null,
        @RequestHeader("X-Request-ID") requestID: String,
        @RequestHeader("X-Correlation-ID") correlationID: String,
        @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
        @RequestHeader("OCPI-from-party-id") fromPartyID: String,
        @RequestHeader("OCPI-to-country-code") toCountryCode: String,
        @RequestHeader("OCPI-to-party-id") toPartyID: String,
        @RequestBody body: StopSession
    ): ResponseEntity<OcpiResponse<CommandResponse>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        val requestVariables = OcpiRequestVariables(
            module = ModuleID.COMMANDS,
            interfaceRole = InterfaceRole.RECEIVER,
            method = HttpMethod.POST,
            headers = OcnHeaders(authorization, signature, requestID, correlationID, sender, receiver),
            urlPath = "STOP_SESSION",
            body = body
        )

        return requestHandlerBuilder
            .build<CommandResponse>(requestVariables)
            .forwardHaasAsync()
            .forwardAsync(body.responseURL) {
                requestVariables.copy(body = body.copy(responseURL = it))
            }
            .getResponse()
    }


    @PostMapping("/ocpi/receiver/2.2.1/commands/UNLOCK_CONNECTOR")
    fun postUnlockConnector(
        @RequestHeader("authorization") authorization: String,
        @RequestHeader("OCN-Signature") signature: String? = null,
        @RequestHeader("X-Request-ID") requestID: String,
        @RequestHeader("X-Correlation-ID") correlationID: String,
        @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
        @RequestHeader("OCPI-from-party-id") fromPartyID: String,
        @RequestHeader("OCPI-to-country-code") toCountryCode: String,
        @RequestHeader("OCPI-to-party-id") toPartyID: String,
        @RequestBody body: UnlockConnector
    ): ResponseEntity<OcpiResponse<CommandResponse>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        val requestVariables = OcpiRequestVariables(
            module = ModuleID.COMMANDS,
            interfaceRole = InterfaceRole.RECEIVER,
            method = HttpMethod.POST,
            headers = OcnHeaders(authorization, signature, requestID, correlationID, sender, receiver),
            urlPath = "UNLOCK_CONNECTOR",
            body = body
        )

        return requestHandlerBuilder
            .build<CommandResponse>(requestVariables)
            .forwardHaasAsync()
            .forwardAsync(body.responseURL) {
                requestVariables.copy(body = body.copy(responseURL = it))
            }
            .getResponse()
    }

}