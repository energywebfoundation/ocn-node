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
import khttp.*
import khttp.responses.Response
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import snc.openchargingnetwork.client.models.*
import snc.openchargingnetwork.client.models.exceptions.OcpiServerUnusableApiException
import snc.openchargingnetwork.client.models.ocpi.*
import snc.openchargingnetwork.client.tools.urlJoin
import kotlin.reflect.KClass

@Service
class HttpService {

    val mapper = jacksonObjectMapper()


    /**
     * Generic HTTP request expecting a response of type OcpiResponse<T> as defined by the caller
     */
    fun <T: Any> makeOcpiRequest(request: () -> Response): HttpResponse<T> {
        val response = request()
//        val type = mapper.typeFactory.constructParametricType(OcpiResponse::class.java, responseType::class.java)
//        val body: OcpiResponse<T> = mapper.readValue(response.text)
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

        val headersMap = headers.encode()

        var jsonBody: Map<String,Any>? = null
        if (requestVariables.body != null) {
            val jsonString = mapper.writeValueAsString(requestVariables.body)
            jsonBody = mapper.readValue(jsonString)
        }

        var paramsMap: Map<String, String> = mapOf()
        if (requestVariables.urlEncodedParams != null) {
            paramsMap = requestVariables.urlEncodedParams.encode()
        }

        return when (requestVariables.method) {
            HttpMethod.GET -> makeOcpiRequest { get(url = url, headers = headersMap, params = paramsMap) }
            HttpMethod.POST -> makeOcpiRequest { post(url = url, headers = headersMap, json = jsonBody, params = paramsMap) }
            HttpMethod.PUT -> makeOcpiRequest { put(url = url, headers = headersMap, json = jsonBody) }
            HttpMethod.PATCH -> makeOcpiRequest { patch(url = url, headers = headersMap, json = jsonBody) }
            HttpMethod.DELETE -> makeOcpiRequest { delete(url = url, headers = headersMap, json = jsonBody) }
            else -> throw IllegalStateException("Invalid method: ${requestVariables.method}")
        }
    }


    /**
     * Get OCPI versions during the Credentials registration handshake
     */
    fun getVersions(url: String, authorization: String): Versions {
        try {
            val response = get(url = url, headers = mapOf("Authorization" to "Token $authorization"))
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
            val response = get(url = url, headers = mapOf("Authorization" to "Token $authorization"))
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
                                body: OcpiRequestVariables): HttpResponse<T> {

        val headersMap = headers.encode()

        val jsonString = mapper.writeValueAsString(body)
        val jsonBody: Map<String,Any> = mapper.readValue(jsonString)

        val fullURL = urlJoin(url, "/ocn/message")

        return makeOcpiRequest { post(url = fullURL, headers = headersMap, json = jsonBody) }
    }

}