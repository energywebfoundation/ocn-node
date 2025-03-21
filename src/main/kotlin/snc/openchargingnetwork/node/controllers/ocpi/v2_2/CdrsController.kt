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
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import snc.openchargingnetwork.node.components.OcpiRequestHandlerBuilder
import snc.openchargingnetwork.node.models.OcnHeaders
import snc.openchargingnetwork.node.models.ocpi.*
import snc.openchargingnetwork.node.tools.filterNull

@RequestMapping("\${ocn.node.apiPrefix}")
@RestController
class CdrsController(private val requestHandlerBuilder: OcpiRequestHandlerBuilder) {


    /**
     * SENDER INTERFACE
     */

    @GetMapping("/ocpi/sender/2.2/cdrs")
    fun getCdrsFromDataOwner(@RequestHeader("authorization") authorization: String,
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
                             @RequestParam("limit", required = false) limit: Int?): ResponseEntity<OcpiResponse<Array<CDR>>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        val params = mapOf("date_from" to dateFrom, "date_to" to dateTo, "offset" to offset, "limit" to limit).filterNull()

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.CDRS,
                interfaceRole = InterfaceRole.SENDER,
                method = HttpMethod.GET,
                headers = OcnHeaders(authorization, signature, requestID, correlationID, sender, receiver),
                queryParams = params)

        // TODO: all pagination response header links should contain original url-encoded parameters
        return requestHandlerBuilder
               .build<Array<CDR>>(requestVariables)
               .forwardDefault()
               .getResponseWithPaginationHeaders() // proxies the Link response header
    }

    @GetMapping("/ocpi/sender/2.2/cdrs/page/{uid}")
    fun getCdrPageFromDataOwner(@RequestHeader("authorization") authorization: String,
                                @RequestHeader("OCN-Signature") signature: String? = null,
                                @RequestHeader("X-Request-ID") requestID: String,
                                @RequestHeader("X-Correlation-ID") correlationID: String,
                                @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
                                @RequestHeader("OCPI-from-party-id") fromPartyID: String,
                                @RequestHeader("OCPI-to-country-code") toCountryCode: String,
                                @RequestHeader("OCPI-to-party-id") toPartyID: String,
                                @PathVariable uid: String): ResponseEntity<OcpiResponse<Array<CDR>>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.CDRS,
                interfaceRole = InterfaceRole.SENDER,
                method = HttpMethod.GET,
                headers = OcnHeaders(authorization, signature, requestID, correlationID, sender, receiver),
                urlPath = uid)

        return requestHandlerBuilder
                .build<Array<CDR>>(requestVariables)
                .forwardDefault(proxied = true) // retrieves proxied Link response header
                .getResponseWithPaginationHeaders()
    }


    /**
     * RECEIVER INTERFACE
     */

    @GetMapping("/ocpi/receiver/2.2/cdrs/{cdrID}")
    fun getClientOwnedCdr(@RequestHeader("authorization") authorization: String,
                          @RequestHeader("OCN-Signature") signature: String? = null,
                          @RequestHeader("X-Request-ID") requestID: String,
                          @RequestHeader("X-Correlation-ID") correlationID: String,
                          @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
                          @RequestHeader("OCPI-from-party-id") fromPartyID: String,
                          @RequestHeader("OCPI-to-country-code") toCountryCode: String,
                          @RequestHeader("OCPI-to-party-id") toPartyID: String,
                          @PathVariable cdrID: String): ResponseEntity<OcpiResponse<CDR>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.CDRS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.GET,
                headers = OcnHeaders(authorization, signature, requestID, correlationID, sender, receiver),
                urlPath = cdrID)

        return requestHandlerBuilder
                .build<CDR>(requestVariables)
                .forwardDefault(proxied = true) // retrieves proxied Location response header
                .getResponse()
    }

//    @Transactional
    @PostMapping("/ocpi/receiver/2.2/cdrs")
    fun postClientOwnedCdr(@RequestHeader("authorization") authorization: String,
                           @RequestHeader("OCN-Signature") signature: String? = null,
                           @RequestHeader("X-Request-ID") requestID: String,
                           @RequestHeader("X-Correlation-ID") correlationID: String,
                           @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
                           @RequestHeader("OCPI-from-party-id") fromPartyID: String,
                           @RequestHeader("OCPI-to-country-code") toCountryCode: String,
                           @RequestHeader("OCPI-to-party-id") toPartyID: String,
                           @RequestBody body: CDR): ResponseEntity<OcpiResponse<Unit>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.CDRS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.POST,
                headers = OcnHeaders(authorization, signature, requestID, correlationID, sender, receiver),
                body = body)

        return requestHandlerBuilder
                .build<Unit>(requestVariables)
                .forwardDefault()
                .getResponseWithLocationHeader("/ocpi/receiver/2.2/cdrs")
    }

}