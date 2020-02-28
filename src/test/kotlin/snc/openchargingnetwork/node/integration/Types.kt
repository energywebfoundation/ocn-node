package snc.openchargingnetwork.node.integration

class JavalinException(val httpCode: Int = 200, val ocpiCode: Int = 2001, message: String): Exception(message)