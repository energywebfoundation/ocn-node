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

import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import snc.openchargingnetwork.client.models.exceptions.OcpiHubUnknownReceiverException
import snc.openchargingnetwork.client.models.ocpi.OcpiResponse
import snc.openchargingnetwork.client.services.WalletService
import snc.openchargingnetwork.client.services.HttpService
import snc.openchargingnetwork.client.services.RoutingService


@RestController
@RequestMapping("/ocn/message")
class MessageController(private val routingService: RoutingService,
                        private val walletService: WalletService,
                        private val httpService: HttpService) {


    @PostMapping
    fun postMessage(@RequestHeader("X-Request-ID") requestID: String,
                    @RequestHeader("OCN-Signature") signature: String,
                    @RequestBody body: String): ResponseEntity<OcpiResponse<out Any>> {

        val requestVariables = httpService.convertToRequestVariables(body)

        val sender = requestVariables.headers.sender
        val receiver = requestVariables.headers.receiver

        // verify the signer of the request is authorized to forward messages on behalf of the sender
        walletService.verify(body, signature, sender)

        // check sender has been registered on network
        if (!routingService.isRoleKnownOnNetwork(sender, belongsToMe = false)) {
            throw OcpiHubUnknownReceiverException("Sending party not registered on Open Charging Network")
        }

        // check receiver known to client
        if (!routingService.isRoleKnown(receiver)) {
            throw OcpiHubUnknownReceiverException("Recipient unknown to OCN client entered in Registry")
        }

        val (url, headers) = routingService.prepareLocalPlatformRequest(requestVariables)

        val response = httpService.makeOcpiRequest<Any>(url, headers, requestVariables)

        val responseHeaders = HttpHeaders()
        response.headers["location"]?.let { responseHeaders.set("Location", it) }
        response.headers["Link"]?.let { responseHeaders.set("Link", it) }
        response.headers["X-Total-Count"]?.let { responseHeaders.set("X-Total-Count", it) }
        response.headers["X-Limit"]?.let { responseHeaders.set("X-Limit", it) }

        return ResponseEntity.status(response.statusCode).headers(responseHeaders).body(response.body)
    }

}