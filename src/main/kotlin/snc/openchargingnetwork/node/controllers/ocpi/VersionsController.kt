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
@RequestMapping("\${ocn.node.apiPrefix}/ocpi")
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
        val endpoints = this.getAllEndpoints()
        val response = OcpiResponse(
                    OcpiStatus.SUCCESS.code,
                    data = VersionDetail("2.2", endpoints))

        return when {
            repository.existsByAuth_TokenA(token) -> response
            repository.existsByAuth_TokenC(token) -> response
            else -> throw OcpiClientInvalidParametersException("Invalid CREDENTIALS_TOKEN_A")
        }
    }

    private fun getModuleEndpoints(module: ModuleID): List<Endpoint> {
        return InterfaceRole.values().map {
            val paths = if (module == ModuleID.CUSTOM) {
                "/ocpi/custom/${it.id}"
            } else {
                "/ocpi/${it.id}/2.2/${module.id}"
            }
            Endpoint(
                    identifier = module.id,
                    role = it,
                    url = urlJoin(properties.url, paths)
            )
        }
    }

    private fun getAllEndpoints(): List<Endpoint> {
        val endpoints = mutableListOf<Endpoint>()
        val senderOnlyInterfaces = listOf(ModuleID.CREDENTIALS, ModuleID.HUB_CLIENT_INFO)

        for (module in ModuleID.values()) {

            if (senderOnlyInterfaces.contains(module)) {
                // these modules have only SENDER endpoint (the node/hub)
                 endpoints.add(Endpoint(
                        identifier = module.id,
                        role = InterfaceRole.SENDER,
                        url = urlJoin(properties.url, "/ocpi/2.2/${module.id}")))
            } else {
                endpoints.addAll(getModuleEndpoints(module))
            }
        }

        // add custom OcnRules module endpoint
        endpoints.add(Endpoint(
                identifier = "ocnrules",
                role = InterfaceRole.RECEIVER,
                url = urlJoin(properties.url, "/ocpi/2.2/receiver/ocnrules")
        ))

        return endpoints
    }

}