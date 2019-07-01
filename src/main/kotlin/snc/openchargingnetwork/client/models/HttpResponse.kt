package snc.openchargingnetwork.client.models

import snc.openchargingnetwork.client.models.ocpi.OcpiResponse

data class HttpResponse<T>(val statusCode: Int,
                        val headers: Map<String, String>,
                        val body: OcpiResponse<T>)