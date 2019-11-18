/*
    Copyright 2019 Share&Charge Foundation

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

package snc.openchargingnetwork.client.models.ocpi

import com.fasterxml.jackson.annotation.JsonProperty

data class Credentials(@JsonProperty("token") val token: String,
                       @JsonProperty("url") val url: String,
                       @JsonProperty("roles") val roles: List<CredentialsRole>)

data class CredentialsRole(@JsonProperty("role") val role: Role,
                           @JsonProperty("business_details") val businessDetails: BusinessDetails,
                           @JsonProperty("party_id") val partyID: String,
                           @JsonProperty("country_code") val countryCode: String)
