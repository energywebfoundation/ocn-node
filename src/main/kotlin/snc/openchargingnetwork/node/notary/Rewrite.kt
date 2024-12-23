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
package shareandcharge.openchargingnetwork.notary

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.jayway.jsonpath.ParseContext
import org.web3j.crypto.Hash

/**
 * A Rewrite object contains information about a previous Notary object.
 * @property rewrittenFields Key-Value pairs of JsonPaths with a corresponding value that was overwritten
 * @property hash the message hash of the overwritten message
 * @property rsv the concatenated signature of the overwritten message
 * @property signatory the signer of the overwritten message
 */
class Rewrite(val rewrittenFields: Map<String, Any?>,
              val hash: String,
              val rsv: String,
              val signatory: String) {

    /**
     * Verify a rewrite given a modified request.
     * @param fields array of json paths dictating the order of values signed
     * @param modifiedValues the newly modified request used to build the old request
     * @returns RewriteVerifyResult with message validity and optional error message / re-built message
     */
    fun verify(fields: List<String>, modifiedValues: ValuesToSign<*>, jsonPath: ParseContext, objectMapper: ObjectMapper): RewriteVerifyResult {
        val valuesAsJsonString = objectMapper.writeValueAsString(modifiedValues)
        val parser = jsonPath.parse(valuesAsJsonString)

        // 1. rebuild previous ValuesToSign that was stashed in this rewrite using rewritten fields map
        for ((key, value) in rewrittenFields.entries) {
            parser.set(key, value)
        }

        // 2. verify hash matches stashed hash
        var message = ""
        for (field in fields) {
            // only take the field's value if not null
            parser.read<Any?>(field.lowercase())?.let { message += it }
        }

        if (hash != Hash.sha3String(message)) {
            return RewriteVerifyResult(false, "stashed values do not match original.")
        }

        // 3. verify signatory matches stashed signatory
        val expectedSignatory = signerOfMessage(hash, rsv)
        if (signatory.toChecksumAddress() != expectedSignatory.toChecksumAddress()) {
            return RewriteVerifyResult(false, "signatory of stashed rewrite incorrect.")
        }

        val originalValues: ValuesToSign<*> = objectMapper.readValue(parser.jsonString())
        return RewriteVerifyResult(true, previousValues = originalValues)
    }

}