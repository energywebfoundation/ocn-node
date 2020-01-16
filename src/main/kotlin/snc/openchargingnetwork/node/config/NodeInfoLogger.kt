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

import org.springframework.stereotype.Component
import snc.openchargingnetwork.node.services.WalletService

@Component
class NodeInfoLogger(properties: NodeProperties,
                     walletService: WalletService) {

    init {
        val borderLength = calculateBorderLength(properties.url.length, properties.apikey.length)
        val border = "=".repeat(borderLength)
        println("\n$border\n" +
                "URL        | ${properties.url}\n" +
                "ADDRESS    | ${walletService.address}\n" +
                "APIKEY     | ${properties.apikey}\n" +
                "SIGNATURES | ${properties.signatures}" +
                "\n$border\n")
    }

    private fun calculateBorderLength(url: Int, apikey: Int): Int {
        val baseLength = 13
        val address = 42
        return baseLength + when {
            url >= apikey && url >= address -> url
            apikey >= url && apikey >= address -> apikey
            address >= url && address >= apikey -> address
            else -> 50
        }
    }

}