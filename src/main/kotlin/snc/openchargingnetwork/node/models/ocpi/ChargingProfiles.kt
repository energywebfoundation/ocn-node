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
