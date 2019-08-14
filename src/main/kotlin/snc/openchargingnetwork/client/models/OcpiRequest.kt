package snc.openchargingnetwork.client.models

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.http.HttpMethod
import snc.openchargingnetwork.client.models.ocpi.BasicRole
import snc.openchargingnetwork.client.models.ocpi.InterfaceRole
import snc.openchargingnetwork.client.models.ocpi.ModuleID
import snc.openchargingnetwork.client.models.ocpi.TokenType
import kotlin.reflect.KClass

class OcpiRequestHeaders(@JsonProperty("Authorization") val authorization: String? = null,
                         @JsonProperty("X-Request-ID") val requestID: String,
                         @JsonProperty("X-Correlation-ID") val correlationID: String,
                         @JsonProperty("OCPI-From-Country-Code") val ocpiFromCountryCode: String,
                         @JsonProperty("OCPI-From-Party-ID") val ocpiFromPartyID: String,
                         @JsonProperty("OCPI-To-Country-Code") val ocpiToCountryCode: String,
                         @JsonProperty("OCPI-To-Party-ID") val ocpiToPartyID: String)

@JsonInclude(JsonInclude.Include.NON_NULL)
class OcpiRequestParameters(@JsonProperty("type") val type: TokenType? = null,
                            @JsonProperty("date_from") val dateFrom: String? = null,
                            @JsonProperty("date_to") val dateTo: String? = null,
                            @JsonProperty("offset") val offset: Int? = null,
                            @JsonProperty("limit") val limit: Int? = null) {

    fun encode(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        if (type != null) {
            map["type"] = type.toString()
        }
        if (dateFrom != null) {
            map["date_from"] = dateFrom
        }
        if (dateTo != null) {
            map["date_to"] = dateTo
        }
        if (offset != null) {
            map["offset"] = offset.toString()
        }
        if (limit != null) {
            map["limit"] = limit.toString()
        }
        return map
    }
}

data class OcpiRequest<T: Any>(val module: ModuleID,
                               val interfaceRole: InterfaceRole,
                               val method: HttpMethod,
                               val headers: OcpiRequestHeaders,
                               val urlEncodedParameters: OcpiRequestParameters? = null,
                               val urlPath: String? = null,
                               val body: Any? = null,
                               val expectedResponseBodyType: KClass<T>? = null) {

    val sender = BasicRole(headers.ocpiFromPartyID, headers.ocpiFromCountryCode)
    val receiver = BasicRole(headers.ocpiToPartyID, headers.ocpiToCountryCode)

}