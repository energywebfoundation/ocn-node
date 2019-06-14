package snc.connect.broker.models.ocpi

import com.fasterxml.jackson.annotation.JsonProperty

data class BasicParty(@JsonProperty("party_id") val id: String,
                      @JsonProperty("country_code") val country: String)

data class RegistrationInformation(@JsonProperty("token") val token: String,
                                   @JsonProperty("versions") val versions: String)