package snc.connect.broker.models.ocpi

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import snc.connect.broker.enums.ImageCategory
import java.time.Instant
import java.time.format.DateTimeFormatter
import javax.persistence.Embeddable
import javax.persistence.Embedded

data class BasicParty(@JsonProperty("party_id") val id: String,
                      @JsonProperty("country_code") val country: String)

data class RegistrationInformation(@JsonProperty("token") val token: String,
                                   @JsonProperty("versions") val versions: String)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class OcpiResponseBody(@JsonProperty("status_code") val statusCode: Int,
                            @JsonProperty("status_message") val statusMessage: String? = null,
                            @JsonProperty("data") val data: Any? = null,
                            @JsonProperty("timestamp") val timestamp: String = DateTimeFormatter.ISO_INSTANT.format(Instant.now()))

@Embeddable
@JsonInclude(JsonInclude.Include.NON_NULL)
data class BusinessDetails(@JsonProperty("name") val name: String,
                           @JsonProperty("website") val website: String? = null,
                           @Embedded @JsonProperty("logo") val logo: Image? = null)

@Embeddable
@JsonInclude(JsonInclude.Include.NON_NULL)
data class Image(@JsonProperty("url") val url: String,
                 @JsonProperty("thumbnail") val thumbnail: String? = null,
                 @JsonProperty("category") val category: ImageCategory,
                 @JsonProperty("type") val type: String,
                 @JsonProperty("width") val width: Int? = null,
                 @JsonProperty("height") val height: Int? = null)