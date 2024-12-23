package snc.openchargingnetwork.node.services

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.olisystems.ocnregistryv2_0.OcnRegistry
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.web3j.crypto.Credentials
import org.web3j.crypto.Sign
import org.web3j.tuples.generated.Tuple2
import snc.openchargingnetwork.node.models.exceptions.OcpiHubConnectionProblemException
import snc.openchargingnetwork.node.models.ocpi.*
import snc.openchargingnetwork.node.tools.generatePrivateKey
import snc.openchargingnetwork.node.config.NodeProperties
import snc.openchargingnetwork.node.models.OcnHeaders
import snc.openchargingnetwork.node.tools.getTimestamp
import java.math.BigInteger

class WalletServiceTest {

    private val privateKey = "71eb44ccb94e00b6d462232618085daeea239daf76804acf040a7f037549598f"
    private val address = "0xF686dd2b1Cbf4c77079Ca48D176e157ddB744eeF"

    private val body = OcpiRequestVariables(
            method = HttpMethod.GET,
            module = ModuleID.SESSIONS,
            interfaceRole = InterfaceRole.RECEIVER,
            headers = OcnHeaders(
                    authorization = "Token token-c",
                    requestID = "1",
                    correlationID = "1",
                    sender = BasicRole("XXX", "DE"),
                    receiver = BasicRole("AAA", "DE")))

    private val properties: NodeProperties = mockk()
    private val registry: OcnRegistry = mockk()
    private val httpService: HttpService = mockk()

    private val walletService: WalletService

    init {
        walletService = WalletService(properties, registry, httpService)
        every { properties.privateKey } returns privateKey
    }


    @Test
    fun toByteArray() {
        val sig = "0x9955af11969a2d2a7f860cb00e6a00cfa7c581f5df2dbe8ea16700b33f4b4b9" +
                "b69f945012f7ea7d3febf11eb1b78e1adc2d1c14c2cf48b25000938cc1860c83e01"
        val (r, s, v) = walletService.signatureStringToByteArray(sig)
        assertThat(r.size).isEqualTo(32)
        assertThat(s.size).isEqualTo(32)
        assertThat(v.size).isEqualTo(1)
        Sign.SignatureData(v, r, s)
    }


    @Test
    fun sign() {
        val jsonStringBody = jacksonObjectMapper().writeValueAsString(body)
        val sig = walletService.sign(jsonStringBody)
        assertThat(sig.length).isEqualTo(130)
    }

    @Test
    fun `verifyRequest silently succeeds`() {
        val jsonStringBody = jacksonObjectMapper().writeValueAsString(body)
        val sig = walletService.sign(jsonStringBody)
        every { registry.getOperatorByOcpi("DE".toByteArray(), "XXX".toByteArray()).sendAsync().get() } returns Tuple2(address, "")
        walletService.verify(jsonStringBody, sig, BasicRole("XXX", "DE"))
    }

    @Test
    fun `verifyRequest loudly fails`() {
        val credentials2 = Credentials.create(generatePrivateKey())
        val jsonStringBody = jacksonObjectMapper().writeValueAsString(body)
        val sig = walletService.sign(jsonStringBody)
        every { registry.getOperatorByOcpi("DE".toByteArray(), "XXX".toByteArray()).sendAsync().get() } returns Tuple2(credentials2.address, "")
        try {
            walletService.verify(jsonStringBody, sig, BasicRole("XXX", "DE"))
        } catch (e: OcpiHubConnectionProblemException) {
            assertThat(e.message).isEqualTo("Could not verify OCN-Signature of request")
        }
    }

    @Test
    fun verifyClientInfo() {
        val objectMapper = jacksonObjectMapper()

        val clientInfo = ClientInfo(partyID = "ABC", countryCode = "DE", role = Role.EMSP, status = ConnectionStatus.PLANNED, lastUpdated = getTimestamp())
        val clientInfoString = objectMapper.writeValueAsString(clientInfo)
        val signature = walletService.sign(clientInfoString)

        every { httpService.mapper } returns objectMapper
        every { registry.getPartyDetailsByOcpi(clientInfo.countryCode.toByteArray(), clientInfo.partyID.toByteArray()).sendAsync().get().component6() } returns address

        val actual = walletService.verifyClientInfo(clientInfoString, signature)
        assertThat(actual).isEqualTo(clientInfo)
    }


}