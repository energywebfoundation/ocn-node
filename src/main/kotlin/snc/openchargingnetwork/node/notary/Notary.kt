package shareandcharge.openchargingnetwork.notary

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.web3j.crypto.Credentials
import org.web3j.crypto.Hash
import java.util.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.Option

/**
 * The Open Charging Network Notary signs OCPI requests and verifies OCN Signatures.
 */
class Notary {

    var fields: MutableList<String> = mutableListOf()
    var hash: String = ""
    var rsv: String = ""
    var signatory: String = ""
    var rewrites: MutableList<Rewrite> = mutableListOf()

    companion object {
        private val objectMapper = jacksonObjectMapper()
        private val jsonPath = JsonPath.using(Configuration.defaultConfiguration().addOptions(Option.SUPPRESS_EXCEPTIONS))

        /**
         * Compress signature using GZIP
         */
        @JvmStatic
        fun compress(input: ByteArray): ByteArray {
            val byteArrayOutputStream = ByteArrayOutputStream()
            GZIPOutputStream(byteArrayOutputStream).use { it.write(input) }
            return byteArrayOutputStream.toByteArray()
        }

        /**
         * Decompress signature using GZIP
         */
        @JvmStatic
        fun decompress(input: ByteArray): ByteArray {
            val byteArrayInputStream = ByteArrayInputStream(input)
            return GZIPInputStream(byteArrayInputStream).readBytes()
        }

        @JvmStatic
        fun deserialize(ocnSignature: String): Notary {
            val decoded = Base64.getDecoder().decode(ocnSignature)
            val decompressed = decompress(decoded)
            return jacksonObjectMapper().readValue(decompressed)
        }
    }

    fun serialize(): String {
        val serialized = objectMapper.writeValueAsBytes(this)
        val compressed = compress(serialized)
        return Base64.getEncoder().encodeToString(compressed)
    }

    fun sign(valuesToSign: ValuesToSign<*>, privateKey: String): Notary {
        val credentials = Credentials.create(privateKey)
        fields = mutableListOf()
        val message = walk("$", valuesToSign)
        hash = Hash.sha3String(message)
        rsv = signStringMessage(hash, credentials.ecKeyPair)
        signatory = credentials.address
        return this
    }

    fun verify(valuesToVerify: ValuesToSign<*>): VerifyResult {
        val valuesAsJsonString = objectMapper.writeValueAsString(valuesToVerify)
        val parser = jsonPath.parse(valuesAsJsonString)

        var message = ""
        fields.forEach { message += parser.read(it.lowercase()) }

        if (hash != Hash.sha3String(message)) {
            return VerifyResult(false, "Request has been modified.")
        }

        if (signatory.toChecksumAddress() != signerOfMessage(hash, rsv).toChecksumAddress()) {
            return VerifyResult(false, "Signatories do not match.")
        }

        var nextValues = valuesToVerify.copy()
        for ((index, rewrite) in rewrites.reversed().withIndex()) {
            val (isValid, error, previousValues) = rewrite.verify(fields, nextValues, jsonPath, objectMapper)
            if (!isValid) {
                return VerifyResult(isValid, "Rewrite $index: $error")
            }
            if (previousValues == null) {
                throw IllegalStateException("Rewrite $index: Previous values missing in rewrite verification")
            }
            nextValues = previousValues.copy()
        }

        return VerifyResult(true)
    }

    fun stash(rewrittenFields: Map<String, Any?>): Notary {
        val rewrite = Rewrite(rewrittenFields, hash, rsv, signatory)
        rewrites.add(rewrite)
        return this
    }

    private fun walk(jsonPath: String, value: Any?, message: String = ""): String {
        var mutableMsg = message

        fun walkThroughListLike(value: List<*>) {
            for ((index, subValue) in value.withIndex()) {
                mutableMsg = walk("$jsonPath[$index]", subValue, mutableMsg)
            }
        }

        if (value != null && value != "") {
            when (value) {
                is Array<*> -> walkThroughListLike(value.toList())
                is Set<*> -> walkThroughListLike(value.toList())
                is List<*> -> walkThroughListLike(value)
                is Map<*, *> -> {
                    for ((key, subValue) in value.entries) {
                        mutableMsg = walk("$jsonPath['$key']", subValue, mutableMsg)
                    }
                }
                is String, is Boolean, is Int, is Byte, is Short, is Long, is Float, is Double, is Char -> {
                    fields.add(jsonPath)
                    mutableMsg += value
                }
                else -> {
                    val valueAsJsonString = objectMapper.writeValueAsString(value)
                    val valueAsMap: Map<String, Any?> = objectMapper.readValue(valueAsJsonString)
                    mutableMsg = walk(jsonPath, valueAsMap, mutableMsg)
                }
            }
        }
        return mutableMsg
    }
}