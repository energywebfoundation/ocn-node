package snc.connect.broker.services

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import khttp.get
import org.springframework.stereotype.Service
import snc.connect.broker.models.exceptions.OcpiServerUnusableApiException
import snc.connect.broker.models.ocpi.VersionDetail
import snc.connect.broker.models.ocpi.Versions

@Service
class HttpRequestService {

    fun getVersions(url: String, authorization: String): Versions {
        try {
            val response = get(
                    url = url,
                    headers = mapOf("Authorization" to "Token $authorization")
            )
            return jacksonObjectMapper().readValue(response.text)
        } catch (e: Exception) {
            throw OcpiServerUnusableApiException("Failed to request versions from $url: ${e.message}")
        }
    }

    fun getVersionDetail(url: String, authorization: String): VersionDetail {
        try {
            val response = get(
                    url = url,
                    headers = mapOf("Authorization" to "Token $authorization")
            )
            return jacksonObjectMapper().readValue(response.text)
        } catch (e: Exception) {
            throw OcpiServerUnusableApiException("Failed to request v2.2 details from $url: ${e.message}")
        }
    }

}