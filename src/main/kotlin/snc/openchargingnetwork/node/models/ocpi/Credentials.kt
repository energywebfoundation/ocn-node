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

import com.fasterxml.jackson.annotation.JsonProperty

data class Credentials(@JsonProperty("token") val token: String,
                       @JsonProperty("url") val url: String,
                       @JsonProperty("roles") val roles: List<CredentialsRole>)

data class CredentialsRole(@JsonProperty("role") val role: Role,
                           @JsonProperty("business_details") val businessDetails: BusinessDetails,
                           @JsonProperty("party_id") val partyID: String,
                           @JsonProperty("country_code") val countryCode: String)
