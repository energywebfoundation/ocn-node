package snc.openchargingnetwork.node.integration

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.boot.builder.SpringApplicationBuilder
import org.web3j.crypto.Credentials
import org.web3j.crypto.Keys
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import org.web3j.tx.ClientTransactionManager
import org.web3j.tx.gas.StaticGasProvider
import snc.openchargingnetwork.contracts.Registry
import snc.openchargingnetwork.node.Application
import snc.openchargingnetwork.node.models.ocpi.BasicRole

const val provider = "http://localhost:8544"
val objectMapper = jacksonObjectMapper()

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

fun setUpNode(registryAddress: String, credentials: Credentials, port: Int) {
    val domain = "http://localhost:$port"
    SpringApplicationBuilder(Application::class.java)
            .addCommandLineProperties(true)
            .run("--server.port=$port",
                    "--ocn.node.url=$domain",
                    "--ocn.node.privatekey=${credentials.ecKeyPair.privateKey.toString(16)}",
                    "--ocn.node.web3.provider=$provider",
                    "--ocn.node.web3.contracts.registry=$registryAddress")
    getRegistryInstance(credentials, registryAddress).setNode(domain).sendAsync().get()
}

fun getTokenA(node: String, parties: List<BasicRole>): String {
    val response = khttp.post("$node/admin/generate-registration-token",
            headers = mapOf("Authorization" to "Token randomkey"),
            json = coerceToJson(parties))
    return response.jsonObject.getString("token")
}

fun coerceToJson(obj: Any): Any {
    return objectMapper.readValue(objectMapper.writeValueAsString(obj))
}

fun String.checksum(): String {
    return Keys.toChecksumAddress(this)
}