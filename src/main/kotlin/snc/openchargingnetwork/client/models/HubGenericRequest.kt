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
import snc.openchargingnetwork.client.models.ocpi.CommandType
import snc.openchargingnetwork.client.models.ocpi.InterfaceRole

@JsonInclude(JsonInclude.Include.NON_NULL)
class HubGenericRequest<T>(@JsonProperty("method") val method: String,
                        @JsonProperty("module") val module: String,
                        @JsonProperty("role") val role: InterfaceRole,
                        @JsonProperty("path") val path: String? = null,
                        @JsonProperty("params") val params: Map<String, String>? = null,
                        @JsonProperty("body") val body: T? = null,
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