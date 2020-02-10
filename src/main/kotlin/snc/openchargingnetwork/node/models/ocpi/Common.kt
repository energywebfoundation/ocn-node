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

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.http.HttpMethod
import shareandcharge.openchargingnetwork.notary.OcpiRequest
import shareandcharge.openchargingnetwork.notary.OcpiUrlEncodedParameters
import snc.openchargingnetwork.node.models.OcnHeaders
import snc.openchargingnetwork.node.models.exceptions.OcpiClientInvalidParametersException
import java.time.Instant
import java.time.format.DateTimeFormatter
import javax.persistence.Embeddable
import javax.persistence.Embedded


@Embeddable
data class BasicRole(@JsonProperty("party_id") final val id: String,
                     @JsonProperty("country_code") final val country: String) {

    init {
        if (country.length != 2) {
            throw OcpiClientInvalidParametersException("Given country_code \"$country\" not 2 characters")
        }
        if (id.length != 3) {
            throw OcpiClientInvalidParametersException("Given party-id \"$id\" not 3 characters")
        }
    }
}


@JsonInclude(JsonInclude.Include.NON_NULL)
data class OcpiRequestVariables(@JsonProperty("module") val module: ModuleID,
                                @JsonProperty("interface_role") val interfaceRole: InterfaceRole,
                                @JsonProperty("method") val method: HttpMethod,
                                @JsonProperty("headers") val headers: OcnHeaders,
                                @JsonProperty("url_path_variables") val urlPathVariables: String? = null,
                                @JsonProperty("url_encoded_params") val urlEncodedParams: Map<String, Any?>? = null,
                                @JsonProperty("proxy_uid") val proxyUID: String? = null,
                                @JsonProperty("proxy_resource") val proxyResource: String? = null,
                                @JsonProperty("body") val body: Any? = null) {

    fun toNotaryReadableVariables(): OcpiRequest<*> {
        return OcpiRequest(
                headers = headers.toNotaryReadableHeaders(),
                params = urlEncodedParams?.run {
                    OcpiUrlEncodedParameters(
                            countryCode = get("country_code")?.toString(),
                            partyId = get("party_id")?.toString(),
                            tokenUid = get("token_uid")?.toString(),
                            type = get("type")?.toString(),
                            dateFrom = get("date_from")?.toString(),
                            date_to = get("date_to")?.toString(),
                            offset = get("offset")?.toString(),
                            limit = get("limit")?.toString())
                },
                body = body)
        }
}


data class RegistrationInfo(@JsonProperty("token") val token: String,
                            @JsonProperty("versions") val versions: String)


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
    UNKNOWN_TOKEN(2004, "Unknown token"),
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
    NAP,
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
