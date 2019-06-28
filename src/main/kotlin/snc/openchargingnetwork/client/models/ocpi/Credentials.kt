package snc.openchargingnetwork.client.models.ocpi

import com.fasterxml.jackson.annotation.JsonProperty

data class Credentials(@JsonProperty("token") val token: String,
                       @JsonProperty("url") val url: String,
                       @JsonProperty("roles") val roles: List<CredentialsRole>)

data class CredentialsRole(@JsonProperty("role") val role: Role,
                           @JsonProperty("business_details") val businessDetails: BusinessDetails,
                           @JsonProperty("party_id") val partyID: String,
                           @JsonProperty("country_code") val countryCode: String)
