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

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import snc.openchargingnetwork.client.config.Properties
import snc.openchargingnetwork.client.models.*
import snc.openchargingnetwork.client.models.entities.ProxyResourceEntity
import snc.openchargingnetwork.client.models.ocpi.*
import snc.openchargingnetwork.client.repositories.ProxyResourceRepository
import snc.openchargingnetwork.client.services.HttpRequestService
import snc.openchargingnetwork.client.services.RoutingService
import snc.openchargingnetwork.client.tools.urlJoin

@RestController
class CdrsController(val routingService: RoutingService,
                     val httpService: HttpRequestService,
                     val proxyResourceRepo: ProxyResourceRepository,
                     val properties: Properties) {

    /**
     * SENDER INTERFACE
     */

    @GetMapping("/ocpi/sender/2.2/cdrs")
    fun getCdrsFromDataOwner(@RequestHeader("authorization") authorization: String,
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

        routingService.validateSender(authorization, sender)

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.Cdrs,
                interfaceRole = InterfaceRole.SENDER,
                method = HttpMethod.GET,
                requestID = requestID,
                correlationID = correlationID,
                sender = sender,
                receiver = receiver,
                urlEncodedParameters = OcpiRequestParameters(
                        dateFrom = dateFrom,
                        dateTo = dateTo,
                        offset = offset,
                        limit = limit),
                expectedResponseType = OcpiResponseDataType.CDR_ARRAY)

        val response = when (routingService.validateReceiver(receiver)) {

            OcpiRequestType.LOCAL -> {

                val (url, headers) = routingService.prepareLocalPlatformRequest(requestVariables)

                httpService.makeRequest(
                        method = requestVariables.method,
                        url = url,
                        headers = headers,
                        params = requestVariables.urlEncodedParameters,
                        expectedDataType = requestVariables.expectedResponseType)
            }

            OcpiRequestType.REMOTE -> {

                val (url, headers, body) = routingService.prepareRemotePlatformRequest(requestVariables)

                httpService.postClientMessage(url = url, headers = headers, body = body)
            }

        }

        val headers = routingService.proxyPaginationHeaders(response.headers)

        return ResponseEntity
                .status(response.statusCode)
                .headers(headers)
                .body(response.body)


//        val sender = BasicRole(fromPartyID, fromCountryCode)
//        val receiver = BasicRole(toPartyID, toCountryCode)
//
//        routingService.validateSender(authorization, sender)
//
//        val params = HubRequestParameters(dateFrom = dateFrom, dateTo = dateTo, offset = offset, limit = limit)
//
//        val response = if (routingService.isRoleKnown(receiver)) {
//            val platformID = routingService.getPlatformID(receiver)
//            val endpoint = routingService.getPlatformEndpoint(platformID, "cdrs", InterfaceRole.SENDER)
//            val headers = routingService.makeHeaders(platformID, correlationID, sender, receiver)
//            routingService.forwardRequest(
//                    method = "GET",
//                    url = endpoint.url,
//                    headers = headers,
//                    params = params.encode(),
//                    expectedDataType = Array<CDR>::class)
//        } else {
//            val url = routingService.findBrokerUrl(receiver)
//            val headers = routingService.makeHeaders(requestID, correlationID, sender, receiver)
//            val hubRequestBody = HubGenericRequest(
//                    method = "GET",
//                    module = "cdrs",
//                    role = InterfaceRole.SENDER,
//                    params = params,
//                    headers = headers,
//                    body = null,
//                    expectedResponseType = HubRequestResponseType.CDR_ARRAY)
//            routingService.forwardRequest(
//                    method = "POST",
//                    url = urlJoin(url, "/ocn/message"),
//                    headers = mapOf(
//                            "X-Request-ID" to generateUUIDv4Token(),
//                            "OCN-Signature" to routingService.signRequest(hubRequestBody)),
//                    body = hubRequestBody,
//                    expectedDataType = Array<CDR>::class)
//        }
//
//        val headers = HttpHeaders()
//        //TODO: implement brokered pagination (somehow)
//        response.headers["Link"]?.let { headers.add("Link", "<RESPONSE_URL>; rel=\"next\"")}
//        response.headers["X-Total-Count"]?.let { headers.add("X-Total-Count", it) }
//        response.headers["X-Limit"]?.let { headers.add("X-Limit", it) }
//
//        return ResponseEntity
//                .status(response.statusCode)
//                .headers(headers)
//                .body(response.body)
    }

    /**
     * RECEIVER INTERFACE
     */

    @GetMapping("/ocpi/receiver/2.2/cdrs/{cdrID}")
    fun getCdr(@RequestHeader("authorization") authorization: String,
                              @RequestHeader("X-Request-ID") requestID: String,
                              @RequestHeader("X-Correlation-ID") correlationID: String,
                              @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
                              @RequestHeader("OCPI-from-party-id") fromPartyID: String,
                              @RequestHeader("OCPI-to-country-code") toCountryCode: String,
                              @RequestHeader("OCPI-to-party-id") toPartyID: String,
                              @PathVariable cdrID: String): ResponseEntity<OcpiResponse<CDR>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        routingService.validateSender(authorization, sender)

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.Cdrs,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.GET,
                requestID = requestID,
                correlationID = correlationID,
                sender = sender,
                receiver = receiver,
                urlPathVariables = cdrID,
                expectedResponseType = OcpiResponseDataType.CDR)

        val response = when (routingService.validateReceiver(receiver)) {

            OcpiRequestType.LOCAL -> {

                val (url, headers) = routingService.prepareLocalProxiedPlatformRequest(requestVariables)

                httpService.makeRequest(
                        method = requestVariables.method,
                        url = url,
                        headers = headers,
                        expectedDataType = requestVariables.expectedResponseType)
            }

            OcpiRequestType.REMOTE -> {

                val (url, headers, body) = routingService.prepareRemotePlatformRequest(requestVariables)

                httpService.postClientMessage(url = url, headers = headers, body = body)
            }

        }

        return ResponseEntity.status(response.statusCode).body(response.body)


