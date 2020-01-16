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

package snc.openchargingnetwork.node.config

import org.springframework.boot.context.properties.ConfigurationProperties
import snc.openchargingnetwork.node.tools.generateUUIDv4Token

@ConfigurationProperties("ocn.node")
class NodeProperties {

    var apikey: String = generateUUIDv4Token()

    var dev: Boolean = false

    var signatures: Boolean = false

    lateinit var url: String

    val web3 = Web3()

    class Web3 {

        lateinit var provider: String

        val contracts = Contracts()

        class Contracts {
            lateinit var registry: String
        }
    }
}