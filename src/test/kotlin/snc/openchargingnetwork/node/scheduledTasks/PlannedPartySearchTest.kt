package snc.openchargingnetwork.node.scheduledTasks

import com.olisystems.ocnregistryv2_0.OcnRegistry
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.web3j.tuples.generated.Tuple7
import org.web3j.tuples.generated.Tuple9
import snc.openchargingnetwork.node.config.NodeProperties
import snc.openchargingnetwork.node.models.entities.NetworkClientInfoEntity
import snc.openchargingnetwork.node.repositories.NetworkClientInfoRepository
import snc.openchargingnetwork.node.repositories.RoleRepository
import java.math.BigInteger

class PlannedPartySearchTest {

    private val registry: OcnRegistry = mockk()
    private val roleRepo: RoleRepository = mockk()
    private val networkClientInfoRepo: NetworkClientInfoRepository = mockk()
    private val properties: NodeProperties = mockk()

    private val plannedPartySearch: PlannedPartySearch

    init {
        plannedPartySearch = PlannedPartySearch(
                registry,
                roleRepo,
                networkClientInfoRepo,
                properties
        )
    }

    @Test
    fun deletedParty_should_notBePlanned() {
        every { properties.privateKey } returns "c6cbd7d76bc5baca530c875663711b947efa6a86a900a9e8645ce32e5821484e"
        val address = "0xc0ffee254729296a45a3885639AC7E10F9d54979"
        every { registry.parties.sendAsync().get() } returns listOf(address)
        every { registry.getPartyDetailsByAddress(address).sendAsync().get() } returns
                Tuple9(
                    "",
                    ByteArray(2),
                    ByteArray(3),
                    emptyList<BigInteger>(),
                    BigInteger.ZERO,
                    "0x0000000000000000000000000000000000000000",
                    "",
                    "",
                    false
                )

        plannedPartySearch.run()
        verify(exactly = 0) { roleRepo.existsByCountryCodeAndPartyIDAllIgnoreCase(any(), any()) }
        verify(exactly = 0) { networkClientInfoRepo.save(any<NetworkClientInfoEntity>()) }
    }
}
