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
data class CDR(@JsonProperty("country_code") val countryCode: String,
               @JsonProperty("party_id") val partyID: String,
               @JsonProperty("id") val id: String,
               @JsonProperty("start_date_time") val startDateTime: String,
               @JsonProperty("end_date_time") val endDateTime: String,
               @JsonProperty("session_id") val sessionID: String? = null,
               @JsonProperty("cdr_token") val cdrToken: CdrToken,
               @JsonProperty("auth_method") val authMethod: AuthMethod,
               @JsonProperty("authorization_reference") val authorizationReference: String? = null,
               @JsonProperty("cdr_location") val cdrLocation: CdrLocation,
               @JsonProperty("meter_id") val meterID: String? = null,
               @JsonProperty("currency") val currency: String,
               @JsonProperty("tariffs") val tariffs: List<Tariff>? = null,
               @JsonProperty("charging_periods") val chargingPeriods: List<ChargingPeriod>,
               @JsonProperty("signed_data") val signedData: SignedData? = null,
               @JsonProperty("total_cost") val totalCost: Price,
               @JsonProperty("total_fixed_cost") val totalFixedCost: Price? = null,
               @JsonProperty("total_energy") val totalEnergy: Float,
               @JsonProperty("total_energy_cost") val totalEnergyCost: Price? = null,
               @JsonProperty("total_time") val totalTime: Float,
               @JsonProperty("total_time_cost") val totalTimeCost: Price? = null,
               @JsonProperty("total_parking_time") val totalParkingTime: Float? = null,
               @JsonProperty("total_parking_cost") val totalParkingCost: Price? = null,
               @JsonProperty("total_reservation_cost") val totalReservationCost: Price? = null,
               @JsonProperty("remark") val remark: String? = null,
               @JsonProperty("invoice_reference_id") val invoiceReferenceID: String? = null,
               @JsonProperty("credit") val credit: Boolean? = null,
               @JsonProperty("credit_reference_id") val creditReferenceID: String? = null,
               @JsonProperty("last_updated") val lastUpdated: String)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CdrLocation(@JsonProperty("id") val id: String,
                       @JsonProperty("name") val name: String? = null,
                       @JsonProperty("address") val address: String,
                       @JsonProperty("city") val city: String,
                       @JsonProperty("postal_code") val postalCode: String,
                       @JsonProperty("country") val countryCode: String,
                       @JsonProperty("coordinates") val coordinates: GeoLocation,
                       @JsonProperty("evse_uid") val evseUID: String,
                       @JsonProperty("evse_id") val evseID: String,
                       @JsonProperty("connector_id") val connectorID: String,
                       @JsonProperty("connector_standard") val connectorStandard: ConnectorType,
                       @JsonProperty("connector_format") val connectorFormat: ConnectorFormat,
                       @JsonProperty("connector_power_type") val connectorPowerType: PowerType)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class SignedData(@JsonProperty("encoding_method") val encodingMethod: String,
                      @JsonProperty("encoding_method_version") val encodingMethodVersion: Int? = null,
                      @JsonProperty("public_key") val publicKey: String? = null,
                      @JsonProperty("signed_values") val signedValues: List<SignedValue>,
                      @JsonProperty("url") val url: String)

data class SignedValue(@JsonProperty("nature") val nature: String,
                       @JsonProperty("plain_data") val plainData: String,
                       @JsonProperty("signed_data") val signedData: String)