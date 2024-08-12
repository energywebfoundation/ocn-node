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

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Location(@JsonProperty("country_code") val countryCode: String,
                    @JsonProperty("party_id") val partyID: String,
                    @JsonProperty("id") val id: String,
                    @JsonProperty("publish") val publish: Boolean,
                    @JsonProperty("publish_allowed_to") val publishAllowedTo: List<PublishTokenType>? = null,
                    @JsonProperty("name") val name: String? = null,
                    @JsonProperty("address") val address: String,
                    @JsonProperty("city") val city: String,
                    @JsonProperty("postal_code") val postalCode: String? = null,
                    @JsonProperty("state") val state: String? = null,
                    @JsonProperty("country") val country: String,
                    @JsonProperty("coordinates") val coordinates: GeoLocation,
                    @JsonProperty("related_locations") val relatedLocations: List<AdditionalGeoLocation>? = null,
                    @JsonProperty("parking_type") val parkingType: ParkingType? = null,
                    @JsonProperty("evses") val evses: List<Evse>? = null,
                    @JsonProperty("directions") val directions: List<DisplayText>? = null,
                    @JsonProperty("operator") val operator: BusinessDetails? = null,
                    @JsonProperty("suboperator") val subOperator: BusinessDetails? = null,
                    @JsonProperty("owner") val owner: BusinessDetails? = null,
                    @JsonProperty("facilities") val facilities: List<Facility>? = null,
                    @JsonProperty("time_zone") val timeZone: String? = null,
                    @JsonProperty("opening_times") val openingTimes: Hours? = null,
                    @JsonProperty("charging_when_closed") val chargingWhenClosed: String? = null,
                    @JsonProperty("images") val images: List<Image>? = null,
                    @JsonProperty("energy_mix") val energyMix: EnergyMix? = null,
                    @JsonProperty("last_updated") val lastUpdated: String)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Evse(@JsonProperty("uid") val uid: String,
                @JsonProperty("evse_id") val evseId: String? = null,
                @JsonProperty("status") val status: EvseStatus,
                @JsonProperty("status_schedule") val statusSchedule: List<StatusSchedule>? = null,
                @JsonProperty("capabilities") val capabilities: List<Capability>? = null,
                @JsonProperty("connectors") val connectors: List<Connector>,
                @JsonProperty("floor_level") val floorLevel: String? = null,
                @JsonProperty("coordinates") val coordinates: GeoLocation? = null,
                @JsonProperty("physical_reference") val physicalReference: String? = null,
                @JsonProperty("directions") val directions: List<DisplayText>? = null,
                @JsonProperty("parking_restrictions") val parkingRestrictions: List<ParkingRestriction>? = null,
                @JsonProperty("images") val images: List<Image>? = null,
                @JsonProperty("last_updated") val lastUpdated: String)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Connector(@JsonProperty("id") val id: String,
                     @JsonProperty("standard") val standard: ConnectorType,
                     @JsonProperty("format") val format: ConnectorFormat,
                     @JsonProperty("power_type") val powerType: PowerType,
                     @JsonProperty("max_voltage") val maxVoltage: Int,
                     @JsonProperty("max_amperage") val maxAmperage: Int,
                     @JsonProperty("max_electric_power") val maxElectricPower: Int? = null,
                     @JsonProperty("tariff_ids") val tariffIDs: List<String>? = null,
                     @JsonProperty("terms_and_conditions") val termsAndConditions: String? = null,
                     @JsonProperty("last_updated") val lastUpdated: String)

data class GeoLocation(@JsonProperty("latitude") val latitude: String,
                       @JsonProperty("longitude") val longitude: String)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class AdditionalGeoLocation(@JsonProperty("latitude") val latitude: String,
                                 @JsonProperty("longitude") val longitude: String,
                                 @JsonProperty("name") val name: DisplayText? = null)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Hours(@JsonProperty("twentyfourseven") val twentyfourseven: Boolean,
                 @JsonProperty("regular_hours") val regularHours: List<RegularHours>? = null,
                 @JsonProperty("exceptional_openings") val exceptionalOpenings: List<ExceptionalPeriod>? = null,
                 @JsonProperty("exceptional_closings") val exceptionalClosings: List<ExceptionalPeriod>? = null)

data class RegularHours(@JsonProperty("weekday") val weekday: Int,
                        @JsonProperty("period_begin") val periodBegin: String,
                        @JsonProperty("period_end") val periodEnd: String)

data class ExceptionalPeriod(@JsonProperty("period_begin") val periodBegin: String,
                             @JsonProperty("period_end") val periodEnd: String)

