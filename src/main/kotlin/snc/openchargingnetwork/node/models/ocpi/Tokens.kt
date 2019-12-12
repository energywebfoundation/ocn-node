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
