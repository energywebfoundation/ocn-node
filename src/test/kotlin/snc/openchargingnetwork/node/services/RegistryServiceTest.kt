package snc.openchargingnetwork.node.services

import com.olisystems.ocnregistryv2_0.OcnRegistry
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.web3j.tuples.generated.Tuple2
import snc.openchargingnetwork.node.config.NodeProperties
import snc.openchargingnetwork.node.models.RegistryNode
import snc.openchargingnetwork.node.models.ocpi.BasicRole


class RegistryServiceTest {

    private val registry: OcnRegistry = mockk()
    private val properties: NodeProperties = mockk()

    private val registryService: RegistryService

    init {
        registryService = RegistryService(registry, properties)
    }

    @Test
    fun getNodes() {
        val nodes = listOf(
                RegistryNode(operator = "0xf17f52151EbEF6C7334FAD080c5704D77216b732", url = "http://localhost:8080"),
                RegistryNode(operator = "0xC5fdf4076b8F3A5357c5E395ab970B5B54098Fef", url = "http://localhost:8081"))

        every { registry.nodeOperators.sendAsync().get() } returns nodes.map { it.operator }

        for (node in nodes) {
            every { registry.getNode(node.operator).sendAsync().get() } returns node.url
        }

        every { properties.privateKey } returns "0xae6ae8e5ccbfb04590405997ee2d52d2b330726137b875053c36d94e974d162f"

        val actual = registryService.getNodes(omitMine = true)
        assertThat(actual.size).isEqualTo(1)
        assertThat(actual[0].operator).isEqualTo(nodes[1].operator)
        assertThat(actual[0].url).isEqualTo(nodes[1].url)
    }

    @Test
    fun `isRoleKnown with belongsToMe flag returns true`() {
        val role = BasicRole("XYZ", "CH")
        val serverURL = "https://my.server.com"
        val serverEthAddress = "0x9bC1169Ca09555bf2721A5C9eC6D69c8073bfeB4"
        every { registry.getOperatorByOcpi(role.country.toByteArray(), role.id.toByteArray()).sendAsync().get() } returns Tuple2(serverEthAddress, serverURL)
        every { properties.url } returns serverURL
        every { properties.privateKey } returns "0x1c3e5453c0f9aa74a8eb0216310b2b013f017813a648fce364bf41dbc0b37647"
        assertThat(registryService.isRoleKnown(role)).isEqualTo(true)
    }


    @Test
    fun `isRoleKnown without belongsToMe flag returns true`() {
        val role = BasicRole("XYZ", "CH")
        val serverURL = "https://my.server.com"
        every { registry.getOperatorByOcpi(role.country.toByteArray(), role.id.toByteArray()).sendAsync().get() } returns Tuple2("", serverURL)
        assertThat(registryService.isRoleKnown(role, belongsToMe = false)).isEqualTo(true)
    }

    @Test
    fun getRemoteNodeURLOf() {
        val role = BasicRole("XXX", "NL")
        every { registry.getOperatorByOcpi(role.country.toByteArray(), role.id.toByteArray()).sendAsync().get() } returns Tuple2("", "https://some.node.com")
        assertThat(registryService.getRemoteNodeUrlOf(role)).isEqualTo("https://some.node.com")
    }

}