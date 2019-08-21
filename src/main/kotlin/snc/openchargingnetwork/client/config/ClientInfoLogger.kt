package snc.openchargingnetwork.client.config

import org.springframework.stereotype.Component
import snc.openchargingnetwork.client.services.CredentialsService

@Component
class ClientInfoLogger(properties: Properties,
                       credentialsService: CredentialsService) {

    init {
        println("\n====================================================\n" +
                "ADMIN_APIKEY = ${properties.apikey}\n" +
                "URL = ${properties.url}\n" +
                "ADDRESS = ${credentialsService.credentials.address}" +
                "\n====================================================\n")
    }

}