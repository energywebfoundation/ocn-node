package snc.connect.broker.models.ocpi

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant
import java.time.format.DateTimeFormatter

data class BasicParty(@JsonProperty("party_id") val id: String,
                      @JsonProperty("country_code") val country: String)

data class RegistrationInformation(@JsonProperty("token") val token: String,
                                   @JsonProperty("versions") val versions: String)

data class OcpiResponseBody(@JsonProperty("status_code") val statusCode: Int,
                            @JsonProperty("status_message", required = false) val statusMessage: String? = null,
                            @JsonProperty("data", required = false) val data: Any? = null,
                            @JsonProperty("timestamp") val timestamp: String = DateTimeFormatter.ISO_INSTANT.format(Instant.now()))

