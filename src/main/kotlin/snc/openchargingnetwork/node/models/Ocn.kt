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
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.RequestHeader
import shareandcharge.openchargingnetwork.notary.OcpiHeaders
import snc.openchargingnetwork.node.models.ocpi.BasicRole


data class OcnMessageHeaders(val requestID: String,
                             val signature: String) {

    fun toMap(): Map<String, String> {
        return mapOf(
                "X-Request-ID" to requestID,
                "OCN-Signature" to signature)
    }

}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class OcnHeaders(@JsonProperty("Authorization") val authorization: String,
                      @JsonProperty("OCN-Signature") var signature: String? = null,
                      @JsonProperty("X-Request-ID") val requestID: String,
                      @JsonProperty("X-Correlation-ID") val correlationID: String,
                      val sender: BasicRole,
                      val receiver: BasicRole) {

    fun toMap(): Map<String, String?> {
        val map = mutableMapOf<String, String?>()
        map["Authorization"] = authorization
        map["OCN-Signature"] = signature
        map["X-Request-ID"] = requestID
        map["X-Correlation-ID"] = correlationID
        map["OCPI-from-country-code"] = sender.country
        map["OCPI-from-party-id"] = sender.id
        map["OCPI-to-country-code"] = receiver.country
        map["OCPI-to-party-id"] = receiver.id
        return map
    }

    fun toNotaryReadableHeaders(): OcpiHeaders {
        return OcpiHeaders(
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

data class OcnRulesList(val active: Boolean, val list: List<BasicRole>)
data class OcnRules(val signatures: Boolean, val whitelist: OcnRulesList, val blacklist: OcnRulesList)

enum class OcnRulesListType {
    WHITELIST,
    BLACKLIST
}