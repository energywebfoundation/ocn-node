package snc.openchargingnetwork.client.models.ocpi

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Location(@JsonProperty("country_code") val countryCode: String,
                    @JsonProperty("party_id") val partyID: String,
                    @JsonProperty("id") val id: String,
                    @JsonProperty("type") val type: LocationType,
                    @JsonProperty("name") val name: String? = null,
                    @JsonProperty("address") val address: String,
                    @JsonProperty("city") val city: String,
                    @JsonProperty("postal_code") val postalCode: String? = null,
                    @JsonProperty("state") val state: String? = null,
                    @JsonProperty("country") val country: String,
                    @JsonProperty("coordinates") val coordinates: GeoLocation,
                    @JsonProperty("related_locations") val relatedLocations: Array<AdditionalGeoLocation>? = null,
                    @JsonProperty("evses") val evses: Array<Evse>? = null,
                    @JsonProperty("directions") val directions: Array<DisplayText>? = null,
                    @JsonProperty("operator") val operator: BusinessDetails? = null,
                    @JsonProperty("suboperator") val subOperator: BusinessDetails? = null,
                    @JsonProperty("owner") val owner: BusinessDetails? = null,
                    @JsonProperty("facilities") val facilities: Array<Facility>? = null,
                    @JsonProperty("time_zone") val timeZone: String? = null,
                    @JsonProperty("opening_times") val openingTimes: Hours? = null,
                    @JsonProperty("charging_when_closed") val chargingWhenClosed: String? = null,
                    @JsonProperty("images") val images: Array<Image>? = null,
                    @JsonProperty("energy_mix") val energyMix: EnergyMix? = null,
                    @JsonProperty("last_updated") val lastUpdated: String) {

    override fun equals(other: Any?): Boolean {
        return super.equals(other)
    }
    override fun hashCode(): Int {
        return super.hashCode()
    }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Evse(@JsonProperty("uid") val uid: String,
                @JsonProperty("evseId") val evseId: String? = null,
                @JsonProperty("status") val status: EvseStatus,
                @JsonProperty("status_schedule") val statusSchedule: Array<StatusSchedule>? = null,
                @JsonProperty("capabilities") val capabilities: Array<Capability>? = null,
                @JsonProperty("connectors") val connectors: Array<Connector>,
                @JsonProperty("floor_level") val floorLevel: String? = null,
                @JsonProperty("coordinates") val coordinates: GeoLocation? = null,
                @JsonProperty("physical_reference") val physicalReference: String? = null,
                @JsonProperty("directions") val directions: Array<DisplayText>? = null,
                @JsonProperty("parking_restrictions") val parkingRestrictions: Array<ParkingRestriction>? = null,
                @JsonProperty("images") val images: Array<Image>? = null,
                @JsonProperty("last_updated") val lastUpdated: String) {

    override fun equals(other: Any?): Boolean {
        return super.equals(other)
    }
    override fun hashCode(): Int {
        return super.hashCode()
    }

}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Connector(@JsonProperty("id") val id: String,
                     @JsonProperty("standard") val standard: ConnectorType,
                     @JsonProperty("format") val format: ConnectorFormat,
                     @JsonProperty("power_type") val powerType: PowerType,
                     @JsonProperty("max_voltage") val maxVoltage: Int,
                     @JsonProperty("max_amperage") val maxAmperage: Int,
                     @JsonProperty("max_electric_power") val maxElectricPower: Int? = null,
                     @JsonProperty("tariff_ids") val tariffIDs: Array<String>? = null,
                     @JsonProperty("terms_and_conditions") val termsAndConditions: String? = null,
                     @JsonProperty("last_updated") val lastUpdated: String) {

    override fun equals(other: Any?): Boolean {
        return super.equals(other)
    }
    override fun hashCode(): Int {
        return super.hashCode()
    }
}

data class GeoLocation(@JsonProperty("latitude") val latitude: String,
                       @JsonProperty("longitude") val longitude: String)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class AdditionalGeoLocation(@JsonProperty("latitude") val latitude: String,
                                 @JsonProperty("longitude") val longitude: String,
                                 @JsonProperty("name") val name: DisplayText? = null)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Hours(@JsonProperty("twentyfourseven") val twentyfourseven: Boolean,
                 @JsonProperty("regular_hours") val regularHours: RegularHours? = null,
                 @JsonProperty("exceptional_openings") val exceptionalOpenings: ExceptionalPeriod? = null,
                 @JsonProperty("exceptional_closings") val exceptionalClosings: ExceptionalPeriod? = null)

data class RegularHours(@JsonProperty("weekday") val weekday: Int,
                        @JsonProperty("period_begin") val periodBegin: String,
                        @JsonProperty("period_end") val periodEnd: String)

data class ExceptionalPeriod(@JsonProperty("period_begin") val periodBegin: String,
                             @JsonProperty("period_end") val periodEnd: String)

data class EnergyMix(@JsonProperty("is_green_energy") val isGreenEnergy: Boolean,
                     @JsonProperty("energy_sources") val energySources: Array<EnergySource>? = null,
                     @JsonProperty("environ_impact") val environImpact: Array<EnvironmentalImpact>? = null,
                     @JsonProperty("supplier_name") val supplierName: String? = null,
                     @JsonProperty("energy_product_name") val energyProductName: String? = null) {

    override fun equals(other: Any?): Boolean {
        return super.equals(other)
    }
    override fun hashCode(): Int {
        return super.hashCode()
    }
}

data class EnergySource(@JsonProperty("source") val source: EnergySourceCategory,
                        @JsonProperty("percentage") val percentage: Int)

data class EnvironmentalImpact(@JsonProperty("category") val category: EnvironmentalImpactCategory,
                               @JsonProperty("amount") val amount: Int)



@JsonInclude(JsonInclude.Include.NON_NULL)
data class StatusSchedule(@JsonProperty("period_begin") val periodBegin: String,
                          @JsonProperty("period_end") val periodEnd: String? = null,
                          @JsonProperty("status") val status: EvseStatus)

