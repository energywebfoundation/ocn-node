package snc.connect.broker.tools

import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.UUID

fun generateUUIDv4Token(): String {
    return UUID.randomUUID().toString()
}

fun urlJoin(base: String, vararg paths: String): String {
    var uri = if (base.endsWith("/")) {
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
        uri = uri.plus(sanitizedPath)
    }
    return uri
}

fun getTimestamp(): String {
    return DateTimeFormatter.ISO_INSTANT.format(Instant.now())
}
