/*
    Copyright 2019 Energy Web Foundation

    This file is part of Open Charging Network Client.

    Open Charging Network Client is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Open Charging Network Client is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Open Charging Network Client.  If not, see <https://www.gnu.org/licenses/>.
*/


package snc.openchargingnetwork.client.models

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.http.HttpMethod
import snc.openchargingnetwork.client.models.ocpi.InterfaceRole
import snc.openchargingnetwork.client.models.ocpi.ModuleID

//@JsonInclude(JsonInclude.Include.NON_NULL)
//data class OcnMessageRequestBody(@JsonProperty("module_id") val module: ModuleID,
//                                         @JsonProperty("interface_role") val interfaceRole: InterfaceRole,
//                                         @JsonProperty("http_method") val method: HttpMethod,
//                                         @JsonProperty("headers") val headers: OcpiRequestHeaders,
//                                         @JsonProperty("url_path_variables") val urlPathVariables: String? = null,
//                                         @JsonProperty("url_encoded_parameters") val urlEncodedParameters: OcpiRequestParameters? = null,
//                                         @JsonProperty("body") val body: Any? = null,
//                                         @JsonProperty("proxy_resource") var proxyResource: String? = null,
//                                         @JsonProperty("expected_response_type") val expectedResponseType: OcpiDataType)

//data class OcnMessageCommandRequestBody(@JsonProperty("type") val type: CommandType,
//                                        @JsonProperty("headers") val headers: OcpiRequestHeaders,
//                                        @JsonProperty("body") val body: String)

data class OcnMessageHeaders(val requestID: String,
                             val signature: String) {

    fun encode(): Map<String, String> {
        return mapOf(
                "X-Request-ID" to requestID,
                "OCN-Signature" to signature)
    }

}

enum class Recipient {
    LOCAL,
    REMOTE,
}
