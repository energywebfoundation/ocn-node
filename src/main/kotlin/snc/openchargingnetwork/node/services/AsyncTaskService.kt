package snc.openchargingnetwork.node.services

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import snc.openchargingnetwork.contracts.Permissions
import snc.openchargingnetwork.node.models.OcnAppPermission
import snc.openchargingnetwork.node.models.matches
import snc.openchargingnetwork.node.models.ocpi.BasicRole
import snc.openchargingnetwork.node.models.ocpi.InterfaceRole
import snc.openchargingnetwork.node.models.ocpi.ModuleID
import java.util.concurrent.CompletableFuture

@Service
class AsyncTaskService(private val permissions: Permissions,
                       private val httpService: HttpService) {

    companion object {
        private val logger = LoggerFactory.getLogger(AsyncTaskService::class.java)
    }

    @Async
    fun findLinkedApps(party: BasicRole, module: ModuleID, interfaceRole: InterfaceRole): CompletableFuture<List<BasicRole>> {
        /**
         * TODO: where to run most optimally?
         *  - runs once we have validated the sender AND receiver on the network AND signature
         *  - must use routing service to find out where app is located on network
         *  - request must be modified with new signature containing new recipient
         *  - fire and forget request
         */

        val recipients: MutableList<BasicRole> = mutableListOf()

        val agreements = permissions
                .getUserAgreementsByOcpi(party.country.toByteArray(), party.id.toByteArray())
                .sendAsync()
                .get()

        for (agreement in agreements) {
            val (countryCode, partyId, _, _, needs) = permissions.getApp(agreement as String).sendAsync().get()

            logger.info("needs=$needs")

//            Thread.sleep(5000L)

            for (need in needs) {
                if (OcnAppPermission.getByIndex(need).matches(module, interfaceRole)) {
                    recipients.add(BasicRole(
                            id = partyId.toString(Charsets.UTF_8),
                            country = countryCode.toString(Charsets.UTF_8)))
                    logger.info("need $need matched")
                    break
                }
                logger.info("need $need not matched")
            }
        }

        return CompletableFuture.completedFuture(recipients)
    }

}