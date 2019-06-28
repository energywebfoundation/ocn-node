package snc.openchargingnetwork.client.tools

fun String.extractToken() = split(" ").last()

fun MutableMap<String, Any?>.asUrlEncodedParameters(): Map<String, String>? {
    val map = mutableMapOf<String, String>()
    for ((key, value) in this) {
        if (value != null) {
            map[key] = value.toString()
        }
    }
    return map
}