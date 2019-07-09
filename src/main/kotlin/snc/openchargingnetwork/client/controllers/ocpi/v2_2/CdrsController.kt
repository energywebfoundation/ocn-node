package snc.openchargingnetwork.client.controllers.ocpi.v2_2

import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import snc.openchargingnetwork.client.config.Properties
import snc.openchargingnetwork.client.models.HubGenericRequest
import snc.openchargingnetwork.client.models.HubRequestResponseType
import snc.openchargingnetwork.client.models.ocpi.*
import snc.openchargingnetwork.client.services.RoutingService
import snc.openchargingnetwork.client.tools.urlJoin

@RestController
class CdrsController(val routingService: RoutingService,
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

        val params = PaginatedRequest(dateFrom, dateTo, offset, limit).encode()

        val response = if (routingService.isRoleKnown(receiver)) {
            val platformID = routingService.getPlatformID(receiver)
            val endpoint = routingService.getPlatformEndpoint(platformID, "cdrs", InterfaceRole.SENDER)
            val headers = routingService.makeHeaders(platformID, correlationID, sender, receiver)
            routingService.forwardRequest(
                    method = "GET",
                    url = endpoint.url,
                    headers = headers,
                    params = params,
                    expectedDataType = Array<CDR>::class)
        } else {
            val url = routingService.findBrokerUrl(receiver)
            val headers = routingService.makeHeaders(correlationID, sender, receiver)
            routingService.forwardRequest(
                    method = "POST",
                    url = urlJoin(url, "/ocn/message"),
                    headers = headers,
                    body = HubGenericRequest(
                            method = "GET",
                            module = "cdrs",
                            role = InterfaceRole.SENDER,
                            params = params,
                            body = null,
                            expectedResponseType = HubRequestResponseType.CDR_ARRAY),
                    expectedDataType = Array<CDR>::class)
        }

        val headers = HttpHeaders()
        //TODO: implement brokered pagination (somehow)
        response.headers["Link"]?.let { headers.add("Link", "<RESPONSE_URL>; rel=\"next\"")}
        response.headers["X-Total-Count"]?.let { headers.add("X-Total-Count", it) }
        response.headers["X-Limit"]?.let { headers.add("X-Limit", it) }

        return ResponseEntity
                .status(response.statusCode)
                .headers(headers)
                .body(response.body)
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

        val response = if (routingService.isRoleKnown(receiver)) {
            val platformID = routingService.getPlatformID(receiver)
            val headers = routingService.makeHeaders(platformID, correlationID, sender, receiver)
            val url = routingService.findCDR(cdrID, sender, receiver)
            routingService.forwardRequest(
                    method = "GET",
                    url = url,
                    headers = headers,
                    expectedDataType = CDR::class)
        } else {
            val url = routingService.findBrokerUrl(receiver)
            val headers = routingService.makeHeaders(correlationID, sender, receiver)
            //TODO: save URL on remote broker
            routingService.forwardRequest(
                    method = "POST",
                    url = urlJoin(url, "/ocn/message"),
                    headers = headers,
                    body = HubGenericRequest(
                            method = "GET",
                            module = "cdrs",
                            role = InterfaceRole.RECEIVER,
                            body = null,
                            expectedResponseType = HubRequestResponseType.CDR),
                    expectedDataType = CDR::class)
        }

        return ResponseEntity.status(response.statusCode).body(response.body)
    }

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
        val objectData = BasicRole(body.partyID, body.countryCode)

        routingService.validateSender(authorization, sender, objectData)

        val response = if (routingService.isRoleKnown(receiver)) {
            val platformID = routingService.getPlatformID(receiver)
            val endpoint = routingService.getPlatformEndpoint(platformID, "cdrs", InterfaceRole.RECEIVER)
            val headers = routingService.makeHeaders(platformID, correlationID, sender, receiver)
            routingService.forwardRequest(
                    method = "POST",
                    url = endpoint.url,
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
                            module = "cdrs",
                            path = url,
                            role = InterfaceRole.RECEIVER,
                            body = body),
                    expectedDataType = Nothing::class)
        }

        val headers = HttpHeaders()

        response.headers["Location"]?.let {
            routingService.saveCDR(body.id, it, sender, receiver)
            val cdr = urlJoin(properties.url, "/ocpi/receiver/2.2/cdrs/${body.id}")
            headers.add("Location", cdr)
        }

        return ResponseEntity
                .status(response.statusCode)
                .headers(headers)
                .body(response.body)
    }

}