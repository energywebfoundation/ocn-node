package snc.openchargingnetwork.client.controllers.ocpi.v2_2

import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import snc.openchargingnetwork.client.config.Properties
import snc.openchargingnetwork.client.models.HubRequest
import snc.openchargingnetwork.client.models.HubRequestResponseType
import snc.openchargingnetwork.client.models.entities.CdrEntity
import snc.openchargingnetwork.client.models.exceptions.OcpiClientUnknownLocationException
import snc.openchargingnetwork.client.models.ocpi.*
import snc.openchargingnetwork.client.repositories.CdrRepository
import snc.openchargingnetwork.client.services.RoutingService
import snc.openchargingnetwork.client.tools.urlJoin

@RestController
class CdrsController(val routingService: RoutingService,
                     val properties: Properties,
                     val cdrRepository: CdrRepository) {

    /**
     * SENDER INTERFACE
     */

    @GetMapping("/ocpi/cpo/2.2/cdrs")
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
            val endpoint = routingService.getPlatformEndpoint(platformID, "cdrs", InterfaceRole.CPO)
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
                    url = urlJoin(url, "/ocn/messages"),
                    headers = headers,
                    body = HubRequest(
                            method = "GET",
                            module = "cdrs",
                            role = InterfaceRole.CPO,
                            params = params,
                            type = HubRequestResponseType.CDR_ARRAY),
                    expectedDataType = Array<CDR>::class)
        }

        val headers = HttpHeaders()
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

    @GetMapping("/ocpi/emsp/2.2/cdrs/{cdrID}")
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

        val cdr = cdrRepository.findByCdrIDAndOwnerIDAndOwnerCountryAndCreatorIDAndCreatorCountryAllIgnoreCase(cdrID, receiver.id, receiver.country, sender.id, sender.country)
                ?: throw OcpiClientUnknownLocationException("CDR with id '$cdrID' does not exist")

        val response = if (routingService.isRoleKnown(receiver)) {
            val platformID = routingService.getPlatformID(receiver)
            val headers = routingService.makeHeaders(platformID, correlationID, sender, receiver)
            routingService.forwardRequest(
                    method = "GET",
                    url = cdr.location,
                    headers = headers,
                    expectedDataType = CDR::class)
        } else {
            val url = routingService.findBrokerUrl(receiver)
            val headers = routingService.makeHeaders(correlationID, sender, receiver)
            routingService.forwardRequest(
                    method = "POST",
                    url = urlJoin(url, "/ocn/messages"),
                    headers = headers,
                    body = HubRequest(
                            method = "GET",
                            module = "cdrs",
                            path = cdr.location,
                            role = InterfaceRole.MSP,
                            type = HubRequestResponseType.CDR),
                    expectedDataType = CDR::class)
        }

        return ResponseEntity.status(response.statusCode).body(response.body)
    }

    @PostMapping("/ocpi/emsp/2.2/cdrs")
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

        // TODO: could also validate that sender matches party_id and country_code in body
        routingService.validateSender(authorization, sender)

        val response = if (routingService.isRoleKnown(receiver)) {
            val platformID = routingService.getPlatformID(receiver)
            val endpoint = routingService.getPlatformEndpoint(platformID, "cdrs", InterfaceRole.MSP)
            val headers = routingService.makeHeaders(platformID, correlationID, sender, receiver)
            routingService.forwardRequest(
                    method = "PUT",
                    url = endpoint.url,
                    headers = headers,
                    body = body,
                    expectedDataType = Nothing::class)
        } else {
            val url = routingService.findBrokerUrl(receiver)
            val headers = routingService.makeHeaders(correlationID, sender, receiver)
            routingService.forwardRequest(
                    method = "POST",
                    url = urlJoin(url, "/ocn/messages"),
                    headers = headers,
                    body = HubRequest(
                            method = "PUT",
                            module = "cdrs",
                            path = url,
                            role = InterfaceRole.MSP,
                            body = body),
                    expectedDataType = Nothing::class)
        }

        val headers = HttpHeaders()

        response.headers["Location"]?.let {

            cdrRepository.save(CdrEntity(
                    cdrID = body.id,
                    ownerID = receiver.id,
                    ownerCountry = receiver.country,
                    creatorID = sender.id,
                    creatorCountry = sender.country,
                    location = it
            ))

            val cdr = urlJoin(properties.url, "/ocpi/emsp/2.2/cdrs/${body.id}")
            headers.add("Location", cdr)
        }

        return ResponseEntity
                .status(response.statusCode)
                .headers(headers)
                .body(response.body)
    }

}