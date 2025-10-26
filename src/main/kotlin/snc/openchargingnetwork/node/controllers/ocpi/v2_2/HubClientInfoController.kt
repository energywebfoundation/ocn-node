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

import com.fasterxml.jackson.module.kotlin.readValue
import jakarta.websocket.server.PathParam
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import snc.openchargingnetwork.node.components.HttpClientComponent
import snc.openchargingnetwork.node.components.OcpiRequestHandlerBuilder
import snc.openchargingnetwork.node.config.HCIProperties
import snc.openchargingnetwork.node.config.NodeProperties
import snc.openchargingnetwork.node.models.OcnHeaders
import snc.openchargingnetwork.node.models.ocpi.BasicRole
import snc.openchargingnetwork.node.models.ocpi.ClientInfo
import snc.openchargingnetwork.node.models.ocpi.InterfaceRole
import snc.openchargingnetwork.node.models.ocpi.ModuleID
import snc.openchargingnetwork.node.models.ocpi.OcpiRequestVariables
import snc.openchargingnetwork.node.models.ocpi.OcpiResponse
import snc.openchargingnetwork.node.services.HubClientInfoService
import snc.openchargingnetwork.node.services.ModuleNotificationService
import snc.openchargingnetwork.node.services.RoutingService
import snc.openchargingnetwork.node.services.WalletService
import snc.openchargingnetwork.node.tools.filterNull

@RestController
@RequestMapping("\${ocn.node.apiPrefix}/ocpi/2.2.1/hubclientinfo")
class HubClientInfoController(
    private val routingService: RoutingService,
    private val hubClientInfoService: HubClientInfoService,
    private val requestHandlerBuilder: OcpiRequestHandlerBuilder,
    private val hciProperties: HCIProperties,
    private val nodeProperties: NodeProperties,
    private val walletService: WalletService,
    private val httpClientComponent: HttpClientComponent,
    private val moduleNotificationService: ModuleNotificationService,
) {

    @GetMapping
    fun getHubClientInfo(
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
    ): ResponseEntity<OcpiResponse<Array<ClientInfo>>> {

        if (toCountryCode == "OCN" && toPartyID == "CH") {
            return this.handleInternalClientInfoRequest(fromCountryCode, fromPartyID, authorization);
        }

        val params =
            mapOf(
                "date_from" to dateFrom,
                "date_to" to dateTo,
                "offset" to offset,
                "limit" to limit
            ).filterNull()

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        val requestVariables = OcpiRequestVariables(
            module = ModuleID.HUB_CLIENT_INFO,
            interfaceRole = InterfaceRole.SENDER,
            method = HttpMethod.GET,
            headers = OcnHeaders(authorization, signature, requestID, correlationID, sender, receiver),
            queryParams = params
        )

        return requestHandlerBuilder
            .build<Array<ClientInfo>>(requestVariables)
            .forwardDefault() // retrieves proxied Link response header
            .getResponseWithPaginationHeaders()
    }

    @GetMapping("/{country_code}/{party_id}")
    fun getHubClientInfo(
        @RequestHeader("authorization") authorization: String,
        @RequestHeader("OCN-Signature") signature: String? = null,
        @RequestHeader("X-Request-ID") requestID: String,
        @RequestHeader("X-Correlation-ID") correlationID: String,
        @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
        @RequestHeader("OCPI-from-party-id") fromPartyID: String,
        @RequestHeader("OCPI-to-country-code") toCountryCode: String,
        @RequestHeader("OCPI-to-party-id") toPartyID: String,
        @PathVariable("country_code") countryCode: String,
        @PathVariable("party_id") partyID: String
    ): ResponseEntity<OcpiResponse<ClientInfo>> {
        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        val requestVariables = OcpiRequestVariables(
            module = ModuleID.HUB_CLIENT_INFO,
            interfaceRole = InterfaceRole.SENDER,
            method = HttpMethod.GET,
            headers = OcnHeaders(authorization, signature, requestID, correlationID, sender, receiver),
            urlPath = countryCode + "/" + partyID
        )

        return requestHandlerBuilder
            .build<ClientInfo>(requestVariables)
            .forwardDefault() // retrieves proxied Link response header
            .getResponseWithPaginationHeaders()
    }

    @PutMapping
    fun updateClientInfo(
        @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
        @RequestHeader("OCPI-from-party-id") fromPartyID: String,
        @RequestHeader("OCN-Signature") signature: String?,
        @RequestBody body: String
    ): ResponseEntity<Any> {
        val sender = BasicRole(fromPartyID, fromCountryCode)

        if(!nodeProperties.dev && nodeProperties.signatures) {
            walletService.verify(body, signature ?: "", sender)
        }

        val clientInfo: ClientInfo = httpClientComponent.mapper.readValue(body)

        if (hciProperties.countryCode != fromCountryCode || hciProperties.partyId != fromPartyID
        ) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Invalid Hub Client Info publisher")
        }

        hubClientInfoService.saveClientInfo(clientInfo)

        val parties =
            moduleNotificationService.getPartiesToNotifyOfModuleChange(
                moduleId = ModuleID.HUB_CLIENT_INFO,
                partyId = fromPartyID,
                countryCode = fromCountryCode
            )

        if (parties.isNotEmpty()) {
            val filteredParties = parties.filter { it.countryCode != clientInfo.countryCode && it.partyID != clientInfo.partyID }

            moduleNotificationService.notifyPartiesOfModuleChangeAsync(
                moduleId = ModuleID.HUB_CLIENT_INFO,
                parties = filteredParties,
                changedData = clientInfo,
                urlPath = "${clientInfo.countryCode}/${clientInfo.partyID}"
            )
        }

        return ResponseEntity.ok("New client info object stored and broadcasted")
    }


    private fun handleInternalClientInfoRequest(
        fromCountryCode: String,
        fromPartyID: String,
        authorization: String,
    ): ResponseEntity<OcpiResponse<Array<ClientInfo>>> {
        // TODO: implement pagination
        val sender = BasicRole(fromPartyID, fromCountryCode)
        routingService.checkSenderKnown(authorization, sender)
        // val params = PaginatedRequest(dateFrom, dateTo, offset, limit).encode()
        val result = hubClientInfoService.getList(authorization).toTypedArray()
        val count = result.size.toString()

        val headers = HttpHeaders()
        headers["X-Total-Count"] = count
        headers["X-Limit"] = count

        return ResponseEntity
            .ok()
            .headers(headers)
            .body(
                OcpiResponse(
                    statusCode = 1000,
                    statusMessage = "Pagination request parameters were ignored due to lack of their implementation on the OCN.",
                    data = result
                )
            )
    }


}