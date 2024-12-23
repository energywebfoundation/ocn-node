package snc.openchargingnetwork.node.integration.parties

import com.olisystems.ocnregistryv2_0.OcnRegistry
import io.javalin.Javalin
import org.web3j.tx.ClientTransactionManager
import shareandcharge.openchargingnetwork.notary.Notary
import shareandcharge.openchargingnetwork.notary.SignableHeaders
import shareandcharge.openchargingnetwork.notary.ValuesToSign
import snc.openchargingnetwork.node.integration.utils.*
import snc.openchargingnetwork.node.models.OcnServicePermission
import snc.openchargingnetwork.node.models.OcnRulesListType
import snc.openchargingnetwork.node.models.ocpi.*
import snc.openchargingnetwork.node.tools.generateUUIDv4Token
import snc.openchargingnetwork.node.tools.toBs64String

open class PartyServer(val config: PartyDefinition, deployedContracts: OcnContracts) {

    val app: Javalin = Javalin.create().start(config.port)
    private val tokenB: String = generateUUIDv4Token()
    lateinit var tokenC: String
    lateinit var node: String

    val hubClientInfoStatuses = mutableMapOf<BasicRole, ConnectionStatus>() // store received hubclientinfo updates

    val messageStore = mutableListOf<ReceivedMessage>() // store any received messages

    // replace deployed contract instances with own party's transaction manager
    private val txManager = ClientTransactionManager(web3, config.credentials.address)
    val contracts = deployedContracts.copy(
            registry = OcnRegistry.load(deployedContracts.registry.contractAddress, web3, txManager, gasProvider))

    init {
        app.exception(JavalinException::class.java) { e, ctx ->
            ctx.status(e.httpCode).json(OcpiResponse<Unit>(statusCode = e.ocpiCode, statusMessage = e.message))
        }

        app.before {
            if (it.header("Authorization") != "Token ${tokenB.toBs64String()}") {
                throw JavalinException(message = "Unauthorized")
            }
        }

        app.get("/ocpi/versions") {
            it.json(OcpiResponse(
                    statusCode = 1000,
                    data = listOf(Version(version = "2.2", url = urlBuilder("/ocpi/versions/2.2")))
            ))
        }

        app.put("ocpi/2.2/clientinfo/:countryCode/:partyID") {
            this.hubClientInfoStatuses[BasicRole(id = it.pathParam("partyID"), country = it.pathParam("countryCode"))] = it.body<ClientInfo>().status
            val body = OcpiResponse(statusCode = 1000, data = "")
            body.signature = sign(body = body)
            it.json(body)
        }
    }

    fun sign(headers: SignableHeaders? = null, params: Map<String, Any?>? = null, body: Any? = null): String {
        val valuesToSign = ValuesToSign(headers, params, body)
        return Notary().sign(valuesToSign, config.credentials.privateKey()).serialize()
    }

    fun setPartyInRegistry(operator: String, role: Role, name: String, url: String) {
        val (id, country) = config.party
        val rolesList = listOf(role.ordinal.toBigInteger())
        contracts.registry.setParty(country.toByteArray(), id.toByteArray(), rolesList, operator, name, url).sendAsync().get()
        node = contracts.registry.getNode(operator).sendAsync().get()
    }


    fun registerCredentials() {
        // TODO: could also request versions and store endpoints in memory
        val tokenA = getTokenA(node, listOf(config.party)).toBs64String()

        val response = khttp.post("$node/ocpi/2.2/credentials",
                headers = mapOf("Authorization" to "Token $tokenA"),
                json = coerceToJson(Credentials(
                        token = tokenB,
                        url = urlBuilder("/ocpi/versions"),
                        roles = listOf(CredentialsRole(
                                role = Role.CPO,
                                businessDetails = BusinessDetails(name = "Some CPO"),
                                countryCode = config.party.country,
                                partyID = config.party.id)))))

        tokenC = response
            .jsonObject
            .getJSONObject("data")
            .getString("token")
            .let { it.toBs64String() }
    }

    fun deleteCredentials() {
        khttp.delete("$node/ocpi/2.2/credentials",
                headers = mapOf("Authorization" to "Token $tokenC"))
    }

    fun urlBuilder(path: String): String {
        return "http://localhost:${config.port}$path"
    }

    fun getSignableHeaders(to: BasicRole): SignableHeaders {
        return SignableHeaders(
                correlationId = generateUUIDv4Token(),
                fromCountryCode = config.party.country,
                fromPartyId = config.party.id,
                toCountryCode = to.country,
                toPartyId = to.id)
    }

    fun addToList(type: OcnRulesListType, party: BasicRole, modules: List<String>? = listOf()) {
        khttp.post("$node/ocpi/receiver/2.2/ocnrules/${type.toString().toLowerCase()}",
                headers = mapOf("Authorization" to "Token $tokenC"),
                json = mapOf("country_code" to party.country, "party_id" to party.id, "modules" to modules))
    }

    fun stopServer() {
        hubClientInfoStatuses.clear()
        app.stop()
    }

}