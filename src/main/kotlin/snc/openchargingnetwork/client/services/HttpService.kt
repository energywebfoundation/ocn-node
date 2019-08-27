/*
    Copyright 2019 Energy Web Foundation

    This file is part of Open Charging Network Client.

    Open Charging Network Client is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Open Charging Network Client is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Open Charging Network Client.  If not, see <https://www.gnu.org/licenses/>.
*/

package snc.openchargingnetwork.client.services

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import snc.openchargingnetwork.client.models.*
import snc.openchargingnetwork.client.models.exceptions.OcpiServerUnusableApiException
import snc.openchargingnetwork.client.models.ocpi.*
import snc.openchargingnetwork.client.tools.urlJoin


@Service
class HttpService {

    val mapper = jacksonObjectMapper()

    fun convertToRequestVariables(stringBody: String): OcpiRequestVariables = mapper.readValue(stringBody)


    /**
     * Generic HTTP request expecting a response of type OcpiResponse<T> as defined by the caller
     */
    fun <T: Any> makeOcpiRequest(method: HttpMethod, url: String, headers: Map<String, String>, params: Map<String, String> = mapOf(), json: Map<String, Any>? = null): HttpResponse<T> {
        val response = when (method) {
            HttpMethod.GET -> khttp.get(url, headers, params)
            HttpMethod.POST -> khttp.post(url, headers, params, json = json)
            HttpMethod.PUT -> khttp.put(url, headers, params, json = json)
            HttpMethod.PATCH -> khttp.patch(url, headers, params, json = json)
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
    final inline fun <reified T: Any> makeOcpiRequest(url: String,
                                                      headers: OcpiRequestHeaders,
                                                      requestVariables: OcpiRequestVariables): HttpResponse<T> {

        val headersMap = headers.toMap()

        var jsonBody: Map<String,Any>? = null
        if (requestVariables.body != null) {
            val jsonString = mapper.writeValueAsString(requestVariables.body)
            jsonBody = mapper.readValue(jsonString)
        }

        var paramsMap: Map<String, String> = mapOf()
        if (requestVariables.urlEncodedParams != null) {
            paramsMap = requestVariables.urlEncodedParams.toMap()
        }

        return makeOcpiRequest(
                method = requestVariables.method,
                url = url,
                headers = headersMap,
                params = paramsMap,
                json = jsonBody)
    }


    /**
     * Get OCPI versions during the Credentials registration handshake
     */
    fun getVersions(url: String, authorization: String): Versions {
        try {
            val response = khttp.get(url = url, headers = mapOf("Authorization" to "Token $authorization"))
            val body: OcpiResponse<Versions> = mapper.readValue(response.text)

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
     * Will provide OCN Client with modules implemented by OCPI platform and their endpoints
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
     * Make a POST request to an OCN client which implements /ocn/message
     * Used to forward requests to OCPI platforms of which the OCN client does not share a local connection with
     */
    final inline fun <reified T: Any> postOcnMessage(url: String,
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