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

package snc.openchargingnetwork.node.models.ocpi

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Token(@JsonProperty("country_code") val countryCode: String,
                 @JsonProperty("party_id") val partyID: String,
                 @JsonProperty("uid") val uid: String,
                 @JsonProperty("type") val type: TokenType,
                 @JsonProperty("contract_id") val contractID: String,
                 @JsonProperty("visual_number") val visualNumber: String? = null,
                 @JsonProperty("issuer") val issuer: String,
                 @JsonProperty("group_id") val groupID: String? = null,
                 @JsonProperty("valid") val valid: Boolean,
                 @JsonProperty("whitelist") val whitelist: WhitelistType,
                 @JsonProperty("language") val language: String? = null,
                 @JsonProperty("default_profile_type") val defaultProfileType: ProfileType? = null,
                 @JsonProperty("energy_contract") val energyContract: EnergyContract? = null,
                 @JsonProperty("last_updated") val lastUpdated: String)

enum class TokenType {
    AD_HOC_USER,
    APP_USER,
    OTHER,
    RFID
}

enum class WhitelistType {
    ALWAYS,
    ALLOWED,
    ALLOWED_OFFLINE,
    NEVER
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class EnergyContract(@JsonProperty("supplier_name") val supplierName: String,
                          @JsonProperty("contract_id") val contractID: String? = null)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class LocationReferences(@JsonProperty("location_id") val locationID: String,
                              @JsonProperty("evse_uids") val evseUIDs: List<String>? = null,
                              @JsonProperty("connector_ids") val connectorIDs: List<String>? = null)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class AuthorizationInfo(@JsonProperty("allowed") val allowed: Allowed,
                             @JsonProperty("token") val token: Token,
                             @JsonProperty("location") val location: LocationReferences? = null,
                             @JsonProperty("authorization_reference") val authorizationReference: String? = null,
                             @JsonProperty("info") val info: DisplayText? = null)

enum class Allowed {
    ALLOWED,
    BLOCKED,
    EXPIRED,
    NO_CREDIT,
    NOT_ALLOWED
}
