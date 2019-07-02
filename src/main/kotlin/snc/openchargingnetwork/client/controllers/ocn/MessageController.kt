package snc.openchargingnetwork.client.controllers.ocn

import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import snc.openchargingnetwork.client.models.HubRequest
import snc.openchargingnetwork.client.models.HubRequestResponseType
import snc.openchargingnetwork.client.models.exceptions.OcpiHubConnectionProblemException
import snc.openchargingnetwork.client.models.exceptions.OcpiHubUnknownReceiverException
import snc.openchargingnetwork.client.models.ocpi.*
import snc.openchargingnetwork.client.services.RoutingService
import snc.openchargingnetwork.client.tools.urlJoin

@RestController
@RequestMapping("/ocn/message")
class MessageController(val routingService: RoutingService) {

    @PostMapping
    fun postMessage(@RequestHeader("X-Request-ID") requestID: String,
                    @RequestHeader("X-Correlation-ID") correlationID: String,
                    @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
                    @RequestHeader("OCPI-from-party-id") fromPartyID: String,
                    @RequestHeader("OCPI-to-country-code") toCountryCode: String,
                    @RequestHeader("OCPI-to-party-id") toPartyID: String,
                    @RequestBody body: HubRequest): ResponseEntity<OcpiResponse<out Any>> {

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
                        expectedDataType = when (body.type) {
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
                        })

            } else {
                throw OcpiHubUnknownReceiverException()
            }

        } else {
            throw OcpiHubConnectionProblemException("Sending party not registered on Open Charging Network")
        }

        val headers = HttpHeaders()
        response.headers["location"]?.let { headers.add("Location", it) }
        response.headers["Link"]?.let { headers.add("Link", it) }
        response.headers["X-Total-Count"]?.let { headers.add("X-Total-Count", it) }
        response.headers["X-Limit"]?.let { headers.add("X-Limit", it) }

        return ResponseEntity.status(response.statusCode).headers(headers).body(response.body)
    }


}