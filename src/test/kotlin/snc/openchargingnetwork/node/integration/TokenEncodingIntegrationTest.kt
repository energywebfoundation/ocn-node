package snc.openchargingnetwork.node.integration

import com.olisystems.ocnregistryv2_0.OcnRegistry
import io.javalin.Javalin
import khttp.responses.Response
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.web3j.crypto.Credentials
import org.web3j.tx.ClientTransactionManager
import shareandcharge.openchargingnetwork.notary.Notary
import shareandcharge.openchargingnetwork.notary.SignableHeaders
import shareandcharge.openchargingnetwork.notary.ValuesToSign
import snc.openchargingnetwork.node.integration.parties.CpoServer
import snc.openchargingnetwork.node.integration.utils.*
import snc.openchargingnetwork.node.models.entities.*
import snc.openchargingnetwork.node.models.ocpi.*
import snc.openchargingnetwork.node.repositories.EndpointRepository
import snc.openchargingnetwork.node.repositories.PlatformRepository
import snc.openchargingnetwork.node.repositories.RoleRepository
import snc.openchargingnetwork.node.tools.generateUUIDv4Token
import java.math.BigInteger

class TokenEncodingIntegrationTest {

    private lateinit var networkComponents: NetworkComponents
    private lateinit var node: OcnNode
    private lateinit var cpo: CpoServer

    private lateinit var platformRepo: PlatformRepository
    private lateinit var roleRepo: RoleRepository
    private lateinit var endpointRepo: EndpointRepository

    object LegacyMsp {
        private const val port = 8050
        private val credentials = Credentials.create(
            "0x82d052c865f5763aad42add438569276c00d3d88a2d062d36b2bae914d58b8c8"
        )

        val platform = PlatformEntity(
            // auth tokens stored in ascii, not bs64 encoded
            auth = Auth(tokenB = generateUUIDv4Token(), tokenC = generateUUIDv4Token()),
            status = ConnectionStatus.CONNECTED,
            versionsUrl = "http://localhost:$port")
        var role = RoleEntity(
            platformID = 0L,
            countryCode = "TR",
            partyID = "ABC",
            role = Role.EMSP,
            businessDetails = BusinessDetails(name = "abc industries"))
        var endpoint = EndpointEntity(
                platformID = 0L,
                url = "http://localhost:$port/ocpi/cdrs",
                identifier = "cdrs",
                role = InterfaceRole.RECEIVER)

        private val app = Javalin.create().start(port)

        init {
            app.exception(JavalinException::class.java) { e, ctx ->
                ctx.status(e.httpCode).json(OcpiResponse<Unit>(statusCode = e.ocpiCode, statusMessage = e.message))
            }
            app.before {
                if (it.header("Authorization") != "Token ${platform.auth.tokenB!!}") {
                    throw JavalinException(message = "Unauthorized")
                }
            }
            app.get("/ocpi/versions") {
                it.json(OcpiResponse(
                    statusCode = 1000,
                    data = listOf(Version(version = "2.2", url = "http://localhost:$port/ocpi/versions/2.2"))
                ))
            }
            app.get("/ocpi/versions/2.2") {
                it.json(OcpiResponse(
                    statusCode = 1000,
                    data = VersionDetail(
                        version = "2.2",
                        endpoints = listOf(Endpoint(endpoint.identifier, endpoint.role, endpoint.url)))
                ))
            }
            app.post("/ocpi/cdrs") {
                val body = OcpiResponse<Unit>(statusCode = 1000)
                val notary = Notary().sign(ValuesToSign(body = body), credentials.privateKey())
                it.json(body.copy(signature = notary.serialize()))
            }
        }

        fun register(contracts: OcnContracts, node: NodeDefinition) {
            // register in smart contract; already "registered" on ocn node
            val registry = OcnRegistry.load(
                contracts.registry.contractAddress,
                web3,
                ClientTransactionManager(web3, credentials.address),
                gasProvider)

            val call = registry.setParty(
                role.countryCode.toByteArray(),
                role.partyID.toByteArray(),
                listOf(BigInteger.ZERO),
                node.credentials.address,
                "name", "url"
            )
            call.sendAsync().get()
        }

        fun sendLocationsRequest(node: NodeDefinition, to: BasicRole): Response {
            val headers = SignableHeaders(
                correlationId = "0",
                fromCountryCode = role.countryCode,
                fromPartyId = role.partyID,
                toCountryCode = to.country,
                toPartyId = to.id
            )
            val notary = Notary().sign(ValuesToSign<Unit>(headers = headers), credentials.privateKey())
            return khttp.get(
                url = "http://localhost:${node.port}/ocpi/sender/2.2/locations",
                headers = headers.toMap(platform.auth.tokenC!!, notary.serialize())
            )
        }

        fun stop(): Javalin = app.stop()
    }

    @BeforeAll
    fun beforeAll() {
        networkComponents = setupNetwork(HubClientInfoParams())
        cpo = networkComponents.cpos[0].server
        node = networkComponents.nodes[0]

        platformRepo = node.appContext.getBean(PlatformRepository::class.java)
        roleRepo = node.appContext.getBean(RoleRepository::class.java)
        endpointRepo = node.appContext.getBean(EndpointRepository::class.java)

        // enter platform/party/endpoints in respective repositories
        val platform = platformRepo.save(LegacyMsp.platform)

        LegacyMsp.role.platformID = platform.id!!
        LegacyMsp.endpoint.platformID = platform.id!!
        roleRepo.save(LegacyMsp.role)
        endpointRepo.save(LegacyMsp.endpoint)

        // enter in ocn registry
        LegacyMsp.register(networkComponents.contracts, node.definition)
    }

    @AfterAll
    fun afterAll() {
        stopPartyServers(networkComponents)
        LegacyMsp.stop()
    }

    @Test
    fun legacy_auth_incoming() {
        // tests that a previously registered party can still use their ascii token
        // (no encoding) to make requests to the OCN
        val response = LegacyMsp.sendLocationsRequest(node.definition, cpo.config.party)
        assertThat(response.statusCode).isEqualTo(200)
        assertThat(response.jsonObject.get("status_code")).isEqualTo(1000)
    }

    @Test
    fun legacy_auth_outgoing() {
        // tests that a previously registered party can still receive requests from the
        // OCN (no encoding)
        val response = cpo.sendCdr(to = BasicRole(
            id = LegacyMsp.role.partyID,
            country = LegacyMsp.role.countryCode))
        assertThat(response.statusCode).isEqualTo(200)
        assertThat(response.jsonObject.get("status_code")).isEqualTo(1000)
    }

}