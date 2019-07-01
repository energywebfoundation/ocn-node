package snc.openchargingnetwork.client.tools

fun String.extractToken() = split(" ").last()