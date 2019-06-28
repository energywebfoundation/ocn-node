package snc.openchargingnetwork.client.config

import org.springframework.boot.context.properties.ConfigurationProperties
import snc.openchargingnetwork.client.tools.generateUUIDv4Token

@ConfigurationProperties("ocn.client")
class Properties(val apikey: String = generateUUIDv4Token()) {

    lateinit var url: String

    val web3 = Web3()

    class Web3 {

        lateinit var provider: String

        val wallet = Wallet()

        val contracts = Contracts()

        class Wallet {
            lateinit var password: String
            lateinit var filepath: String
        }

        class Contracts {
            lateinit var registry: String
        }
    }

    init {
        //TODO: use proper logger
        println("admin.apikey = $apikey")
    }
}