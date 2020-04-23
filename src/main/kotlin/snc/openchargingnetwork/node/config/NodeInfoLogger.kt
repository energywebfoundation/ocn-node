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

import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.web3j.crypto.Credentials

@Component
class NodeInfoLogger(private val properties: NodeProperties) {

    val hasPrivateKey = properties.privateKey != null

    @EventListener(ApplicationReadyEvent::class)
    fun log() {
        val borderLength = calculateBorderLength(properties.url.length, properties.apikey.length)
        val border = "=".repeat(borderLength)

        val addressText = getAddressText()
        val registryStage = getRegistryStage()
        val stillAliveText = getStillAliveText()
        val plannedPartyText = getPlannedPartyText()

        println("\n${border.substring(0, 3)} NODE INFO ${border.substring(14)}\n" +
                " URL      | ${properties.url}\n" +
                " ADDRESS  | $addressText\n" +
                " API KEY  | ${properties.apikey}\n" +
                " ETH RPC  | ${properties.web3.provider}\n" +
                " REGISTRY | ${properties.web3.contracts.registry} [$registryStage]\n" +
                "${border.substring(0, 3)} FEATURES ${border.substring(13)}\n" +
                " DEV MODE             | ${properties.dev}\n" +
                " SIGNATURES           | ${properties.signatures}\n" +
                " STILL ALIVE CHECK    | $stillAliveText\n" +
                " PLANNED PARTY SEARCH | $plannedPartyText\n")
    }

    private fun calculateBorderLength(url: Int, apikey: Int): Int {
        val baseLength = 22
        val address = 42
        return baseLength + when {
            url >= apikey && url >= address -> url
            apikey >= url && apikey >= address -> apikey
            address >= url && address >= apikey -> address
            else -> 50
        }
    }

    private fun getAddressText(): String = if (hasPrivateKey) {
        Credentials.create(properties.privateKey).address
    } else {
        if (properties.dev) {
            "0x9bC1169Ca09555bf2721A5C9eC6D69c8073bfeB4  [Warning: Generated from a hardcoded private key that should only be used in a local development environment!]"
        } else {
            "Please set \"ocn.node.privateKey\" in your application properties."
        }
    }

    private fun getRegistryStage(): String = when (properties.web3.contracts.registry) {
        "0xd57595D5FA1F94725C426739C449b15D92758D55" -> "test"
        "0x184aeD70F2aaB0Cd1FC62261C1170560cBfd0776" -> "prod"
        else -> "custom"
    }

    private fun getStillAliveText(): String = if (properties.stillAliveEnabled && hasPrivateKey) {
        "true (${properties.stillAliveRate.toLong() / 1000}s)"
    } else {
        "false"
    }

    private fun getPlannedPartyText(): String = if (properties.plannedPartySearchEnabled && hasPrivateKey) {
        "true (${properties.plannedPartySearchRate.toLong() / 1000}s)"
    } else {
        "false"
    }

}