package snc.openchargingnetwork.client.services

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import khttp.*
import org.springframework.stereotype.Service
import snc.openchargingnetwork.client.models.HttpResponse
import snc.openchargingnetwork.client.models.exceptions.OcpiServerUnusableApiException
import snc.openchargingnetwork.client.models.ocpi.OcpiResponse
import snc.openchargingnetwork.client.models.ocpi.VersionDetail
import snc.openchargingnetwork.client.models.ocpi.Versions
import kotlin.reflect.KClass

@Service
class HttpRequestService {

    val mapper = jacksonObjectMapper()


    fun <In: Any?, Out: Any> makeRequest(method: String,
                                         url: String,
                                         headers: Map<String, String>,
                                         params: Map<String, String>? = null,
                                         body: In? = null,
                                         expectedDataType: KClass<Out>): HttpResponse<Out> {

        val response = when (method) {
            "GET" -> get(url = url, headers = headers, params = params ?: mapOf())
            "POST" -> post(url = url, headers = headers, json = body, params = params ?: mapOf())
            "PUT" -> put(url = url, headers = headers, json = body)
            "PATCH" -> patch(url = url, headers = headers, json = body)
            "DELETE" -> delete(url = url, headers = headers, json = body)
            else -> throw IllegalStateException("Invalid method: $method")
        }

        val type = mapper.typeFactory.constructParametricType(OcpiResponse::class.java, expectedDataType.java)
        return HttpResponse(
                statusCode = response.statusCode,
                headers = response.headers,
                body = mapper.readValue(response.text, type))
    }

    fun getVersions(url: String, authorization: String): Versions {
        try {

            val response = this.makeRequest("GET", url, mapOf("Authorization" to "Token $authorization"), body = null, expectedDataType = Versions::class)

            return if (response.statusCode == 200 && response.body.statusCode == 1000) {
                response.body.data!!
            } else {
                throw Exception("Returned HTTP status code ${response.statusCode}; OCPI status code ${response.body.statusCode} - ${response.body.statusMessage}")
            }

        } catch (e: Exception) {
            throw OcpiServerUnusableApiException("Failed to request from $url: ${e.message}")
        }
    }

    fun getVersionDetail(url: String, authorization: String): VersionDetail {
        try {
            val response = this.makeRequest("GET", url, mapOf("Authorization" to "Token $authorization"), body = null, expectedDataType = VersionDetail::class)

            return if (response.statusCode == 200 && response.body.statusCode == 1000) {
                response.body.data!!
            } else {
                throw Exception("Returned HTTP status code ${response.statusCode}; OCPI status code ${response.body.statusCode} - ${response.body.statusMessage}")
            }

        } catch (e: Exception) {
            throw OcpiServerUnusableApiException("Failed to request v2.2 details from $url: ${e.message}")
        }
    }

}