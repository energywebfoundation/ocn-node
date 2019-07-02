package snc.openchargingnetwork.client.models

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import snc.openchargingnetwork.client.models.ocpi.CommandType
import snc.openchargingnetwork.client.models.ocpi.InterfaceRole

@JsonInclude(JsonInclude.Include.NON_NULL)
class HubGenericRequest(@JsonProperty("method") val method: String,
                        @JsonProperty("module") val module: String,
                        @JsonProperty("role") val role: InterfaceRole,
                        @JsonProperty("path") val path: String? = null,
                        @JsonProperty("params") val params: Map<String, String>? = null,
                        @JsonProperty("body") val body: Any? = null,
                        @JsonProperty("expectedResponseType") val expectedResponseType: HubRequestResponseType = HubRequestResponseType.NOTHING)

enum class HubRequestResponseType {
    LOCATION,
    LOCATION_ARRAY,
    EVSE,
    CONNECTOR,
    SESSION,
    SESSION_ARRAY,
    CHARGING_PREFERENCE_RESPONSE,
    CDR,
    CDR_ARRAY,
    TARIFF,
    TARIFF_ARRAY,
    TOKEN,
    TOKEN_ARRAY,
    AUTHORIZATION_INFO,
    COMMAND_RESPONSE,
    NOTHING
}

class HubCommandsRequest(@JsonProperty("type") val type: CommandType,
                         @JsonProperty("body") val body: String)