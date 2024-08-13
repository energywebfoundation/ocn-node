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

package snc.openchargingnetwork.node.config

import org.springframework.boot.context.properties.ConfigurationProperties
import snc.openchargingnetwork.node.tools.generateUUIDv4Token
import javax.annotation.PostConstruct

@ConfigurationProperties("ocn.node")
class NodeProperties {

    var apikey: String = generateUUIDv4Token()

    var base64apiKey: String = "";

    var dev: Boolean = false

    var privateKey: String? = null

    var signatures: Boolean = true

    lateinit var url: String

    var web3 = Web3()

    class Web3 {

        lateinit var provider: String

        var contracts = Contracts()

        class Contracts {
            lateinit var registry: String
            lateinit var permissions: String
        }
    }

    var stillAliveRate: String = "900000" // defaults to 15 minutes

    var stillAliveEnabled: Boolean = true

    var plannedPartySearchRate: String = "3600000" // defaults to 1 hour

    var plannedPartySearchEnabled: Boolean = true

    var serviceInterfaceEnabled: Boolean = true

    @PostConstruct
    fun init() {
        base64apiKey = java.util.Base64.getEncoder().encodeToString(apikey.toByteArray());
    }
}