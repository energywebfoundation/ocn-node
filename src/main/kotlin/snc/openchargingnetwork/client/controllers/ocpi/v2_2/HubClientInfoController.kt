package snc.openchargingnetwork.client.controllers.ocpi.v2_2

import org.springframework.web.bind.annotation.*
import snc.openchargingnetwork.client.models.ocpi.*
import snc.openchargingnetwork.client.services.RoutingService

@RestController
@RequestMapping("/ocpi/hub/2.2/hubclientinfo")
class HubClientInfoController(private val routingService: RoutingService) {

    /**
     * SENDER INTERFACE
     */

    @GetMapping
    fun getHubClientInfo(@RequestHeader("authorization") authorization: String,
                         @RequestHeader("X-Request-ID") requestID: String,
                         @RequestHeader("X-Correlation-ID") correlationID: String,
                         @RequestParam("date_from", required = false) dateFrom: String?,
                         @RequestParam("date_to", required = false) dateTo: String?,
                         @RequestParam("offset", required = false) offset: Int?,
                         @RequestParam("limit", required = false) limit: Int?): OcpiResponse<Array<ClientInfo>> {

        // TODO: add pagination

        routingService.validateSender(authorization)

        // val params = PaginatedRequest(dateFrom, dateTo, offset, limit).encode()

        return OcpiResponse(statusCode = 1000, data = routingService.findClientInfo())
    }


}