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

package snc.openchargingnetwork.node.controllers.ocpi

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import snc.openchargingnetwork.node.repositories.PlatformRepository
import snc.openchargingnetwork.node.config.NodeProperties
import snc.openchargingnetwork.node.models.ocpi.InterfaceRole
import snc.openchargingnetwork.node.models.ocpi.ModuleID
import snc.openchargingnetwork.node.models.ocpi.OcpiStatus
import snc.openchargingnetwork.node.models.exceptions.OcpiClientInvalidParametersException
import snc.openchargingnetwork.node.models.ocpi.*
import snc.openchargingnetwork.node.tools.extractToken
import snc.openchargingnetwork.node.tools.urlJoin

@RestController
@RequestMapping("/ocpi")
class VersionsController(private val repository: PlatformRepository,
                         private val properties: NodeProperties) {

    @GetMapping("/versions")
    fun getVersions(@RequestHeader("Authorization") authorization: String): OcpiResponse<List<Version>> {

        val token = authorization.extractToken()
        val endpoint = urlJoin(properties.url, "/ocpi/2.2")
        val versions = listOf(Version("2.2", endpoint))
        val response = OcpiResponse(OcpiStatus.SUCCESS.code, data = versions)

        return when {
            repository.existsByAuth_TokenA(token) -> response
            repository.existsByAuth_TokenC(token) -> response
            else -> throw OcpiClientInvalidParametersException("Invalid CREDENTIALS_TOKEN_A")
        }
    }

    @GetMapping("/2.2")
    fun getVersionsDetail(@RequestHeader("Authorization") authorization: String): OcpiResponse<VersionDetail> {

        val token = authorization.extractToken()
        val endpoints = this.getEndpoints()
        val response = OcpiResponse(
                    OcpiStatus.SUCCESS.code,
                    data = VersionDetail("2.2", endpoints))

        return when {
            repository.existsByAuth_TokenA(token) -> response
            repository.existsByAuth_TokenC(token) -> response
            else -> throw OcpiClientInvalidParametersException("Invalid CREDENTIALS_TOKEN_A")
        }
    }


    private fun getEndpoints(): List<Endpoint> {
        val endpoints = mutableListOf<Endpoint>()

        for (module in ModuleID.values()) {

            if (module.id == "credentials" || module.id == "hubclientinfo") {

                // these modules have only SENDER endpoint (the broker/hub)
                endpoints.add(Endpoint(
                        identifier = module.id,
                        role = InterfaceRole.SENDER,
                        url = urlJoin(properties.url, "/ocpi/2.2/${module.id}")))

            } else if (module.id == "chargingprofiles") {

                // not yet implemented
                continue

            } else {

                // remaining modules have both interfaces implemented
                endpoints.addAll(listOf(
                        Endpoint(
                                identifier = module.id,
                                role = InterfaceRole.SENDER,
                                url = urlJoin(properties.url, "/ocpi/sender/2.2/${module.id}")),
                        Endpoint(
                                identifier = module.id,
                                role = InterfaceRole.RECEIVER,
                                url = urlJoin(properties.url, "/ocpi/receiver/2.2/${module.id}"))))

            }
        }

        return endpoints
    }

}