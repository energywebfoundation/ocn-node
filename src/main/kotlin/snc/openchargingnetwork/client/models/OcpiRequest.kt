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

package snc.openchargingnetwork.client.models

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.http.HttpMethod
import snc.openchargingnetwork.client.models.ocpi.*
import kotlin.reflect.KClass

data class OcpiRequestVariables(val module: ModuleID,
                                val interfaceRole: InterfaceRole,
                                val method: HttpMethod,
                                val requestID: String,
                                val correlationID: String,
                                val sender: BasicRole,
                                val receiver: BasicRole,
                                val urlPathVariables: String? = null,
                                val urlEncodedParameters: OcpiRequestParameters? = null,
                                val body: Any? = null,
                                val proxyResource: String? = null,
                                val expectedResponseType: OcpiResponseDataType) {

    companion object {
        // TODO: combine OcpiRequestVariables with OcnMessageRequestBody
        fun fromOcnMessage(ocnMessage: OcnMessageRequestBody): OcpiRequestVariables {
            return OcpiRequestVariables(
                    module = ocnMessage.module,
                    interfaceRole = ocnMessage.interfaceRole,
                    method = ocnMessage.method,
                    requestID = ocnMessage.headers.requestID,
                    correlationID = ocnMessage.headers.correlationID,
                    sender = BasicRole(ocnMessage.headers.ocpiFromPartyID, ocnMessage.headers.ocpiFromCountryCode),
                    receiver = BasicRole(ocnMessage.headers.ocpiToPartyID, ocnMessage.headers.ocpiToCountryCode),
                    urlPathVariables = ocnMessage.urlPathVariables,
                    urlEncodedParameters = ocnMessage.urlEncodedParameters,
                    body = ocnMessage.body,
                    proxyResource = ocnMessage.proxyResource,
                    expectedResponseType = ocnMessage.expectedResponseType)
        }
    }

}

data class OcpiRequestHeaders(@JsonProperty("Authorization") val authorization: String? = null,
                              @JsonProperty("X-Request-ID") val requestID: String,
                              @JsonProperty("X-Correlation-ID") val correlationID: String,
                              @JsonProperty("OCPI-from-country-code") val ocpiFromCountryCode: String,
                              @JsonProperty("OCPI-from-party-id") val ocpiFromPartyID: String,
                              @JsonProperty("OCPI-to-country-code") val ocpiToCountryCode: String,
                              @JsonProperty("OCPI-to-party-id") val ocpiToPartyID: String) {

    fun encode(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        if (authorization != null) {
            map["Authorization"] = authorization
        }
        map["X-Request-ID"] = requestID
        map["X-Correlation-ID"] = correlationID
        map["OCPI-from-country-code"] = ocpiFromCountryCode
        map["OCPI-from-party-id"] = ocpiFromPartyID
        map["OCPI-to-country-code"] = ocpiToCountryCode
        map["OCPI-to-party-id"] = ocpiToPartyID
        return map
    }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class OcpiRequestParameters(@JsonProperty("type") val type: TokenType? = null,
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

enum class OcpiResponseDataType(val type: KClass<*>) {
    LOCATION(Location::class),
    LOCATION_ARRAY(Array<Location>::class),
    EVSE(Evse::class),
    CONNECTOR(Connector::class),
    SESSION(Session::class),
    SESSION_ARRAY(Array<Session>::class),
    CHARGING_PREFERENCE_RESPONSE(ChargingPreferencesResponse::class),
    CDR(snc.openchargingnetwork.client.models.ocpi.CDR::class),
    CDR_ARRAY(Array<snc.openchargingnetwork.client.models.ocpi.CDR>::class),
    TARIFF(Tariff::class),
    TARIFF_ARRAY(Array<Tariff>::class),
    TOKEN(Token::class),
    TOKEN_ARRAY(Array<Token>::class),
    AUTHORIZATION_INFO(AuthorizationInfo::class),
    COMMAND_RESPONSE(CommandResponse::class),
    NOTHING(Nothing::class),
}

enum class OcpiRequestType {
    LOCAL,
    REMOTE,
}