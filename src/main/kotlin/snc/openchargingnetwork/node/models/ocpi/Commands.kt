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
                        @JsonProperty("connector_id") val connectorID: String? = null,
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