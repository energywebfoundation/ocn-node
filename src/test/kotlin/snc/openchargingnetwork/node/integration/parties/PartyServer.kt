package snc.openchargingnetwork.node.integration.parties

import io.javalin.Javalin
import shareandcharge.openchargingnetwork.notary.Notary
import org.web3j.crypto.Credentials as KeyPair
import shareandcharge.openchargingnetwork.notary.SignableHeaders
import shareandcharge.openchargingnetwork.notary.ValuesToSign
import snc.openchargingnetwork.node.integration.utils.*
import snc.openchargingnetwork.node.models.OcnRulesListType
import snc.openchargingnetwork.node.models.ocpi.*
import snc.openchargingnetwork.node.tools.generateUUIDv4Token

open class PartyServer(private val credentials: KeyPair, private val party: BasicRole, private val port: Int) {

    val app: Javalin = Javalin.create().start(port)
    private val tokenB: String = generateUUIDv4Token()
    lateinit var tokenC: String
    lateinit var node: String

    val hubClientInfoStatuses = mutableMapOf<BasicRole, ConnectionStatus>()

    init {
        app.exception(JavalinException::class.java) { e, ctx ->
            ctx.status(e.httpCode).json(OcpiResponse<Unit>(statusCode = e.ocpiCode, statusMessage = e.message))
        }

        app.before {
            if (it.header("Authorization") != "Token $tokenB") {
                throw JavalinException(message = "Unauthorized")
            }
        }

        app.get("/ocpi/versions") {
            it.json(OcpiResponse(
                    statusCode = 1000,
                    data = listOf(Version(version = "2.2", url = urlBuilder("/ocpi/versions/2.2")))
            ))
        }

        app.put("ocpi/cpo/2.2/clientinfo/:countryCode/:partyID") {
            this.hubClientInfoStatuses[BasicRole(id = it.pathParam("partyID"), country = it.pathParam("countryCode"))] = it.body<ClientInfo>().status
            val body = OcpiResponse(statusCode = 1000, data = "")
            body.signature = Notary().sign(ValuesToSign(body = body), credentials.privateKey()).serialize()
            it.json(body)
        }
    }

    fun setPartyInRegistry(registryAddress: String, operator: String) {
        val registry = getRegistryInstance(credentials, registryAddress)
        registry.setParty(party.country.toByteArray(), party.id.toByteArray(), listOf(0.toBigInteger()), operator).sendAsync().get()
        node = registry.getNode(operator).sendAsync().get()
    }

    fun registerCredentials() {
        val tokenA = getTokenA(node, listOf(party))
        val response = khttp.post("$node/ocpi/2.2/credentials",
                headers = mapOf("Authorization" to "Token $tokenA"),
                json = coerceToJson(Credentials(
                        token = tokenB,
                        url = urlBuilder("/ocpi/versions"),
                        roles = listOf(CredentialsRole(
                                role = Role.CPO,
                                businessDetails = BusinessDetails(name = "Some CPO"),
                                countryCode = party.country,
                                partyID = party.id)))))
        tokenC = response.jsonObject.getJSONObject("data").getString("token")
    }

    fun deleteCredentials() {
        khttp.delete("$node/ocpi/2.2/credentials",
                headers = mapOf("Authorization" to "Token $tokenC"))
    }

    fun urlBuilder(path: String): String {
        return "http://localhost:$port$path"
    }

    fun getSignableHeaders(to: BasicRole): SignableHeaders {
        return SignableHeaders(
                correlationId = generateUUIDv4Token(),
                fromCountryCode = party.country,
                fromPartyId = party.id,
                toCountryCode = to.country,
                toPartyId = to.id)
    }

    fun addToList(type: OcnRulesListType, party: BasicRole) {
        val typeString = when (type) {
            OcnRulesListType.WHITELIST -> "whitelist"
            OcnRulesListType.BLACKLIST -> "blacklist"
        }
        khttp.post("$node/ocpi/receiver/2.2/ocnrules/$typeString",
                headers = mapOf("Authorization" to "Token $tokenC"),
                json = mapOf("country_code" to party.country, "party_id" to party.id, "modules" to listOf<String>()))
    }

    fun stopServer() {
        hubClientInfoStatuses.clear()
        app.stop()
    }

}