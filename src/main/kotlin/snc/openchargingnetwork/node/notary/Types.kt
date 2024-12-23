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
package shareandcharge.openchargingnetwork.notary

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Possible OCPI request/response headers
 */ 
data class SignableHeaders(@JsonProperty("x-correlation-id")        var correlationId: String? = null,
                           @JsonProperty("ocpi-from-country-code")  var fromCountryCode: String? = null,
                           @JsonProperty("ocpi-from-party-id")      var fromPartyId: String? = null,
                           @JsonProperty("ocpi-to-country-code")    var toCountryCode: String? = null,
                           @JsonProperty("ocpi-to-party-id")        var toPartyId: String? = null,
                           @JsonProperty("x-limit")                 var limit: String? = null,
                           @JsonProperty("x-total-count")           var totalCount: String? = null,
                           @JsonProperty("link")                    var link: String? = null,
                           @JsonProperty("location")                var location: String? = null)

/**
 * ValuesToSign provides the values which should be signed.
 * @property headers OCPI request headers
 * @property params optional url-encoded parameters
 * @property body generic body for the appropriate OCPI request
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ValuesToSign<T>(var headers: SignableHeaders? = null,
                           var params: Map<String, Any?>? = null,
                           var body: T? = null)

/**
 * Result of the verify method.
 * @property isValid passed verification
 * @property error optional error message if verification not successful
 */
data class VerifyResult(val isValid: Boolean, val error: String? = null)

/**
 * Result of the Rewrite verify method.
 * @property isValid passed verification
 * @property error optional error message if verification not successful
 * @property previousValues optional re-built ValuesToSign if verification successful
 */
data class RewriteVerifyResult(val isValid: Boolean,
                               val error: String? = null,
                               val previousValues: ValuesToSign<*>? = null)