package snc.openchargingnetwork.node.models.ocpi

import com.fasterxml.jackson.annotation.JsonProperty

data class GenericChargingProfileResult(val result: ChargingProfileResultType,
                                        val profile: ActiveChargingProfile? = null)

enum class ChargingProfileResultType {
    ACCEPTED,
    REJECTED,
    UNKNOWN
}

data class ActiveChargingProfile(@JsonProperty("start_date_time") val startDateTime: String,
                                 @JsonProperty("charging_profile") val chargingProfile: ChargingProfile)

data class ChargingProfile(@JsonProperty("start_date_time") val startDateTime: String? = null,
                           val duration: Int? = null,
                           @JsonProperty("charging_rate_unit") val chargingRateUnit: ChargingRateUnit,
                           @JsonProperty("min_charging_rate") val minChargingRate: Float? = null,
                           @JsonProperty("charging_profile_period") val chargingProfilePeriod: List<ChargingProfilePeriod>? = null)

enum class ChargingRateUnit {
    W,
    A
}

data class ChargingProfilePeriod(@JsonProperty("start_period") val startPeriod: Int,
                                 val limit: Float)

data class ChargingProfileResponse(val result: ChargingProfileResponseType,
                                   val timeout: Int)

enum class ChargingProfileResponseType {
    ACCEPTED,
    NOT_SUPPORTED,
    REJECTED,
    TOO_OFTEN,
    UNKNOWN_SESSION
}

data class SetChargingProfile(@JsonProperty("charging_profile") val chargingProfile: ChargingProfile,
                              @JsonProperty("response_url") val responseUrl: String)
