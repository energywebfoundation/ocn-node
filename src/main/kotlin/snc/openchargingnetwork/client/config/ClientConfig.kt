package snc.openchargingnetwork.client.config

import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.web3j.crypto.WalletUtils
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import org.web3j.tx.gas.StaticGasProvider
import snc.openchargingnetwork.client.repositories.RoleRepository
import snc.openchargingnetwork.client.repositories.EndpointRepository
import snc.openchargingnetwork.client.repositories.PlatformRepository
import snc.openchargingnetwork.contracts.RegistryFacade

@Configuration
class Configuration(private val properties: Properties) {

    @Bean
    fun databaseInitializer(platformRepo: PlatformRepository,
                            roleRepo: RoleRepository,
                            endpointRepo: EndpointRepository) = ApplicationRunner {}

    @Bean
    fun registryFacade(): RegistryFacade {
        val credentials = WalletUtils.loadCredentials(
                properties.web3.wallet.password,
                properties.web3.wallet.filepath
        )
        val gasProvider = StaticGasProvider(0.toBigInteger(), 100000.toBigInteger())
        return RegistryFacade.load(
                properties.web3.contracts.registry,
                Web3j.build(HttpService(properties.web3.provider)),
                credentials,
                gasProvider
        )
    }

}
