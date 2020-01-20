package snc.openchargingnetwork.node.controllers.ocn

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import snc.openchargingnetwork.node.config.NodeProperties
import snc.openchargingnetwork.node.services.WalletService
import snc.openchargingnetwork.contracts.RegistryFacade

@RestController
// TODO: test for API documentation
@RequestMapping("/ocn/registry")
class RegistryController(private val walletService: WalletService,
                         private val properties: NodeProperties,
                         private val registry: RegistryFacade) {

    @GetMapping("/node-info")
    fun getMyNodeInfo() = mapOf(
            "url" to properties.url,
            "address" to walletService.credentials.address)

    @GetMapping("/node/{countryCode}/{partyID}")
    fun getNodeOf(@PathVariable countryCode: String,
                    @PathVariable partyID: String): Any {
        val countryBytes = countryCode.toUpperCase().toByteArray()
        val idBytes = partyID.toUpperCase().toByteArray()

        val url = registry.nodeURLOf(countryBytes, idBytes).sendAsync().get()
        val address = registry.nodeAddressOf(countryBytes, idBytes).sendAsync().get()

        if (url == "" || address == "0x0000000000000000000000000000000000000000") {
            return "Party not registered on OCN"
        }

        return mapOf("url" to url, "address" to address)
    }

}