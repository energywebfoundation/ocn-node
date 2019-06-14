package snc.connect.broker

import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class Configuration {

    @Bean()
    fun databaseInitializer(partyRepository: PartyRepository) = ApplicationRunner {

    }

}
