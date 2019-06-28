package snc.openchargingnetwork.client

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import snc.openchargingnetwork.client.config.Properties

@SpringBootApplication
@EnableConfigurationProperties(Properties::class)
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args) {
        
    }
}