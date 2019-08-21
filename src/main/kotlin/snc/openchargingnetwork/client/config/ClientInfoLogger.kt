package snc.openchargingnetwork.client.config

import org.springframework.stereotype.Component
import snc.openchargingnetwork.client.services.WalletService

@Component
class ClientInfoLogger(properties: Properties,
                       walletService: WalletService) {

    init {
        println("\n====================================================\n" +
                "ADMIN_APIKEY = ${properties.apikey}\n" +
                "URL = ${properties.url}\n" +
                "ADDRESS = ${walletService.address}" +
                "\n====================================================\n")
    }

}