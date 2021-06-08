package snc.openchargingnetwork.node.integration.utils

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.web3j.crypto.Credentials
import shareandcharge.openchargingnetwork.notary.SignableHeaders
import snc.openchargingnetwork.node.models.ocpi.BasicRole
import snc.openchargingnetwork.node.tools.generateUUIDv4Token

val objectMapper = jacksonObjectMapper()

fun getTokenA(node: String, parties: List<BasicRole>): String {
    val response = khttp.post("$node/admin/generate-registration-token",
            headers = mapOf("Authorization" to "Token randomkey"),
            json = coerceToJson(parties))
    return response.jsonObject.getString("token")
}

fun coerceToJson(obj: Any): Any {
    return objectMapper.readValue(objectMapper.writeValueAsString(obj))
}

fun Credentials.privateKey(): String {
    return ecKeyPair.privateKey.toString(16)
}

fun SignableHeaders.toMap(tokenC: String, signature: String): MutableMap<String, String> {
    val map = mutableMapOf<String, String>()
    map["Authorization"] = "Token $tokenC"
    map["OCN-Signature"] = signature
    map["X-Request-ID"] = generateUUIDv4Token()
    correlationId?.let { map["X-Correlation-ID"] = it }
    fromCountryCode?.let { map["OCPI-From-Country-Code"] = it }
    fromPartyId?.let { map["OCPI-From-Party-Id"] = it }
    toCountryCode?.let { map["OCPI-To-Country-Code"] = it }
    toPartyId?.let { map["OCPI-To-Party-Id"] = it }
    return map
}


