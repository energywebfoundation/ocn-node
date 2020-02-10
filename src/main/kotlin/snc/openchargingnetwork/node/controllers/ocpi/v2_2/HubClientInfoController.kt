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