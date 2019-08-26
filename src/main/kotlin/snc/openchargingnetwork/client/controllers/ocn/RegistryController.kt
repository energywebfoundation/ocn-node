package snc.openchargingnetwork.client.controllers.ocn

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import snc.openchargingnetwork.client.config.Properties
import snc.openchargingnetwork.client.services.WalletService
import snc.openchargingnetwork.contracts.RegistryFacade

@RestController
// TODO: test for API documentation
@RequestMapping("/ocn/registry")
class RegistryController(private val walletService: WalletService,
                         private val properties: Properties,
                         private val registry: RegistryFacade) {

    @GetMapping("/client-info")
    fun getMyAddress() = mapOf(
            "url" to properties.url,
            "address" to walletService.credentials.address)

    @GetMapping("/client-url/{countryCode}/{partyID}")
    fun getClientOf(@PathVariable countryCode: String,
                    @PathVariable partyID: String): String {
        return registry.clientURLOf(countryCode.toByteArray(), partyID.toByteArray()).sendAsync().get()
    }

}