package snc.connect.broker

import org.springframework.boot.context.properties.ConfigurationProperties
import snc.connect.broker.tools.generateUUIDv4Token

@ConfigurationProperties("broker")
class Properties(val apikey: String = generateUUIDv4Token()) {

    lateinit var host: String

    init {
        //TODO: use proper logger
        println("admin.apikey = $apikey")
    }
}