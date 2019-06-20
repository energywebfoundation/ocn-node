package snc.connect.broker

import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import snc.connect.broker.repositories.CredentialRepository
import snc.connect.broker.repositories.EndpointRepository
import snc.connect.broker.repositories.OrganizationRepository

@Configuration
class Configuration {

    @Bean()
    fun databaseInitializer(orgRepo: OrganizationRepository,
                            credentialRepo: CredentialRepository,
                            endpointRepo: EndpointRepository) = ApplicationRunner {

    }

}
