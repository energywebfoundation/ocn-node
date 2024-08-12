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

package snc.openchargingnetwork.node.tools

import org.web3j.crypto.Keys
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*

val bs64Encoder: Base64.Encoder = Base64.getEncoder()
val bs64Decoder: Base64.Decoder = Base64.getDecoder()

fun generateUUIDv4Token(): String {
    return UUID.randomUUID().toString()
}

fun urlJoin(base: String, vararg paths: String?): String {
    var url = if (base.endsWith("/")) {
        base.dropLast(1)
    } else {
        base
    }
    for (path in paths) {
        if (path == null) {
            continue
        }
        val sanitizedPath: String = if (path.startsWith("/") && !path.endsWith("/")) {
            path
        } else if (path.startsWith("/") && path.endsWith("/")) {
            path.dropLast(1)
        } else if (!path.startsWith("/") && path.endsWith("/")) {
            "/${path.dropLast(1)}"
        } else {
            "/$path"
        }
        url += (sanitizedPath)
    }
    return url
}

fun getTimestamp(): String {
    return DateTimeFormatter.ISO_INSTANT.format(Instant.now())
}

fun getTimestamp(instant: Instant): String {
    return DateTimeFormatter.ISO_INSTANT.format(instant)
}

fun getInstant(timeStamp: String): Instant {
    return Instant.parse(timeStamp)
}

fun generatePrivateKey(): String {
    val keys = Keys.createEcKeyPair()
    return keys.privateKey.toString(16)
}
