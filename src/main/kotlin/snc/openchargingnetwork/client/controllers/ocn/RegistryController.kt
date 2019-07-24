package snc.openchargingnetwork.client.controllers.ocn

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import snc.openchargingnetwork.client.config.Properties
import snc.openchargingnetwork.contracts.RegistryFacade

@RestController
// TODO: test for API documentation
@RequestMapping("/ocn/registry")
class RegistryController(private val properties: Properties,
                         private val registry: RegistryFacade) {

    @GetMapping("/my-address")
    fun getMyAddress() = properties.url

    @GetMapping("/address/{countryCode}/{partyID}")
    fun getAddressOf(@PathVariable countryCode: String,
                     @PathVariable partyID: String): String {
        return registry.addressOf(countryCode.toByteArray(), partyID.toByteArray()).sendAsync().get()
    }

    @GetMapping("/client/{countryCode}/{partyID}")
    fun getClientOf(@PathVariable countryCode: String,
                    @PathVariable partyID: String): String {
        val address = registry.addressOf(countryCode.toByteArray(), partyID.toByteArray()).sendAsync().get()
        return if (address == "0x0000000000000000000000000000000000000000") {
            ""
        } else {
            registry.brokerOf(address).sendAsync().get()
        }
    }

    @GetMapping("/client/{address}")
    fun getClientOfAddress(@PathVariable address: String): String {
        return registry.brokerOf(address).sendAsync().get()
    }

}