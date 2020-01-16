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
