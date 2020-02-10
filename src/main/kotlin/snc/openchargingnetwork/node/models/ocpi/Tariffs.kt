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
import java.time.DayOfWeek

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Tariff(@JsonProperty("country_code") val countryCode: String,
                  @JsonProperty("party_id") val partyID: String,
                  @JsonProperty("id") val id: String,
                  @JsonProperty("currency") val currency: String,
                  @JsonProperty("type") val type: TariffType? = null,
                  @JsonProperty("tariff_alt_text") val tariffAltText: List<DisplayText>? = null,
                  @JsonProperty("tariff_alt_url") val tariffAltUrl: String? = null,
                  @JsonProperty("min_price") val minPrice: Price? = null,
                  @JsonProperty("max_price") val maxPrice: Price? = null,
                  @JsonProperty("elements") val elements: List<TariffElement>,
                  @JsonProperty("start_date_time") val startDateTime: String? = null,
                  @JsonProperty("end_date_time") val endDateTime: String? = null,
                  @JsonProperty("energy_mix") val energyMix: EnergyMix? = null,
                  @JsonProperty("last_updated") val lastUpdated: String)

enum class TariffType {
    AD_HOC_PAYMENT,
    PROFILE_CHEAP,
    PROFILE_FAST,
    PROFILE_GREEN,
    REGULAR
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class TariffElement(@JsonProperty("price_components") val priceComponents: List<PriceComponent>,
                         @JsonProperty("restrictions") val restrictions: TariffRestrictions? = null)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PriceComponent(@JsonProperty("type") val type: TariffDimensionType,
                          @JsonProperty("price") val price: Float,
                          @JsonProperty("vat") val vat: Float? = null,
                          @JsonProperty("step_size") val stepSize: Int)

enum class TariffDimensionType {
    ENERGY,
    FLAT,
    PARKING_TIME,
    TIME
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class TariffRestrictions(@JsonProperty("start_time") val startTime: String? = null,
                              @JsonProperty("end_time") val endTime: String? = null,
                              @JsonProperty("start_date") val startDate: String? = null,
                              @JsonProperty("end_date") val endDate: String? = null,
                              @JsonProperty("min_kwh") val minKwh: Float? = null,
                              @JsonProperty("max_kwh") val maxKwh: Float? = null,
                              @JsonProperty("min_current") val minCurrent: Float? = null,
                              @JsonProperty("max_current") val maxCurrent: Float? = null,
                              @JsonProperty("min_power") val minPower: Float? = null,
                              @JsonProperty("max_power") val maxPower: Float? = null,
                              @JsonProperty("min_duration") val minDuration: Int? = null,
                              @JsonProperty("max_duration") val maxDuration: Int? = null,
                              @JsonProperty("day_of_week") val dayOfWeek: List<DayOfWeek>? = null,
                              @JsonProperty("reservation") val reservation: ReservationRestrictionType? = null)

enum class ReservationRestrictionType {
    RESERVATION,
    RESERVATION_EXPIRES
}