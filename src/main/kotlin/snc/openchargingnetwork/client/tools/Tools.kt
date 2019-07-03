package snc.openchargingnetwork.client.tools

import org.web3j.crypto.Keys
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.UUID

fun generateUUIDv4Token(): String {
    return UUID.randomUUID().toString()
}

fun urlJoin(base: String, vararg paths: String): String {
    var url = if (base.endsWith("/")) {
        base.dropLast(1)
    } else {
        base
    }
    for (path in paths) {
        val sanitizedPath: String = if (path.startsWith("/") && !path.endsWith("/")) {
            path
        } else if (path.startsWith("/") && path.endsWith("/")) {
            path.dropLast(1)
        } else if (!path.startsWith("/") && path.endsWith("/")) {
            "/${path.dropLast(1)}"
        } else {
            "/$path"
        }
        url = url.plus(sanitizedPath)
    }
    return url
}

fun getTimestamp(): String {
    return DateTimeFormatter.ISO_INSTANT.format(Instant.now())
}

fun generatePrivateKey(): String {
    val keys = Keys.createEcKeyPair()
    return keys.privateKey.toString(16)
}