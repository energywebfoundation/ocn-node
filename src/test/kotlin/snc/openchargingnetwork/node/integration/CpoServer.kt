package snc.openchargingnetwork.node.integration

import io.javalin.Javalin
import org.web3j.crypto.Credentials as KeyPair
import snc.openchargingnetwork.node.models.ocpi.*
import snc.openchargingnetwork.node.tools.extractToken
import snc.openchargingnetwork.node.tools.generateUUIDv4Token

class CpoServer(private val party: BasicRole, private val port: Int) {

    private val app = Javalin.create().start(port)
    private val tokenB = generateUUIDv4Token()

    lateinit var tokenC: String
    lateinit var node: String

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

        app.get("/ocpi/versions/2.2") {
            it.json(OcpiResponse(
                    statusCode = 1000,
                    data = VersionDetail(version = "2.2", endpoints = listOf(
                            Endpoint(
                                    identifier = "credentials",
                                    role = InterfaceRole.RECEIVER,
                                    url = urlBuilder("/ocpi/cpo/2.2/credentials"))))
            ))
        }
    }

    fun setPartyInRegistry(registryAddress: String, credentials: KeyPair, operator: String) {
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

    private fun urlBuilder(path: String): String {
        return "http://localhost:$port$path"
    }

}