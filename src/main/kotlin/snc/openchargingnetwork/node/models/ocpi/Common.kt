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
import shareandcharge.openchargingnetwork.notary.ValuesToSign
import snc.openchargingnetwork.node.models.OcnHeaders
import snc.openchargingnetwork.node.models.exceptions.OcpiClientInvalidParametersException
import java.math.BigInteger
import java.time.Instant
import java.time.format.DateTimeFormatter
import javax.persistence.Embeddable
import javax.persistence.Embedded


// TODO: rename to avoid confusion?
// BasicParty may be a better description
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

    fun toUpperCase(): BasicRole {
        return BasicRole(id = id.toUpperCase(), country = country.toUpperCase())
    }
}


@JsonInclude(JsonInclude.Include.NON_NULL)
data class OcpiRequestVariables(@JsonProperty("module") val module: ModuleID,
                                @JsonProperty("interface_role") val interfaceRole: InterfaceRole,
                                @JsonProperty("method") val method: HttpMethod,
                                @JsonProperty("headers") val headers: OcnHeaders,
                                @JsonProperty("url_path") val urlPath: String? = null,
                                @JsonProperty("query_params") val queryParams: Map<String, Any?>? = null,
                                @JsonProperty("proxy_uid") val proxyUID: String? = null,
                                @JsonProperty("proxy_resource") val proxyResource: String? = null,
                                @JsonProperty("custom_module_id") val customModuleId: String? = null,
                                @JsonProperty("body") val body: Any? = null) {

    init {
        if (module != ModuleID.CUSTOM && customModuleId != null) {
            throw IllegalStateException("customModuleId defined but module is not CUSTOM")
        }
        if (module == ModuleID.CUSTOM && customModuleId == null) {
            throw IllegalStateException("customModuleId not defined but module is CUSTOM")
        }
    }

    fun toSignedValues(): ValuesToSign<*> {
        return ValuesToSign(
                headers = headers.toSignedHeaders(),
                params = queryParams,
                body = body)
    }

    fun resolveModuleId(): String {
        return if (module != ModuleID.CUSTOM) {
            module.id
        } else {
            customModuleId!!
        }
    }

}


data class RegistrationInfo(@JsonProperty("token") val token: String,
                            @JsonProperty("versions") val versions: String)


@JsonInclude(JsonInclude.Include.NON_NULL)
data class OcpiResponse<T>(@JsonProperty("status_code") val statusCode: Int,
                           @JsonProperty("status_message") val statusMessage: String? = null,
                           @JsonProperty("data") val data: T? = null,
                           @JsonProperty("timestamp") val timestamp: String = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                           @JsonProperty("ocn_signature") var signature: String? = null)


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


enum class OcpiStatus(val code: Int) {
    SUCCESS(1000),
    CLIENT_ERROR(2000),
    CLIENT_INVALID_PARAMETERS(2001),
    CLIENT_NOT_ENOUGH_INFO(2002),
    CLIENT_UNKNOWN_LOCATION(2003),
    CLIENT_UNKNOWN_TOKEN(2004),
    CLIENT_BLACKLISTED(2900),
    SERVER_ERROR(3000),
    SERVER_UNUSABLE_API(3001),
    SERVER_UNSUPPORTED_VERSION(3002),
    SERVER_NO_MATCHING_ENDPOINTS(3003),
    HUB_UNKNOWN_RECEIVER(4001),
    HUB_REQUEST_TIMEOUT(4002),
    HUB_CONNECTION_PROBLEM(4003)
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
    TOKENS("tokens"),
    CUSTOM("custom")
}


enum class InterfaceRole(val id: String) {
    SENDER(id = "sender"),
    RECEIVER(id = "receiver");

    companion object {
        fun values(): List<InterfaceRole> {
            return listOf(SENDER, RECEIVER)
        }
        fun resolve(role: String): InterfaceRole {
            val values = values()
            return values.find { it.id.toLowerCase() == role }
                    ?: throw OcpiClientInvalidParametersException("No interface $role found. Expected one of $values.")
        }
    }
}


enum class Role {
    CPO,
    EMSP,
    HUB,
    NAP,
    NSP,
    OTHER,
    SCSP;

    companion object {
        fun getByIndex(index: Int): Role {
            return values()[index]
        }
        fun getByIndex(index: BigInteger): Role {
            return values()[index.intValueExact()]
        }
    }

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
