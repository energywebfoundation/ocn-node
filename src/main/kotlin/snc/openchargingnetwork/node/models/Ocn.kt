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


package snc.openchargingnetwork.node.models

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import shareandcharge.openchargingnetwork.notary.SignableHeaders
import snc.openchargingnetwork.node.models.ocpi.BasicRole
import snc.openchargingnetwork.node.models.ocpi.InterfaceRole
import snc.openchargingnetwork.node.models.ocpi.ModuleID
import snc.openchargingnetwork.node.models.ocpi.Role
import java.math.BigInteger

data class OcnMessageHeaders(val requestID: String,
                             val signature: String) {

    fun toMap(): Map<String, String> {
        return mapOf(
                "X-Request-ID" to requestID,
                "OCN-Signature" to signature)
    }

}

// TODO: could differentiate between Function Module headers and Configuration Module headers
@JsonInclude(JsonInclude.Include.NON_NULL)
data class OcnHeaders(@JsonProperty("Authorization") val authorization: String,
                      @JsonProperty("OCN-Signature") var signature: String? = null,
                      @JsonProperty("X-Request-ID") val requestID: String,
                      @JsonProperty("X-Correlation-ID") val correlationID: String,
                      val sender: BasicRole,
                      val receiver: BasicRole) {

    fun toMap(routingHeaders: Boolean = true): MutableMap<String, String?> {
        val map = mutableMapOf<String, String?>()
        map["Authorization"] = authorization
        map["OCN-Signature"] = signature
        map["X-Request-ID"] = requestID
        map["X-Correlation-ID"] = correlationID
        if (routingHeaders) {
            map["OCPI-from-country-code"] = sender.country
            map["OCPI-from-party-id"] = sender.id
            map["OCPI-to-country-code"] = receiver.country
            map["OCPI-to-party-id"] = receiver.id
        }
        return map
    }

    fun toSignedHeaders(): SignableHeaders {
        return SignableHeaders(
                correlationId = correlationID,
                fromCountryCode = sender.country,
                fromPartyId = sender.id,
                toCountryCode = receiver.country,
                toPartyId = receiver.id)
    }

}

enum class Receiver {
    LOCAL,
    REMOTE,
}


data class OcnRules(val signatures: Boolean, val whitelist: OcnRulesList, val blacklist: OcnRulesList)
data class OcnRulesList(val active: Boolean, val list: List<OcnRulesListParty>)

enum class OcnRulesListType {
    WHITELIST,
    BLACKLIST
}

data class OcnRulesListParty(@JsonProperty("party_id") val id: String,
                             @JsonProperty("country_code") val country: String,
                             @JsonProperty("modules") val modules: List<String>)

data class RegistryPartyDetailsBasic(val address: String, val operator: String)

data class RegistryPartyDetails(val party: BasicRole, val roles: List<Role>, val nodeOperator: String)

data class NewRegistryPartyDetails(val nodeOperator: String, val party: BasicRole, val roles: List<Role> )

data class RegistryNode(val operator: String, val url: String)

data class OcnService(val provider: BasicRole, val permissions: List<OcnServicePermission>)

data class BasicRequestType(val moduleID: ModuleID, val interfaceRole: InterfaceRole)

// each enum value takes a "matcher" which tests a given module/interface
enum class OcnServicePermission(val matches: (request: BasicRequestType) -> Boolean) {
    FORWARD_ALL({true}),
    FORWARD_ALL_SENDER({it.interfaceRole == InterfaceRole.SENDER}),
    FORWARD_ALL_RECEIVER({it.interfaceRole == InterfaceRole.RECEIVER}),
    FORWARD_MODULE_LOCATIONS_SENDER({it.moduleID == ModuleID.LOCATIONS && it.interfaceRole == InterfaceRole.SENDER}),
    FORWARD_MODULE_LOCATIONS_RECEIVER({it.moduleID == ModuleID.LOCATIONS && it.interfaceRole == InterfaceRole.RECEIVER}),
    FORWARD_MODULE_SESSIONS_SENDER({it.moduleID == ModuleID.SESSIONS && it.interfaceRole == InterfaceRole.SENDER}),
    FORWARD_MODULE_SESSIONS_RECEIVER({it.moduleID == ModuleID.SESSIONS && it.interfaceRole == InterfaceRole.RECEIVER }),
    FORWARD_MODULE_CDRS_SENDER({it.moduleID == ModuleID.CDRS && it.interfaceRole == InterfaceRole.SENDER}),
    FORWARD_MODULE_CDRS_RECEIVER({it.moduleID == ModuleID.CDRS && it.interfaceRole == InterfaceRole.RECEIVER}),
    FORWARD_MODULE_TARIFFS_SENDER({it.moduleID == ModuleID.TARIFFS && it.interfaceRole == InterfaceRole.SENDER}),
    FORWARD_MODULE_TARIFFS_RECEIVER({it.moduleID == ModuleID.TARIFFS && it.interfaceRole == InterfaceRole.RECEIVER}),
    FORWARD_MODULE_TOKENS_SENDER({it.moduleID == ModuleID.TOKENS && it.interfaceRole == InterfaceRole.SENDER}),
    FORWARD_MODULE_TOKENS_RECEIVER({it.moduleID == ModuleID.TOKENS && it.interfaceRole == InterfaceRole.RECEIVER}),
    FORWARD_MODULE_COMMANDS_SENDER({it.moduleID == ModuleID.COMMANDS && it.interfaceRole == InterfaceRole.SENDER}),
    FORWARD_MODULE_COMMANDS_RECEIVER({it.moduleID == ModuleID.COMMANDS && it.interfaceRole == InterfaceRole.RECEIVER}),
    FORWARD_MODULE_CHARGINGPROFILES_SENDER({it.moduleID == ModuleID.CHARGING_PROFILES && it.interfaceRole == InterfaceRole.SENDER}),
    FORWARD_MODULE_CHARGINGPROFILES_RECEIVER({it.moduleID == ModuleID.CHARGING_PROFILES && it.interfaceRole == InterfaceRole.RECEIVER});


    companion object {
        fun getByIndex(index: BigInteger): OcnServicePermission? {
            return try {
                values()[index.intValueExact()]
            } catch (e: ArrayIndexOutOfBoundsException) {
                null
            }
        }
    }
}

fun OcnServicePermission.matches(moduleID: ModuleID, interfaceRole: InterfaceRole): Boolean {
    return matches(BasicRequestType(moduleID, interfaceRole))
}
