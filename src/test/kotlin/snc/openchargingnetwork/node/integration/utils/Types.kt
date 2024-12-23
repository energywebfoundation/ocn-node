package snc.openchargingnetwork.node.integration.utils

import com.olisystems.ocnregistryv2_0.OcnRegistry
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.http.HttpMethod
import org.web3j.crypto.Credentials
import snc.openchargingnetwork.node.integration.parties.CpoServer
import snc.openchargingnetwork.node.integration.parties.MspServer
import snc.openchargingnetwork.node.models.ocpi.BasicRole
import snc.openchargingnetwork.node.models.ocpi.InterfaceRole
import snc.openchargingnetwork.node.models.ocpi.ModuleID

class JavalinException(val httpCode: Int = 200, val ocpiCode: Int = 2001, message: String): Exception(message)

data class PartyDefinition(val nodeNumber: Int, val port: Int, val party: BasicRole, val credentials: Credentials)

data class NodeDefinition(val nodeNumber: Int, val port: Int, val signatures: Boolean, val hubClientInfoParams: HubClientInfoParams, val credentials: Credentials)

data class OcnNode(val definition: NodeDefinition, val appContext: ConfigurableApplicationContext)

data class TestCpo(private val definition: PartyDefinition, val server: CpoServer, val operator: String) {
    val party = this.definition.party
    val address: String = this.definition.credentials.address
}

data class TestMsp(private val definition: PartyDefinition, val server: MspServer) {
    val party = this.definition.party
    val address: String = this.definition.credentials.address
}

data class OcnContracts(var registry: OcnRegistry)

data class NetworkComponents(val cpos: List<TestCpo>, val msps: List<TestMsp>, val nodes: List<OcnNode>, val contracts: OcnContracts)

data class Scheduler(val enabled: Boolean, val rate: Long = 2000)

data class HubClientInfoParams(val stillAlive: Scheduler = Scheduler(false),
                               val plannedPartySearch: Scheduler = Scheduler(false))

data class ReceivedMessage(val module: ModuleID, val interfaceRole: InterfaceRole, val method: HttpMethod, val sender: BasicRole)