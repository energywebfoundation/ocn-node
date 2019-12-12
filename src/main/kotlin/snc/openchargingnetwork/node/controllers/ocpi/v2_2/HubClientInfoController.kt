/*
    Copyright 2019 Share&Charge Foundation

    This file is part of Open Charging Network Node.

    Open Charging Network Node is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Open Charging Network Node is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Open Charging Network Node.  If not, see <https://www.gnu.org/licenses/>.
*/

package snc.openchargingnetwork.node.controllers.ocpi.v2_2

import org.springframework.web.bind.annotation.*
import snc.openchargingnetwork.node.models.ocpi.*
import snc.openchargingnetwork.node.services.HubClientInfoService
import snc.openchargingnetwork.node.services.RoutingService

@RestController
@RequestMapping("/ocpi/2.2/hubclientinfo")
class HubClientInfoController(private val routingService: RoutingService,
                              private val hubClientInfoService: HubClientInfoService) {

    @GetMapping
    fun getHubClientInfo(@RequestHeader("authorization") authorization: String,
                         @RequestParam("date_from", required = false) dateFrom: String?,
                         @RequestParam("date_to", required = false) dateTo: String?,
                         @RequestParam("offset", required = false) offset: Int?,
                         @RequestParam("limit", required = false) limit: Int?): OcpiResponse<List<ClientInfo>> {

        // TODO: add pagination

        routingService.validateSender(authorization)

        // val params = PaginatedRequest(dateFrom, dateTo, offset, limit).encode()

        return OcpiResponse(statusCode = 1000, data = hubClientInfoService.getLocalList())
    }


}