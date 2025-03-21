/*
    Copyright 2019-2020 eMobilify GmbH

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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.RestTemplate
import snc.openchargingnetwork.node.components.OcpiRequestHandlerBuilder
import snc.openchargingnetwork.node.config.Verification
import snc.openchargingnetwork.node.models.OcnHeaders
import snc.openchargingnetwork.node.models.ocpi.*
import snc.openchargingnetwork.node.tools.filterNull


@RestController
@RequestMapping("\${ocn.node.apiPrefix}")
class TariffsController(private val requestHandlerBuilder: OcpiRequestHandlerBuilder, private val coroutineScope: CoroutineScope) {


    /**
     * SENDER INTERFACE
     */

    @GetMapping("/ocpi/sender/2.2/tariffs")
    fun getTariffsFromDataOwner(@RequestHeader("authorization") authorization: String,
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
                                @RequestParam("limit", required = false) limit: Int?): ResponseEntity<OcpiResponse<Array<Tariff>>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        val params = mapOf("date_from" to dateFrom, "date_to" to dateTo, "offset" to offset, "limit" to limit).filterNull()

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.TARIFFS,
                interfaceRole = InterfaceRole.SENDER,
                method = HttpMethod.GET,
                headers = OcnHeaders(authorization, signature, requestID, correlationID, sender, receiver),
                queryParams = params)

        return requestHandlerBuilder
                .build<Array<Tariff>>(requestVariables)
                .forwardDefault()
                .getResponseWithPaginationHeaders() // proxies Link response header
    }

    @GetMapping("/ocpi/sender/2.2/tariffs/page/{uid}")
    fun getTariffsPageFromDataOwner(@RequestHeader("authorization") authorization: String,
                                    @RequestHeader("OCN-Signature") signature: String? = null,
                                    @RequestHeader("X-Request-ID") requestID: String,
                                    @RequestHeader("X-Correlation-ID") correlationID: String,
                                    @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
                                    @RequestHeader("OCPI-from-party-id") fromPartyID: String,
                                    @RequestHeader("OCPI-to-country-code") toCountryCode: String,
                                    @RequestHeader("OCPI-to-party-id") toPartyID: String,
                                    @PathVariable uid: String): ResponseEntity<OcpiResponse<Array<Tariff>>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.TARIFFS,
                interfaceRole = InterfaceRole.SENDER,
                method = HttpMethod.GET,
                headers = OcnHeaders(authorization, signature, requestID, correlationID, sender, receiver),
                urlPath = uid)

        return requestHandlerBuilder
                .build<Array<Tariff>>(requestVariables)
                .forwardDefault(proxied = true) // retrieves proxied Link response header
                .getResponseWithPaginationHeaders()
    }


    /**
     * RECEIVER INTERFACE
     */

    @GetMapping("/ocpi/receiver/2.2/tariffs/{countryCode}/{partyID}/{tariffID}")
    fun getClientOwnedTariff(@RequestHeader("authorization") authorization: String,
                             @RequestHeader("OCN-Signature") signature: String? = null,
                             @RequestHeader("X-Request-ID") requestID: String,
                             @RequestHeader("X-Correlation-ID") correlationID: String,
                             @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
                             @RequestHeader("OCPI-from-party-id") fromPartyID: String,
                             @RequestHeader("OCPI-to-country-code") toCountryCode: String,
                             @RequestHeader("OCPI-to-party-id") toPartyID: String,
                             @PathVariable countryCode: String,
                             @PathVariable partyID: String,
                             @PathVariable tariffID: String): ResponseEntity<OcpiResponse<Tariff>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.TARIFFS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.GET,
                headers = OcnHeaders(authorization, signature, requestID, correlationID, sender, receiver),
                urlPath = "/$countryCode/$partyID/$tariffID")

        return requestHandlerBuilder
                .build<Tariff>(requestVariables)
                .forwardDefault()
                .getResponse()
    }


    @PutMapping("/ocpi/receiver/2.2/tariffs/{countryCode}/{partyID}/{tariffID}")
    fun putClientOwnedTariff(@RequestHeader("authorization") authorization: String,
                             @RequestHeader("OCN-Signature") signature: String? = null,
                             @RequestHeader("X-Request-ID") requestID: String,
                             @RequestHeader("X-Correlation-ID") correlationID: String,
                             @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
                             @RequestHeader("OCPI-from-party-id") fromPartyID: String,
                             @RequestHeader("OCPI-to-country-code") toCountryCode: String,
                             @RequestHeader("OCPI-to-party-id") toPartyID: String,
                             @PathVariable countryCode: String,
                             @PathVariable partyID: String,
                             @PathVariable tariffID: String,
                             @RequestBody body: Tariff): ResponseEntity<OcpiResponse<Unit>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.TARIFFS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.PUT,
                headers = OcnHeaders(authorization, signature, requestID, correlationID, sender, receiver),
                urlPath = "/$countryCode/$partyID/$tariffID",
                body = body)

        coroutineScope.launch {
            try {
                requestHandlerBuilder
                    .build<Unit>(requestVariables)
                    .forwardHaas()
            } catch (e: Exception) {
                // Use a proper logger
                //logger.error("Error in async HaaS call: ${e.message}", e)
            }
        }

        // Forward the request to the original destination
        val response = requestHandlerBuilder
            .build<Unit>(requestVariables)
            .forwardDefault()
            .getResponse()

        return response
    }


    @DeleteMapping("/ocpi/receiver/2.2/tariffs/{countryCode}/{partyID}/{tariffID}")
    fun deleteClientOwnedTariff(@RequestHeader("authorization") authorization: String,
                                @RequestHeader("OCN-Signature") signature: String? = null,
                                @RequestHeader("X-Request-ID") requestID: String,
                                @RequestHeader("X-Correlation-ID") correlationID: String,
                                @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
                                @RequestHeader("OCPI-from-party-id") fromPartyID: String,
                                @RequestHeader("OCPI-to-country-code") toCountryCode: String,
                                @RequestHeader("OCPI-to-party-id") toPartyID: String,
                                @PathVariable countryCode: String,
                                @PathVariable partyID: String,
                                @PathVariable tariffID: String): ResponseEntity<OcpiResponse<Unit>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.TARIFFS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.DELETE,
                headers = OcnHeaders(authorization, signature, requestID, correlationID, sender, receiver),
                urlPath = "/$countryCode/$partyID/$tariffID")

        return requestHandlerBuilder
                .build<Unit>(requestVariables)
                .forwardDefault()
                .getResponse()
    }

}
