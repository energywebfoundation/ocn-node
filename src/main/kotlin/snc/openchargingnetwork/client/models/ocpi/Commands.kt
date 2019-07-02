package snc.openchargingnetwork.client.models.ocpi

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

abstract class AbstractCommand(val responseURL: String)

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