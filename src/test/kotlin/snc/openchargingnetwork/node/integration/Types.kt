package snc.openchargingnetwork.node.integration

import snc.openchargingnetwork.node.integration.parties.CpoServer
import snc.openchargingnetwork.node.models.ocpi.BasicRole

class JavalinException(val httpCode: Int = 200, val ocpiCode: Int = 2001, message: String): Exception(message)

data class CpoTestCase(val party: BasicRole, val address: String, val operator: String, val server: CpoServer)