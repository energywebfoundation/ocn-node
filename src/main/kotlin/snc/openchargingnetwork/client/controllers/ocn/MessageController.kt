package snc.openchargingnetwork.client.controllers.ocn

import org.springframework.web.bind.annotation.*
import snc.openchargingnetwork.client.models.HubRequest
import snc.openchargingnetwork.client.models.exceptions.OcpiHubConnectionProblemException
import snc.openchargingnetwork.client.models.exceptions.OcpiHubUnknownReceiverException
import snc.openchargingnetwork.client.models.ocpi.BasicRole
import snc.openchargingnetwork.client.models.ocpi.OcpiResponse
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
                    @RequestBody body: HubRequest): OcpiResponse<Any> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        // check sender has been registered on network
        return if (routingService.isRoleKnownOnNetwork(sender)) {

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
                        expectedDataType = Any::class)

            } else {
                throw OcpiHubUnknownReceiverException()
            }

        } else {
            throw OcpiHubConnectionProblemException("Sending party not registered on Open Charging Network")
        }
    }


}