package snc.openchargingnetwork.node.integration.utils

import org.springframework.boot.builder.SpringApplicationBuilder
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import org.web3j.tx.ClientTransactionManager
import org.web3j.tx.gas.StaticGasProvider
import snc.openchargingnetwork.contracts.Registry
import snc.openchargingnetwork.node.Application
import snc.openchargingnetwork.node.integration.parties.CpoServer
import snc.openchargingnetwork.node.integration.parties.MspServer
import snc.openchargingnetwork.node.models.ocpi.BasicRole

const val provider = "http://localhost:8544"

val cpoDefinitions: List<PartyDefinition> = listOf(
    PartyDefinition(
        nodeNumber = 1,
        port = 8100,
        party = BasicRole("CPA", "CH"),
        credentials = Credentials.create("0xc88b703fb08cbea894b6aeff5a544fb92e78a18e19814cd85da83b71f772aa6c")),
    PartyDefinition(
        nodeNumber = 2,
        port = 8101,
        party = BasicRole("CPB", "CH"),
        credentials = Credentials.create("0x388c684f0ba1ef5017716adb5d21a053ea8e90277d0868337519f97bede61418")))

val mspDefinitions: List<PartyDefinition> = listOf(
    PartyDefinition(
        nodeNumber = 1,
        port = 8200,
        party = BasicRole("MSP", "DE"),
        credentials = Credentials.create("0x659cbb0e2411a44db63778987b1e22153c086a95eb6b18bdf89de078917abc63")))

fun getNodeDefinitions(hubClientInfoParams: HubClientInfoParams): List<NodeDefinition> {
    return listOf(
        NodeDefinition(
            nodeNumber = 1,
            port = 8080, // http://localhost:8080
            signatures = true,
            hubClientInfoParams = hubClientInfoParams,
            credentials = Credentials.create("0xae6ae8e5ccbfb04590405997ee2d52d2b330726137b875053c36d94e974d162f")),
        NodeDefinition(
            nodeNumber = 2,
            port = 8081, // http://localhost:8081
            signatures = true,
            hubClientInfoParams = hubClientInfoParams,
            credentials = Credentials.create("0x0dbbe8e4ae425a6d2687f1a7e3ba17bc98c673636790f1b8ad91193c05875ef1")))
}

/**
 * Sets up the registry and test parties for integration tests
 */
fun setupNetwork(hubClientInfoParams: HubClientInfoParams): NetworkComponents {
    val registry = deployRegistry()

    val nodeDefinitions = getNodeDefinitions(hubClientInfoParams)
    val nodes = nodeDefinitions.map {
        nodeDefinition -> setUpNode(nodeDefinition, registry)
    }

    val cpos = cpoDefinitions.map {
        cpoDefinition -> setUpCpo(cpoDefinition, nodeDefinitions.first { d -> d.nodeNumber == cpoDefinition.nodeNumber }, registry)
    }

    val msps = mspDefinitions.map {
        mspDefinition -> setUpMsp(mspDefinition, nodeDefinitions.first { d -> d.nodeNumber == mspDefinition.nodeNumber }, registry)
    }

    return NetworkComponents(cpos, msps, nodes, registry)
}

/**
 * Adds a CPO to the test OCN Network
 */
fun setUpCpo(definition: PartyDefinition, nodeDefinition: NodeDefinition, registry: Registry): TestCpo {
    val cpoServer = CpoServer(definition.credentials, definition.party, definition.port)
    cpoServer.setPartyInRegistry(registry.contractAddress, nodeDefinition.credentials.address)
    cpoServer.registerCredentials()
    return TestCpo(
        definition = definition,
        operator = nodeDefinition.credentials.address,
        server = cpoServer
    )
}

/**
 * Adds an MSP to the test OCN Network
 */
fun setUpMsp(definition: PartyDefinition, nodeDefinition: NodeDefinition, registry: Registry): TestMsp {
    val mspServer = MspServer(definition.credentials, definition.party, definition.port)
    mspServer.setPartyInRegistry(registry.contractAddress, nodeDefinition.credentials.address)
    mspServer.registerCredentials()
    return TestMsp(
            definition = definition,
            server = mspServer
    )
}

/**
 * Adds a node to the test OCN Network
 */
fun setUpNode(definition: NodeDefinition, registry: Registry): OcnNode {
    val domain = "http://localhost:${definition.port}"
    val registryAddress = registry.contractAddress
    val appContext = SpringApplicationBuilder(Application::class.java)
            .addCommandLineProperties(true)
            .run("--server.port=${definition.port}",
                    "--spring.datasource.url=jdbc:h2:mem:testdb-${definition.nodeNumber}", // use different in-mem db for each node
                    "--ocn.node.url=$domain",
                    "--ocn.node.privatekey=${definition.credentials.ecKeyPair.privateKey.toString(16)}",
                    "--ocn.node.web3.provider=$provider",
                    "--ocn.node.web3.contracts.registry=$registryAddress",
                    "--ocn.node.signatures=${definition.signatures}",
                    "--ocn.node.stillAliveEnabled=${definition.hubClientInfoParams.stillAlive.enabled}",
                    "--ocn.node.stillAliveRate=${definition.hubClientInfoParams.stillAlive.rate}",
                    "--ocn.node.plannedPartySearchEnabled=${definition.hubClientInfoParams.plannedPartySearch.enabled}",
                    "--ocn.node.plannedPartySearchRate=${definition.hubClientInfoParams.plannedPartySearch.rate}")
    getRegistryInstance(definition.credentials, registryAddress).setNode(domain).sendAsync().get()
    return OcnNode(definition, appContext)
}

/**
 * Stops the party servers and OCN nodes of the provided list
 */
fun stopPartyServers(components: NetworkComponents) {
    for (cpo in components.cpos) {
        cpo.server.stopServer()
    }
    for (msp in components.msps) {
        msp.server.stopServer()
    }
    for (node in components.nodes) {
        node.appContext.close()
    }
}

/**
 * Deploys and gets instance
 */
fun deployRegistry(): Registry {
    val web3 = Web3j.build(HttpService(provider))
    val txManager = ClientTransactionManager(web3, "0x627306090abaB3A6e1400e9345bC60c78a8BEf57")
    val gasProvider = StaticGasProvider(0.toBigInteger(), 10000000.toBigInteger())
    return Registry.deploy(web3, txManager, gasProvider).sendAsync().get()
}

/**
 * Gets deployed instance
 */
fun getRegistryInstance(credentials: Credentials, contractAddress: String): Registry {
    val web3 = Web3j.build(HttpService(provider))
    val txManager = ClientTransactionManager(web3, credentials.address)
    val gasProvider = StaticGasProvider(0.toBigInteger(), 10000000.toBigInteger())
    return Registry.load(contractAddress, web3, txManager, gasProvider)
}