data class EnergyMix(@JsonProperty("is_green_energy") val isGreenEnergy: Boolean,
                     @JsonProperty("energy_sources") val energySources: List<EnergySource>? = null,
                     @JsonProperty("environ_impact") val environImpact: List<EnvironmentalImpact>? = null,
                     @JsonProperty("supplier_name") val supplierName: String? = null,
                     @JsonProperty("energy_product_name") val energyProductName: String? = null)

data class EnergySource(@JsonProperty("source") val source: EnergySourceCategory,
                        @JsonProperty("percentage") val percentage: Float)

data class EnvironmentalImpact(@JsonProperty("category") val category: EnvironmentalImpactCategory,
                               @JsonProperty("amount") val amount: Float)



@JsonInclude(JsonInclude.Include.NON_NULL)
data class StatusSchedule(@JsonProperty("period_begin") val periodBegin: String,
                          @JsonProperty("period_end") val periodEnd: String? = null,
                          @JsonProperty("status") val status: EvseStatus)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PublishTokenType(@JsonProperty("uid") val uid: String? = null,
                            @JsonProperty("type") val type: TokenType? = null,
                            @JsonProperty("visual_number") val visualNumber: String? = null,
                            @JsonProperty("issuer") val issuer: String? = null,
                            @JsonProperty("group_id ") val groupID: String? = null)


enum class LocationType {
    ON_STREET,
    PARKING_GARAGE,
    UNDERGROUND_GARAGE,
    PARKING_LOT,
    OTHER,
    UNKNOWN
}

enum class Facility {
    HOTEL,
    RESTAURANT,
    CAFE,
    MALL,
    SUPERMARKET,
    SPORT,
    RECREATION_AREA,
    NATURE,
    MUSEUM,
    BIKE_SHARING,
    BUS_STOP,
    TAXI_STAND,
    TRAM_STOP,
    METRO_STATION,
    TRAIN_STATION,
    AIRPORT,
    PARKING_LOT,
    CARPOOL_PARKING,
    FUEL_STATION,
    WIFI
}

enum class EnergySourceCategory {
    NUCLEAR,
    GENERAL_FOSSIL,
    COAL,
    GAS,
    GENERAL_GREEN,
    SOLAR,
    WIND,
    WATER
}

enum class EnvironmentalImpactCategory {
    NUCLEAR_WASTE,
    CARBON_DIOXIDE
}

enum class EvseStatus {
    AVAILABLE,
    BLOCKED,
    CHARGING,
    INOPERATIVE,
    OUTOFORDER,
    PLANNED,
    REMOVED,
    RESERVED,
    UNKNOWN
}

enum class Capability {
    CHARGING_PROFILE_CAPABLE,
    CHARGING_PREFERENCES_CAPABLE,
    CHIP_CARD_SUPPORT,
    CONTACTLESS_CARD_SUPPORT,
    CREDIT_CARD_PAYABLE,
    DEBIT_CARD_PAYABLE,
    PED_TERMINAL,
    REMOTE_START_STOP_CAPABLE,
    RESERVABLE,
    RFID_READER,
    TOKEN_GROUP_CAPABLE,
    UNLOCK_CAPABLE
}

enum class ParkingRestriction {
    EV_ONLY,
    PLUGGED,
    DISABLED,
    CUSTOMERS,
    MOTORCYCLES
}

enum class ConnectorType {
    CHADEMO,
    DOMESTIC_A,
    DOMESTIC_B,
    DOMESTIC_C,
    DOMESTIC_D,
    DOMESTIC_E,
    DOMESTIC_F,
    DOMESTIC_G,
    DOMESTIC_H,
    DOMESTIC_I,
    DOMESTIC_J,
    DOMESTIC_K,
    DOMESTIC_L,
    IEC_60309_2_single_16,
    IEC_60309_2_three_16,
    IEC_60309_2_three_32,
    IEC_60309_2_three_64,
    IEC_62196_T1,
    IEC_62196_T1_COMBO,
    IEC_62196_T2,
    IEC_62196_T2_COMBO,
    IEC_62196_T3A,
    IEC_62196_T3C,
    PANTOGRAPH_BOTTOM_UP,
    PANTOGRAPH_TOP_DOWN,
    TESLA_R,
    TESLA_S
}

enum class ConnectorFormat {
    SOCKET,
    CABLE
}

enum class PowerType {
    AC_1_PHASE,
    AC_3_PHASE,
    DC
}

enum class ParkingType {
    ALONG_MOTORWAY,
    PARKING_GARAGE,
    PARKING_LOT,
    ON_DRIVEWAY,
    ON_STREET,
    UNDERGROUND_GARAGE
}