//        val sender = BasicRole(fromPartyID, fromCountryCode)
//        val receiver = BasicRole(toPartyID, toCountryCode)
//
//        routingService.validateSender(authorization, sender)
//
//        val response = if (routingService.isRoleKnown(receiver)) {
//            val platformID = routingService.getPlatformID(receiver)
//            val headers = routingService.makeHeaders(platformID, correlationID, sender, receiver)
//            val url = routingService.findCDR(cdrID, sender, receiver)
//            routingService.forwardRequest(
//                    method = "GET",
//                    url = url,
//                    headers = headers,
//                    expectedDataType = CDR::class)
//        } else {
//            val url = routingService.findBrokerUrl(receiver)
//            val headers = routingService.makeHeaders(requestID, correlationID, sender, receiver)
//            val hubRequestBody = HubGenericRequest(
//                    method = "GET",
//                    module = "cdrs",
//                    role = InterfaceRole.RECEIVER,
//                    headers = headers,
//                    body = null,
//                    expectedResponseType = HubRequestResponseType.CDR)
//            //TODO: save URL on remote broker
//            routingService.forwardRequest(
//                    method = "POST",
//                    url = urlJoin(url, "/ocn/message"),
//                    headers = mapOf(
//                            "X-Request-ID" to generateUUIDv4Token(),
//                            "OCN-Signature" to routingService.signRequest(hubRequestBody)),
//                    body = hubRequestBody,
//                    expectedDataType = CDR::class)
//        }
//
//        return ResponseEntity.status(response.statusCode).body(response.body)
    }
//
    @Transactional
    @PostMapping("/ocpi/receiver/2.2/cdrs")
    fun postCdr(@RequestHeader("authorization") authorization: String,
                @RequestHeader("X-Request-ID") requestID: String,
                @RequestHeader("X-Correlation-ID") correlationID: String,
                @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
                @RequestHeader("OCPI-from-party-id") fromPartyID: String,
                @RequestHeader("OCPI-to-country-code") toCountryCode: String,
                @RequestHeader("OCPI-to-party-id") toPartyID: String,
                @RequestBody body: CDR): ResponseEntity<OcpiResponse<Nothing>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        routingService.validateSender(authorization, sender)

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.Cdrs,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.POST,
                requestID = requestID,
                correlationID = correlationID,
                sender = sender,
                receiver = receiver,
                body = body,
                expectedResponseType = OcpiResponseDataType.NOTHING)

        val response = when (routingService.validateReceiver(receiver)) {

            OcpiRequestType.LOCAL -> {

                val (url, headers) = routingService.prepareLocalPlatformRequest(requestVariables)

                httpService.makeRequest(
                        method = requestVariables.method,
                        url = url,
                        headers = headers,
                        expectedDataType = requestVariables.expectedResponseType)
            }

            OcpiRequestType.REMOTE -> {

                val (url, headers, ocnBody) = routingService.prepareRemotePlatformRequest(requestVariables)

                httpService.postClientMessage(url = url, headers = headers, body = ocnBody)
            }

        }

        val headers = HttpHeaders()

        response.headers["Location"]?.let {

            val resourceID = routingService.setProxyResource(it, sender, receiver)

            headers["Location"] = urlJoin(properties.url, "/ocpi/receiver/2.2/cdrs/$resourceID")

        }

        return ResponseEntity.status(response.statusCode).body(response.body)



//        val sender = BasicRole(fromPartyID, fromCountryCode)
//        val receiver = BasicRole(toPartyID, toCountryCode)
//        val objectData = BasicRole(body.partyID, body.countryCode)
//
//        routingService.validateSender(authorization, sender, objectData)
//
//        val response = if (routingService.isRoleKnown(receiver)) {
//            val platformID = routingService.getPlatformID(receiver)
//            val endpoint = routingService.getPlatformEndpoint(platformID, "cdrs", InterfaceRole.RECEIVER)
//            val headers = routingService.makeHeaders(platformID, correlationID, sender, receiver)
//            routingService.forwardRequest(
//                    method = "POST",
//                    url = endpoint.url,
//                    headers = headers,
//                    body = body,
//                    expectedDataType = Nothing::class)
//        } else {
//            val url = routingService.findBrokerUrl(receiver)
//            val headers = routingService.makeHeaders(requestID, correlationID, sender, receiver)
//            val hubRequestBody = HubGenericRequest(
//                    method = "POST",
//                    module = "cdrs",
//                    path = url,
//                    role = InterfaceRole.RECEIVER,
//                    headers = headers,
//                    body = body)
//            routingService.forwardRequest(
//                    method = "POST",
//                    url = urlJoin(url, "/ocn/message"),
//                    headers = mapOf(
//                            "X-Request-ID" to generateUUIDv4Token(),
//                            "OCN-Signature" to routingService.signRequest(hubRequestBody)),
//                    body = hubRequestBody,
//                    expectedDataType = Nothing::class)
//        }
//
//        val headers = HttpHeaders()
//
//        response.headers["Location"]?.let {
//            routingService.saveCDR(body.id, it, sender, receiver)
//            val cdr = urlJoin(properties.url, "/ocpi/receiver/2.2/cdrs/${body.id}")
//            headers.add("Location", cdr)
//        }
//
//        return ResponseEntity
//                .status(response.statusCode)
//                .headers(headers)
//                .body(response.body)
    }

}