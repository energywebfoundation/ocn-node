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

package snc.openchargingnetwork.node.services

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import snc.openchargingnetwork.node.models.*
import snc.openchargingnetwork.node.models.exceptions.OcpiServerUnusableApiException
import snc.openchargingnetwork.node.models.ocpi.*
import snc.openchargingnetwork.node.tools.urlJoin


@Service
class HttpService {

    val mapper = jacksonObjectMapper()

    fun convertToRequestVariables(stringBody: String): OcpiRequestVariables = mapper.readValue(stringBody)


    /**
     * Generic HTTP request expecting a response of type OcpiResponse<T> as defined by the caller
     */
    fun <T : Any> makeOcpiRequest(method: HttpMethod, url: String, headers: Map<String, String?>, params: Map<String, Any?>? = null, json: Map<String, Any>? = null): HttpResponse<T> {
        val paramsWithStringValues = params?.mapValues { (_, value) -> value.toString() } ?: mapOf()
        val response = when (method) {
            HttpMethod.GET -> khttp.get(url, headers, paramsWithStringValues)
            HttpMethod.POST -> khttp.post(url, headers, paramsWithStringValues, json = json)
            HttpMethod.PUT -> khttp.put(url, headers, paramsWithStringValues, json = json)
            HttpMethod.PATCH -> khttp.patch(url, headers, paramsWithStringValues, json = json)
            HttpMethod.DELETE -> khttp.delete(url, headers)
            else -> throw IllegalStateException("Invalid method: $method")
        }

        return HttpResponse(
                statusCode = response.statusCode,
                headers = response.headers,
                body = mapper.readValue(response.text))
    }


    /**
     * Generic HTTP request expecting a response of type OcpiResponse<T> as defined by the caller
     */
    final fun <T: Any> makeOcpiRequest(url: String,
                                       headers: OcnHeaders,
                                       requestVariables: OcpiRequestVariables): HttpResponse<T> {

        val headersMap = headers.toMap()

        var jsonBody: Map<String,Any>? = null
        if (requestVariables.body != null) {
            val jsonString = mapper.writeValueAsString(requestVariables.body)
            jsonBody = mapper.readValue(jsonString)
        }

        return makeOcpiRequest(
                method = requestVariables.method,
                url = url,
                headers = headersMap,
                params = requestVariables.urlEncodedParams,
                json = jsonBody)
    }


    /**
     * Get OCPI versions during the Credentials registration handshake
     */
    fun getVersions(url: String, authorization: String): List<Version> {
        try {
            val response = khttp.get(url = url, headers = mapOf("Authorization" to "Token $authorization"))
            val body: OcpiResponse<List<Version>> = mapper.readValue(response.text)

            return if (response.statusCode == 200 && body.statusCode == 1000) {
                body.data!!
            } else {
                throw Exception("Returned HTTP status code ${response.statusCode}; OCPI status code ${body.statusCode} - ${body.statusMessage}")
            }

        } catch (e: Exception) {
            throw OcpiServerUnusableApiException("Failed to request from $url: ${e.message}")
        }
    }


    /**
     * Get version details (using result of above getVersions request) during the Credentials registration handshake
     * Will provide OCN Node with modules implemented by OCPI platform and their endpoints
     */
    fun getVersionDetail(url: String, authorization: String): VersionDetail {
        try {
            val response = khttp.get(url = url, headers = mapOf("Authorization" to "Token $authorization"))
            val body: OcpiResponse<VersionDetail> = mapper.readValue(response.text)

            return if (response.statusCode == 200 && body.statusCode == 1000) {
                body.data!!
            } else {
                throw Exception("Returned HTTP status code ${response.statusCode}; OCPI status code ${body.statusCode} - ${body.statusMessage}")
            }

        } catch (e: Exception) {
            throw OcpiServerUnusableApiException("Failed to request v2.2 details from $url: ${e.message}")
        }
    }


    /**
     * Make a POST request to an OCN Node which implements /ocn/message
     * Used to forward requests to OCPI platforms of which the OCN Node does not share a local connection with
     */
    final fun <T: Any> postOcnMessage(url: String,
                                headers: OcnMessageHeaders,
                                body: String): HttpResponse<T> {

        val headersMap = headers.toMap()

        val fullURL = urlJoin(url, "/ocn/message")

        val response = khttp.post(fullURL, headersMap, data = body)

        return HttpResponse(
                statusCode = response.statusCode,
                headers = response.headers,
                body = mapper.readValue(response.text))
    }

}