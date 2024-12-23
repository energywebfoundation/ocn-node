package snc.openchargingnetwork.node.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "spring.datasource")
class DataSourceProperties {
    lateinit var url: String
    lateinit var username: String
    lateinit var password: String
}