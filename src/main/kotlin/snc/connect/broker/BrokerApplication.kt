package snc.connect.broker

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class BrokerApplication

fun main(args: Array<String>) {
    runApplication<BrokerApplication>(*args)
}