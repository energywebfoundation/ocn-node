package snc.connect.broker.models.ocpi

import com.fasterxml.jackson.annotation.JsonProperty
import snc.connect.broker.enums.Role

data class Credentials(@JsonProperty("token") val token: String,
                       @JsonProperty("url") val url: String,
                       @JsonProperty("roles") val roles: Array<CredentialsRole>) {
    override fun equals(other: Any?): Boolean {
        return super.equals(other)
    }
    override fun hashCode(): Int {
        return super.hashCode()
    }
}

data class CredentialsRole(@JsonProperty("role") val role: Role,
                           @JsonProperty("business_details") val businessDetails: BusinessDetails,
                           @JsonProperty("party_id") val partyID: String,
                           @JsonProperty("country_code") val countryCode: String)
