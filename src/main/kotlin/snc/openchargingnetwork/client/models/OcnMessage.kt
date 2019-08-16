package snc.openchargingnetwork.client.models

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.http.HttpMethod
import snc.openchargingnetwork.client.models.ocpi.CommandType
import snc.openchargingnetwork.client.models.ocpi.InterfaceRole
import snc.openchargingnetwork.client.models.ocpi.ModuleID

@JsonInclude(JsonInclude.Include.NON_NULL)
data class OcnMessageRequestBody<T: Any>(@JsonProperty("module_id") val module: ModuleID,
                                         @JsonProperty("interface_role") val interfaceRole: InterfaceRole,
                                         @JsonProperty("http_method") val method: HttpMethod,
                                         @JsonProperty("headers") val headers: OcpiRequestHeaders,
                                         @JsonProperty("url_path_variables") val urlPathVariables: String? = null,
                                         @JsonProperty("url_encoded_parameters") val urlEncodedParameters: OcpiRequestParameters? = null,
                                         @JsonProperty("body") val body: Any? = null,
                                         @JsonProperty("proxy_resource") var proxyResource: String? = null,
                                         @JsonProperty("expectedResponseType") val expectedResponseType: OcpiResponseDataType<T>)

data class OcnMessageCommandRequestBody(@JsonProperty("type") val type: CommandType,
                                        @JsonProperty("headers") val headers: OcpiRequestHeaders,
                                        @JsonProperty("body") val body: String)

data class OcnMessageHeaders(val requestID: String,
                             val signature: String) {

    fun encode(): Map<String, String> {
        return mapOf(
                "X-Request-ID" to requestID,
                "OCN-Signature" to signature)
    }

}
