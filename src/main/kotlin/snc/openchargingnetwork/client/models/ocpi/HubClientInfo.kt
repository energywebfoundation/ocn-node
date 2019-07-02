package snc.openchargingnetwork.client.models.ocpi

import com.fasterxml.jackson.annotation.JsonProperty

data class ClientInfo(@JsonProperty("party_id") val partyID: String,
                      @JsonProperty("country_code") val countryCode: String,
                      @JsonProperty("role") val role: Role,
                      @JsonProperty("status") val status: ConnectionStatus,
                      @JsonProperty("last_updated") val lastUpdated: String)
