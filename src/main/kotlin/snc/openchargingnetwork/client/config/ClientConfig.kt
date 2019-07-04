package snc.openchargingnetwork.client.config

import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.filter.CommonsRequestLoggingFilter
import org.web3j.crypto.Credentials
import org.web3j.crypto.WalletUtils
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import org.web3j.tx.gas.StaticGasProvider
import snc.openchargingnetwork.client.repositories.RoleRepository
import snc.openchargingnetwork.client.repositories.EndpointRepository
import snc.openchargingnetwork.client.repositories.PlatformRepository
import snc.openchargingnetwork.contracts.RegistryFacade
import java.io.FileNotFoundException

@Configuration
class Configuration(private val properties: Properties) {

    private val web3: Web3j = Web3j.build(HttpService(properties.web3.provider))

    private val credentials: Credentials = try {
        WalletUtils.loadCredentials(
                properties.web3.wallet.password,
                properties.web3.wallet.filepath)
    } catch (e: FileNotFoundException) {
        Credentials.create("0x6e91b60850c1846a1319a36e2300bd409cf22efffa6df1b8a999eb26e71baff2")
    }

    private val gasProvider = StaticGasProvider(0.toBigInteger(), 100000.toBigInteger())


    @Bean
    fun databaseInitializer(platformRepo: PlatformRepository,
                            roleRepo: RoleRepository,
                            endpointRepo: EndpointRepository) = ApplicationRunner {}

    @Bean
    fun registryFacade(): RegistryFacade {
        return RegistryFacade.load(
                properties.web3.contracts.registry,
                web3,
                credentials,
                gasProvider
        )
    }

    @Bean
    fun logFilter(): CommonsRequestLoggingFilter {
        val filter = CommonsRequestLoggingFilter()
        filter.setIncludeQueryString(true)
        filter.setIncludePayload(true)
        filter.setIncludeHeaders(true)
        return filter
    }

    init {
        //TODO: use proper logger
        println("Admin APIKEY = ${properties.apikey}")
        if (credentials.address == "0xe91f246ea100b7320e2d3c3ed0634ed72357f549") {
            println("WARNING: Using unsafe private key")
        }
        println("Client Ethereum address: ${credentials.address}")
    }

}
