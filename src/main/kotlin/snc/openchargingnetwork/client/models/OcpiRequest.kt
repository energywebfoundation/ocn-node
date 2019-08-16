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
import snc.openchargingnetwork.client.models.ocpi.CDR as ChargeDetailRecord
import kotlin.reflect.KClass

data class OcpiRequestVariables<T: Any>(val module: ModuleID,
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
                                        val expectedResponseType: OcpiResponseDataType<T>)

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

sealed class OcpiResponseDataType<T: Any>(val type: KClass<T>) {
    object LOCATION: OcpiResponseDataType<Location>(Location::class)
    object LOCATION_ARRAY: OcpiResponseDataType<Array<Location>>(Array<Location>::class)
    object EVSE: OcpiResponseDataType<Evse>(Evse::class)
    object CONNECTOR: OcpiResponseDataType<Connector>(Connector::class)
    object SESSION: OcpiResponseDataType<Session>(Session::class)
    object SESSION_ARRAY: OcpiResponseDataType<Array<Session>>(Array<Session>::class)
    object CHARGING_PREFERENCE_RESPONSE: OcpiResponseDataType<ChargingPreferencesResponse>(ChargingPreferencesResponse::class)
    object CDR: OcpiResponseDataType<ChargeDetailRecord>(ChargeDetailRecord::class)
    object CDR_ARRAY: OcpiResponseDataType<Array<ChargeDetailRecord>>(Array<ChargeDetailRecord>::class)
    object TARIFF: OcpiResponseDataType<Tariff>(Tariff::class)
    object TARIFF_ARRAY: OcpiResponseDataType<Array<Tariff>>(Array<Tariff>::class)
    object TOKEN: OcpiResponseDataType<Token>(Token::class)
    object TOKEN_ARRAY: OcpiResponseDataType<Array<Token>>(Array<Token>::class)
    object AUTHORIZATION_INFO: OcpiResponseDataType<AuthorizationInfo>(AuthorizationInfo::class)
    object COMMAND_RESPONSE: OcpiResponseDataType<CommandResponse>(CommandResponse::class)
    object NOTHING: OcpiResponseDataType<Nothing>(Nothing::class)
}

enum class OcpiRequestType {
    LOCAL,
    REMOTE,
}