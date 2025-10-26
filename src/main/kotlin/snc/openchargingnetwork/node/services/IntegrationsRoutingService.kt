package snc.openchargingnetwork.node.services;

import jdk.jshell.spi.ExecutionControl
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpServerErrorException
import snc.openchargingnetwork.node.models.ocpi.BasicRole
import snc.openchargingnetwork.node.models.ocpi.ModuleID

@Service
class IntegrationsRoutingService(
    private val registryService: RegistryService
) {
    fun getIntegrationReceivingParties(module: ModuleID, from: BasicRole): List<BasicRole> {
        // Don't allow other modules than CDRs for now
        if(module !== ModuleID.CDRS) {
            throw NotImplementedError("Module integration support not yet implemented");
        }

        return listOf(BasicRole("OLI", "DE"))
    }
}
