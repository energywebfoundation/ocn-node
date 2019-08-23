package snc.openchargingnetwork.client.services

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpMethod
import org.web3j.crypto.Credentials
import org.web3j.crypto.Sign
import snc.openchargingnetwork.client.models.OcnMessageRequestBody
import snc.openchargingnetwork.client.models.ocpi.OcpiRequestHeaders
import snc.openchargingnetwork.client.models.ocpi.OcpiType
import snc.openchargingnetwork.client.models.entities.WalletEntity
import snc.openchargingnetwork.client.models.exceptions.OcpiHubConnectionProblemException
import snc.openchargingnetwork.client.models.ocpi.BasicRole
import snc.openchargingnetwork.client.models.ocpi.InterfaceRole
import snc.openchargingnetwork.client.models.ocpi.ModuleID
import snc.openchargingnetwork.client.repositories.WalletRepository
import snc.openchargingnetwork.client.tools.generatePrivateKey
import snc.openchargingnetwork.contracts.RegistryFacade

class WalletServiceTest {

    private val privateKey = "71eb44ccb94e00b6d462232618085daeea239daf76804acf040a7f037549598f"
    private val address = "0xF686dd2b1Cbf4c77079Ca48D176e157ddB744eeF"

    private val body = OcnMessageRequestBody(
            method = HttpMethod.GET,
            module = ModuleID.SESSIONS,
            interfaceRole = InterfaceRole.RECEIVER,
            headers = OcpiRequestHeaders(
                    requestID = "1",
                    correlationID = "1",
                    ocpiFromCountryCode = "DE",
                    ocpiFromPartyID = "XXX",
                    ocpiToCountryCode = "DE",
                    ocpiToPartyID = "AAA"),
            expectedResponseType = OcpiType.SESSION_ARRAY)

    private val walletRepo: WalletRepository = mockk()
    private val registry: RegistryFacade = mockk()

    private val walletService: WalletService

    init {
        every { walletRepo.findByIdOrNull(1L) } returns WalletEntity(privateKey)
        walletService = WalletService(walletRepo, registry)
    }


    @Test
    fun `init credentials`() {
        assertThat(walletService.credentials.address).isEqualTo(address.toLowerCase())
    }


    @Test
    fun toByteArray() {
        val sig = "0x9955af11969a2d2a7f860cb00e6a00cfa7c581f5df2dbe8ea16700b33f4b4b9" +
                "b69f945012f7ea7d3febf11eb1b78e1adc2d1c14c2cf48b25000938cc1860c83e01"
        val (r, s, v) = walletService.toByteArray(sig)
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
        every { registry.clientAddressOf("DE".toByteArray(), "XXX".toByteArray()).sendAsync().get() } returns address
        walletService.verify(jsonStringBody, sig, BasicRole("XXX", "DE"))
    }

    @Test
    fun `verifyRequest loudly fails`() {
        val credentials2 = Credentials.create(generatePrivateKey())
        val jsonStringBody = jacksonObjectMapper().writeValueAsString(body)
        val sig = walletService.sign(jsonStringBody)
        every { registry.clientAddressOf("DE".toByteArray(), "XXX".toByteArray()).sendAsync().get() } returns credentials2.address
        try {
            walletService.verify(jsonStringBody, sig, BasicRole("XXX", "DE"))
        } catch (e: OcpiHubConnectionProblemException) {
            assertThat(e.message).isEqualTo("Could not verify OCN-Signature of request")
        }
    }


}