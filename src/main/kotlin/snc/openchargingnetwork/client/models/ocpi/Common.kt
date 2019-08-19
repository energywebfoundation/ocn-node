/*
    Copyright 2019 Energy Web Foundation

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

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import snc.openchargingnetwork.client.models.exceptions.OcpiClientInvalidParametersException
import java.time.Instant
import java.time.format.DateTimeFormatter
import javax.persistence.Embeddable
import javax.persistence.Embedded

@Embeddable
data class BasicRole(@JsonProperty("party_id") var id: String,
                     @JsonProperty("country_code") var country: String) {

    init {
        if (country.length != 2) {
            throw OcpiClientInvalidParametersException("Given country_code \"$country\" not 2 characters")
        }
        if (id.length != 3) {
            throw OcpiClientInvalidParametersException("Given party-id \"$id\" not 3 characters")
        }
    }

    fun toLowerCase(): BasicRole {
        this.id = id.toLowerCase()
        this.country = country.toLowerCase()
        return this
    }
}

data class RegistrationInfo(@JsonProperty("token") val token: String,
                            @JsonProperty("versions") val versions: String)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PaginatedRequest(@JsonProperty("date_from") val dateFrom: String? = null,
                            @JsonProperty("date_to") val dateTo: String? = null,
                            @JsonProperty("offset") val offset: Int? = null,
                            @JsonProperty("limit") val limit: Int? = null) {

    fun encode(): Map<String, String> {
        val map = mutableMapOf<String, String>()
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

@JsonInclude(JsonInclude.Include.NON_NULL)
data class OcpiResponse<T>(@JsonProperty("status_code") val statusCode: Int,
                           @JsonProperty("status_message") val statusMessage: String? = null,
                           @JsonProperty("data") val data: T? = null,
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

data class DisplayText(@JsonProperty("language") val language: String,
                       @JsonProperty("text") val text: String)

enum class OcpiStatus(val code: Int, val message: String? = null) {
    SUCCESS(1000),
    CLIENT_ERROR(2000),
    CLIENT_INVALID_PARAMETERS(2001, "Invalid or missing parameters"),
    CLIENT_NOT_ENOUGH_INFO(2002, "Not enough information"),
    CLIENT_UNKNOWN_LOCATION(2003, "Unknown location"),
    SERVER_ERROR(3000),
    SERVER_UNUSABLE_API(3001),
    SERVER_UNSUPPORTED_VERSION(3002),
    SERVER_NO_MATCHING_ENDPOINTS(3003),
    HUB_UNKNOWN_RECEIVER(4001, "Unknown receiver"),
    HUB_REQUEST_TIMEOUT(4002, "Timeout on forwarded request"),
    HUB_CONNECTION_PROBLEM(4003, "Connection problem")
}

enum class ConnectionStatus {
    CONNECTED,
    OFFLINE,
    PLANNED,
    SUSPENDED
}

enum class ModuleID(val id: String) {
    CDRS("cdrs"),
    CHARGING_PROFILES("chargingprofiles"),
    COMMANDS("commands"),
    CREDENTIALS("credentials"),
    HUB_CLIENT_INFO("hubclientinfo"),
    LOCATIONS("locations"),
    SESSIONS("sessions"),
    TARIFFS("tariffs"),
    TOKENS("tokens")
}

enum class InterfaceRole(val id: String) {
    SENDER(id = "sender"),
    RECEIVER(id = "receiver")
}

enum class Role {
    CPO,
    EMSP,
    HUB,
    NSP,
    OTHER,
    SCSP
}

enum class ImageCategory {
    CHARGER,
    ENTRANCE,
    LOCATION,
    NETWORK,
    OPERATOR,
    OTHER,
    OWNER
}

enum class AuthMethod {
    AUTH_REQUEST,
    COMMAND,
    WHITELIST
}