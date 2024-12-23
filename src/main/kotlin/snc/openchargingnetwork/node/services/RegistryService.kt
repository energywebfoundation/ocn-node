package snc.openchargingnetwork.node.services

import com.olisystems.ocnregistryv2_0.OcnRegistry
import org.springframework.stereotype.Service
import org.web3j.crypto.Credentials
import org.web3j.crypto.Keys
import snc.openchargingnetwork.node.config.NodeProperties
import snc.openchargingnetwork.node.models.RegistryNode
import snc.openchargingnetwork.node.models.RegistryPartyDetailsBasic
import snc.openchargingnetwork.node.models.exceptions.OcpiHubUnknownReceiverException
import snc.openchargingnetwork.node.models.ocpi.BasicRole
import snc.openchargingnetwork.node.tools.checksum

/**
 * Simplifies calls to the Registry (registry and permissions smart contracts)
 * TODO: review tests
 */
@Service
class RegistryService(private val registry: OcnRegistry,
                      private val properties: NodeProperties) {

    /**
     * Get nodes listed in registry
     */
    fun getNodes(omitMine: Boolean = false): List<RegistryNode> {
        val nodes = registry.nodeOperators.sendAsync().get()
                .map {
                    val url = registry.getNode(it as String).sendAsync().get()
                    RegistryNode(operator = it.checksum(), url = url)
                }

        return if (omitMine) {
            val myAddress = Credentials.create(properties.privateKey).address.checksum()
            nodes.filter { it.operator != myAddress }
        } else {
            nodes
        }
    }

    /**
     * check OCN registry to see if basic role is registered
     */
    fun isRoleKnown(role: BasicRole, belongsToMe: Boolean = true): Boolean {
        val country = role.country.toByteArray()
        val id = role.id.toByteArray()


        val (operator, domain) = registry.getOperatorByOcpi(country, id).sendAsync().get()
        if (belongsToMe) {
            val myKey = Credentials.create(properties.privateKey).address
            return domain == properties.url && Keys.toChecksumAddress(operator) == Keys.toChecksumAddress(myKey)
        }

        return domain != ""
    }

    /**
     * get the OCN Node URL as registered by the basic role in the OCN Registry
     */
    fun getRemoteNodeUrlOf(role: BasicRole): String {
        val country = role.country.toByteArray()
        val id = role.id.toByteArray()

        val (_, domain) = registry.getOperatorByOcpi(country, id).sendAsync().get()
        if (domain == "") {
            throw OcpiHubUnknownReceiverException("Recipient not registered on OCN")
        }
        return domain
    }

    /**
     * Returns basic party details (address and node operator)
     */
    fun getPartyDetails(role: BasicRole): RegistryPartyDetailsBasic {
        val country = role.country.toByteArray()
        val id = role.id.toByteArray()

        val result = registry.getPartyDetailsByOcpi(country, id).sendAsync().get()
        return RegistryPartyDetailsBasic(address = result.component1(), operator = result.component7())
    }


}