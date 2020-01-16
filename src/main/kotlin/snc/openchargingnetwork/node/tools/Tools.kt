/*
    Copyright 2019 Share&Charge Foundation

    This file is part of Open Charging Network Node.

    Open Charging Network Node is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Open Charging Network Node is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Open Charging Network Node.  If not, see <https://www.gnu.org/licenses/>.
*/

package snc.openchargingnetwork.node.tools

import org.web3j.crypto.Keys
import snc.openchargingnetwork.node.models.HttpResponse
import snc.openchargingnetwork.node.models.ocpi.CommandResponse
import snc.openchargingnetwork.node.models.ocpi.CommandResponseType
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.UUID

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

fun generatePrivateKey(): String {
    val keys = Keys.createEcKeyPair()
    return keys.privateKey.toString(16)
}

fun <T: Any> isOcpiSuccess(response: HttpResponse<T>): Boolean {
    return response.statusCode == 200 && response.body.statusCode == 1000
}