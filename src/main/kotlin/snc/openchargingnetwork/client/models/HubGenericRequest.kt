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
import snc.openchargingnetwork.client.models.ocpi.*
import snc.openchargingnetwork.client.models.ocpi.CDR as ChargeDetailRecord
import kotlin.reflect.KClass

data class HubRequestHeaders(@JsonProperty("Authorization") val authorization: String? = null,
                             @JsonProperty("X-Request-ID") val requestID: String,
                             @JsonProperty("X-Correlation-ID") val correlationID: String,
                             @JsonProperty("OCPI-From-Country-Code") val ocpiFromCountryCode: String,
                             @JsonProperty("OCPI-From-Party-ID") val ocpiFromPartyID: String,
                             @JsonProperty("OCPI-To-Country-Code") val ocpiToCountryCode: String,
                             @JsonProperty("OCPI-To-Party-ID") val ocpiToPartyID: String)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class HubRequestParameters(@JsonProperty("type") val type: TokenType? = null,
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

@JsonInclude(JsonInclude.Include.NON_NULL)
data class HubGenericRequest<T: Any>(@JsonProperty("method") val method: String,
                                     @JsonProperty("module") val module: String,
                                     @JsonProperty("role") val role: InterfaceRole,
                                     @JsonProperty("path") val path: String? = null,
                                     @JsonProperty("params") val params: HubRequestParameters? = null,
                                     @JsonProperty("headers") val headers: HubRequestHeaders,
                                     @JsonProperty("body") val body: Any? = null,
                                     @JsonProperty("expectedResponseType") val expectedResponseType: HubRequestResponseType<T>)

sealed class HubRequestResponseType<T: Any>(val type: KClass<T>) {
    object LOCATION: HubRequestResponseType<Location>(Location::class)
    object LOCATION_ARRAY: HubRequestResponseType<Array<Location>>(Array<Location>::class)
    object EVSE: HubRequestResponseType<Evse>(Evse::class)
    object CONNECTOR: HubRequestResponseType<Connector>(Connector::class)
    object SESSION: HubRequestResponseType<Session>(Session::class)
    object SESSION_ARRAY: HubRequestResponseType<Array<Session>>(Array<Session>::class)
    object CHARGING_PREFERENCE_RESPONSE: HubRequestResponseType<ChargingPreferencesResponse>(ChargingPreferencesResponse::class)
    object CDR: HubRequestResponseType<ChargeDetailRecord>(ChargeDetailRecord::class)
    object CDR_ARRAY: HubRequestResponseType<Array<ChargeDetailRecord>>(Array<ChargeDetailRecord>::class)
    object TARIFF: HubRequestResponseType<Tariff>(Tariff::class)
    object TARIFF_ARRAY: HubRequestResponseType<Array<Tariff>>(Array<Tariff>::class)
    object TOKEN: HubRequestResponseType<Token>(Token::class)
    object TOKEN_ARRAY: HubRequestResponseType<Array<Token>>(Array<Token>::class)
    object AUTHORIZATION_INFO: HubRequestResponseType<AuthorizationInfo>(AuthorizationInfo::class)
    object COMMAND_RESPONSE: HubRequestResponseType<CommandResponse>(CommandResponse::class)
    object NOTHING: HubRequestResponseType<Nothing>(Nothing::class)
}

data class HubCommandsRequest(@JsonProperty("type") val type: CommandType,
                              @JsonProperty("headers") val headers: HubRequestHeaders,
                              @JsonProperty("body") val body: String)