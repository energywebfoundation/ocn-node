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

package snc.openchargingnetwork.node.models

import shareandcharge.openchargingnetwork.notary.OcpiHeaders
import shareandcharge.openchargingnetwork.notary.OcpiRequest
import snc.openchargingnetwork.node.models.ocpi.OcpiResponse

data class HttpResponse<T: Any>(val statusCode: Int,
                                val headers: Map<String, String>,
                                val body: OcpiResponse<T>) {
    fun toSignedValues(): OcpiRequest<OcpiResponse<T>> {
        return OcpiRequest(
                headers = OcpiHeaders(
                        limit = headers["X-Limit"] ?: headers["x-limit"],
                        totalCount = headers["X-Total-Count"] ?: headers["x-total-count"],
                        link = headers["Link"] ?: headers["link"],
                        location = headers["Location"] ?: headers["location"]
                ),
                body = body)
    }
}