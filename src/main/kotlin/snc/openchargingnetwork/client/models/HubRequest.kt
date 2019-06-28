package snc.openchargingnetwork.client.models

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import snc.openchargingnetwork.client.models.ocpi.InterfaceRole

@JsonInclude(JsonInclude.Include.NON_NULL)
class HubRequest(@JsonProperty("method") val method: String,
                 @JsonProperty("module") val module: String,
                 @JsonProperty("role") val role: InterfaceRole,
                 @JsonProperty("path") val path: String? = null,
                 @JsonProperty("params") val params: Map<String, String>? = null,
                 @JsonProperty("body") val body: Any? = null)