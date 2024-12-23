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

package snc.openchargingnetwork.node.controllers.ocn

import com.olisystems.ocnregistryv2_0.OcnRegistry
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.web3j.crypto.Credentials
import snc.openchargingnetwork.node.config.NodeProperties

@RestController
@RequestMapping("\${ocn.node.apiPrefix}/ocn/registry")
class RegistryController(private val properties: NodeProperties,
                         private val registry: OcnRegistry) {

    @GetMapping("/node-info")
    fun getMyNodeInfo() = mapOf(
            "url" to properties.url,
            "address" to Credentials.create(properties.privateKey).address)

    @GetMapping("/node/{countryCode}/{partyID}")
    fun getNodeOf(@PathVariable countryCode: String,
                    @PathVariable partyID: String): Any {
        val countryBytes = countryCode.uppercase().toByteArray()
        val idBytes = partyID.uppercase().toByteArray()

        val (address, url) = registry.getOperatorByOcpi(countryBytes, idBytes).sendAsync().get()

        if (url == "" || address == "0x0000000000000000000000000000000000000000") {
            return "Party not registered on OCN"
        }

        return mapOf("url" to url, "address" to address)
    }

}