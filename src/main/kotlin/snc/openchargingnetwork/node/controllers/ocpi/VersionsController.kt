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

        // custom module
        endpoints.add(Endpoint(
                identifier = "ocnrules",
                role = InterfaceRole.RECEIVER,
                url = urlJoin(properties.url, "/ocpi/2.2/receiver/ocnrules")
        ))

        return endpoints
    }

}