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

import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Keys
import org.web3j.crypto.Sign
import org.web3j.utils.Numeric

/**
 * Wrapper replacing Web3j Sign.signPrefixedMessage with String input and output
 * @param message message to sign (i.e. hash of concatenated string values)
 * @param ecKeyPair Web3j crypto key pair
 * @return String - the signature as hex string containing concatenated r, s and v values
 */
fun signStringMessage(message: String, ecKeyPair: ECKeyPair): String {
    val signatureData = Sign.signPrefixedMessage(message.toByteArray(), ecKeyPair)
    val r = Numeric.cleanHexPrefix(Numeric.toHexString(signatureData.r))
    val s = Numeric.cleanHexPrefix(Numeric.toHexString(signatureData.s))
    val v = Numeric.cleanHexPrefix(Numeric.toHexString(signatureData.v))
    return "0x${r + s + v}"
}

/**
 * Wrapper for Web3j Sign.signedPrefixedMessageToKey which accepts a hex string signature and returns a 0x-prefixed
 * address.
 *
 * @param message the original message which was signed
 * @param signature the signature from the original message signing as hex string
 * @return the key which signed the message
 */
fun signerOfMessage(message: String, signature: String): String {
    val r = Numeric.hexStringToByteArray(signature.substring(2, 66))
    val s = Numeric.hexStringToByteArray(signature.substring(66, 130))
    val v = Numeric.hexStringToByteArray(signature.substring(130, 132))
    val signatureData = Sign.SignatureData(v, r, s)
    val signingKey = Sign.signedPrefixedMessageToKey(message.toByteArray(), signatureData)
    return "0x${Keys.getAddress(signingKey)}"
}

fun String.toChecksumAddress(): String {
    return Keys.getAddress(this)
}