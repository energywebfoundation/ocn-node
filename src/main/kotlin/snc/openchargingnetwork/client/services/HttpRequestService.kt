package snc.openchargingnetwork.client.services

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import khttp.*
import org.springframework.stereotype.Service
import snc.openchargingnetwork.client.models.ocpi.OcpiStatus
import snc.openchargingnetwork.client.models.exceptions.OcpiServerUnusableApiException
import snc.openchargingnetwork.client.models.ocpi.OcpiResponse
import snc.openchargingnetwork.client.models.ocpi.VersionDetail
import snc.openchargingnetwork.client.models.ocpi.Versions
import kotlin.reflect.KClass

@Service
class HttpRequestService {

    val mapper = jacksonObjectMapper()

    fun <Out: Any> makeGetRequest(url: String, headers: Map<String, String>, params: Map<String, String>? = null, expectedDataType: KClass<Out>): OcpiResponse<Out> {
        val response = get(
                url = url,
                headers = headers,
                params = params ?: mapOf())
        return if (response.statusCode == 200) {
            val type = mapper.typeFactory.constructParametricType(OcpiResponse::class.java, expectedDataType.java)
            mapper.readValue(response.text, type)
        } else {
            throw Exception("Returned HTTP status code ${response.statusCode}: ${response.text}")
        }
    }

    fun <In: Any, Out: Any> makePostRequest(url: String, headers: Map<String, String>, body: In? = null, expectedDataType: KClass<Out>): OcpiResponse<Out> {
        val response = post(
                url = url,
                headers = headers,
                json = body)
        return if (response.statusCode == 200) {
            val type = mapper.typeFactory.constructParametricType(OcpiResponse::class.java, expectedDataType.java)
            mapper.readValue(response.text, type)
        } else {
            throw Exception("Returned HTTP status code ${response.statusCode}: ${response.text}")
        }
    }

    fun <In: Any, Out: Any> makePutRequest(url: String, headers: Map<String, String>, body: In? = null, expectedDataType: KClass<Out>): OcpiResponse<Out> {
        val response = put(
                url = url,
                headers = headers,
                json = body)
        return if (response.statusCode == 200) {
            val type = mapper.typeFactory.constructParametricType(OcpiResponse::class.java, expectedDataType.java)
            mapper.readValue(response.text, type)
        } else {
            throw Exception("Returned HTTP status code ${response.statusCode}: ${response.text}")
        }
    }

    fun <In: Any, Out: Any> makePatchRequest(url: String, headers: Map<String, String>, body: In? = null, expectedDataType: KClass<Out>): OcpiResponse<Out> {
        val response = patch(
                url = url,
                headers = headers,
                json = body)
        return if (response.statusCode == 200) {
            val type = mapper.typeFactory.constructParametricType(OcpiResponse::class.java, expectedDataType.java)
            mapper.readValue(response.text, type)
        } else {
            throw Exception("Returned HTTP status code ${response.statusCode}: ${response.text}")
        }
    }

    fun <In: Any, Out: Any> makeDeleteRequest(url: String, headers: Map<String, String>, body: In? = null, expectedDataType: KClass<Out>): OcpiResponse<Out> {
        val response = delete(
                url = url,
                headers = headers,
                json = body)
        return if (response.statusCode == 200) {
            val type = mapper.typeFactory.constructParametricType(OcpiResponse::class.java, expectedDataType.java)
            mapper.readValue(response.text, type)
        } else {
            throw Exception("Returned HTTP status code ${response.statusCode}: ${response.text}")
        }
    }

    fun getVersions(url: String, authorization: String): Versions {
        try {

            val ocpiResponse = this.makeGetRequest(url, mapOf("Authorization" to "Token $authorization"), expectedDataType = Versions::class)

            return if (ocpiResponse.statusCode == OcpiStatus.SUCCESS.code) {
                ocpiResponse.data!!
            } else {
                throw Exception("Returned OCPI status code ${ocpiResponse.statusCode}")
            }

        } catch (e: Exception) {
            throw OcpiServerUnusableApiException("Failed to request from $url: ${e.message}")
        }
    }

    fun getVersionDetail(url: String, authorization: String): VersionDetail {
        try {
            val ocpiResponse = this.makeGetRequest(url, mapOf("Authorization" to "Token $authorization"), expectedDataType = VersionDetail::class)

            return if (ocpiResponse.statusCode == OcpiStatus.SUCCESS.code) {
                ocpiResponse.data!!
            } else {
                throw Exception("Returned OCPI status code ${ocpiResponse.statusCode}")
            }

        } catch (e: Exception) {
            throw OcpiServerUnusableApiException("Failed to request v2.2 details from $url: ${e.message}")
        }
    }

}