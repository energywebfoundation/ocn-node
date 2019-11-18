/*
    Copyright 2019 Share&Charge Foundation

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

data class CancelReservation(@JsonProperty("response_url") var responseURL: String,
                             @JsonProperty("reservation_id") val reservationID: String)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CommandResponse(@JsonProperty("result") val result: CommandResponseType,
                           @JsonProperty("timeout") val timeout: Int,
                           @JsonProperty("message") val message: DisplayText? = null)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CommandResult(@JsonProperty("result") val result: CommandResultType,
                         @JsonProperty("message") val message: DisplayText? = null)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ReserveNow(@JsonProperty("response_url") var responseURL: String,
                      @JsonProperty("token") val token: Token,
                      @JsonProperty("expiry_date") val expiryDate: String,
                      @JsonProperty("reservation_id") val reservationID: String,
                      @JsonProperty("location_id") val locationID: String,
                      @JsonProperty("evse_uid") val evseUID: String? = null,
                      @JsonProperty("authorization_reference") val authorizationReference: String? = null)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class StartSession(@JsonProperty("response_url") var responseURL: String,
                        @JsonProperty("token") val token: Token,
                        @JsonProperty("location_id") val locationID: String,
                        @JsonProperty("evse_uid") val evseUID: String? = null,
                        @JsonProperty("authorization_reference") val authorizationReference: String? = null)

data class StopSession(@JsonProperty("response_url") var responseURL: String,
                       @JsonProperty("session_id") val sessionID: String)

data class UnlockConnector(@JsonProperty("response_url") var responseURL: String,
                           @JsonProperty("location_id") val locationID: String,
                           @JsonProperty("evse_uid") val evseUID: String,
                           @JsonProperty("connector_id") val connectorID: String)

enum class CommandResponseType {
    NOT_SUPPORTED,
    REJECTED,
    ACCEPTED,
    UNKNOWN_SESSION
}

enum class CommandResultType {
    ACCEPTED,
    CANCELED_RESERVATION,
    EVSE_OCCUPIED,
    EVSE_INOPERATIVE,
    FAILED,
    NOT_SUPPORTED,
    REJECTED,
    TIMEOUT,
    UNKNOWN_RESERVATION
}

enum class CommandType {
    CANCEL_RESERVATION,
    RESERVE_NOW,
    START_SESSION,
    STOP_SESSION,
    UNLOCK_CONNECTOR
}