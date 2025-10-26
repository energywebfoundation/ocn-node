/*
    Copyright 2019-2020 eMobilify GmbH

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/

package snc.openchargingnetwork.node.services

import java.time.Instant
import org.slf4j.LoggerFactory
import org.springframework.http.HttpMethod
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import snc.openchargingnetwork.node.components.HttpClientComponent
import snc.openchargingnetwork.node.components.OcnRegistryComponent
import snc.openchargingnetwork.node.config.RegistryIndexerProperties
import snc.openchargingnetwork.node.models.OcnHeaders
import snc.openchargingnetwork.node.models.entities.NetworkClientInfoEntity
import snc.openchargingnetwork.node.models.entities.PlatformEntity
import snc.openchargingnetwork.node.models.entities.RoleEntity
import snc.openchargingnetwork.node.models.ocpi.*
import snc.openchargingnetwork.node.repositories.EndpointRepository
import snc.openchargingnetwork.node.repositories.NetworkClientInfoRepository
import snc.openchargingnetwork.node.repositories.PlatformRepository
import snc.openchargingnetwork.node.repositories.RoleRepository
import snc.openchargingnetwork.node.tools.extractToken
import snc.openchargingnetwork.node.tools.generateUUIDv4Token
import snc.openchargingnetwork.node.tools.getTimestamp

/**
 * Enhanced HubClientInfoService with both push and pull models for party discovery and updates This
 * service manages the OCPI Hub Client Info module with comprehensive functionality Uses
 * event-driven architecture for broadcasting changes
 */
@Service
class HubClientInfoService(
        private val platformRepo: PlatformRepository,
        private val roleRepo: RoleRepository,
        private val endpointRepo: EndpointRepository,
        private val networkClientInfoRepo: NetworkClientInfoRepository,
        private val httpClientComponent: HttpClientComponent,
        private val ocnRegistryComponent: OcnRegistryComponent,
        private val routingService: RoutingService,
        private val walletService: WalletService,
        private val ocnRulesService: OcnRulesService,
        private val registryService: RegistryService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(HubClientInfoService::class.java)
    }

    /** Get a HubClientInfo list of local and known network connections */
    fun getList(fromAuthorization: String): List<ClientInfo> {
        val clientInfoList = mutableListOf<ClientInfo>()

        val requestingPlatform =
                platformRepo.findByAuth_TokenC(fromAuthorization.extractToken())
                        ?: throw IllegalStateException(
                                "Sender is validated but cannot find them by their authorization token"
                        )

        // add connected party roles
        for (platform in platformRepo.findAll()) {
            for (role in roleRepo.findAllByPlatformID(platform.id)) {
                // only if whitelisted
                val counterPartyBasicRole = BasicRole(id = role.partyID, country = role.countryCode)

                if (ocnRulesService.isWhitelisted(requestingPlatform, counterPartyBasicRole)) {
                    clientInfoList.add(
                            ClientInfo(
                                    partyID = role.partyID,
                                    countryCode = role.countryCode,
                                    role = role.role,
                                    status = platform.status,
                                    lastUpdated = platform.lastUpdated
                            )
                    )
                }
            }
        }

        // add network party roles
        for (role in networkClientInfoRepo.findAll()) {
            // only if whitelisted
            if (ocnRulesService.isWhitelisted(requestingPlatform, role.party)) {
                clientInfoList.add(
                        ClientInfo(
                                partyID = role.party.id,
                                countryCode = role.party.country,
                                role = role.role,
                                status = ConnectionStatus.PLANNED,
                                lastUpdated = role.lastUpdated
                        )
                )
            }
        }

        return clientInfoList
    }

    /**
     * Get parties who should be sent a HubClientInfo Push notification (sans the changedPlatform if
     * provided)
     */
    fun getPartiesToNotifyOfClientInfoChange(
            changedPlatform: PlatformEntity? = null,
            clientInfo: ClientInfo
    ): List<RoleEntity> {
        val clientsToNotify = mutableListOf<RoleEntity>()
        for (platform in platformRepo.findAll()) {

            // Only push the update if the platform is connected and it isn't the platform that
            // triggered the event
            if (platform.status == ConnectionStatus.CONNECTED && platform.id != changedPlatform?.id
            ) {

                // Only push the update if the platform has implemented the HubClientInfo Receiver
                // endpoint
                val hubClientInfoPutEndpoint =
                        endpointRepo.findFirstByPlatformIDAndIdentifierAndRoleOrderByIdAsc(
                                platformID = platform.id,
                                identifier = ModuleID.HUB_CLIENT_INFO.id,
                                Role = InterfaceRole.RECEIVER
                        )
                if (hubClientInfoPutEndpoint != null) {
                    for (clientRole in
                            roleRepo.findAllByPlatformID(
                                    platform.id
                            )) { // TODO: It could be redundant to notify each party. Perhaps it's
                        // better to assume single receiver interface

                        // Only push the update if the role has whitelisted the ClientInfo owner
                        val counterParty =
                                BasicRole(id = clientInfo.partyID, country = clientInfo.countryCode)
                        if (ocnRulesService.isWhitelisted(platform, counterParty)) {
                            clientsToNotify.add(clientRole)
                        }
                    }
                }
            }
        }

        return clientsToNotify
    }

    /** Save a client info object */
    fun saveClientInfo(clientInfo: ClientInfo) {
        val basicRole = BasicRole(id = clientInfo.partyID, country = clientInfo.countryCode)

        val updatedClientInfo: NetworkClientInfoEntity? =
                networkClientInfoRepo.findFirstByPartyAndRoleOrderByIdAsc(basicRole, clientInfo.role)?.let {
                    // check status has changed
                    if (it.status != clientInfo.status) {
                        it.apply {
                            status = clientInfo.status
                            lastUpdated = clientInfo.lastUpdated
                        }
                    } else {
                        null
                    }
                }
                        ?: NetworkClientInfoEntity(
                                party = basicRole,
                                role = clientInfo.role,
                                status = clientInfo.status,
                                lastUpdated = clientInfo.lastUpdated
                        )

        if (updatedClientInfo != null) {
            networkClientInfoRepo.save(updatedClientInfo)
        }
    }

    /** Send a notification of a ClientInfo change to a list of parties */
    fun notifyPartiesOfClientInfoChange(
            parties: Iterable<RoleEntity>,
            changedClientInfo: ClientInfo
    ) {
        for (party in parties) {
            val tokenB = platformRepo.findById(party.platformID).get().auth.tokenB
            if (tokenB != null) {
                notifyPartyOfClientInfoChange(
                        party.partyID,
                        party.countryCode,
                        tokenB,
                        changedClientInfo
                )
            }
        }
    }

    /** Send a notification of a ClientInfo change to a list of parties asynchronously */
    @Async
    fun notifyPartiesOfClientInfoChangeAsync(
            parties: Iterable<RoleEntity>,
            changedClientInfo: ClientInfo
    ) {
        logger.info(
                "Starting async notification of client info change to ${parties.count()} parties"
        )
        notifyPartiesOfClientInfoChange(parties, changedClientInfo)
        logger.info("Completed async notification of client info change")
    }

    fun notifyPartyOfClientInfoChange(
            partyId: String,
            countryCode: String,
            tokenB: String,
            changedClientInfo: ClientInfo
    ) {
        val sender =
                BasicRole(
                        id = "OCN",
                        country = "CH"
                ) // TODO: put node platformID and countryCode in a shared, configurable
        // location
        val receiver = BasicRole(partyId, countryCode)
        val requestVariables =
                OcpiRequestVariables(
                        module = ModuleID.HUB_CLIENT_INFO,
                        interfaceRole = InterfaceRole.RECEIVER,
                        method = HttpMethod.PUT,
                        headers =
                                OcnHeaders(
                                        authorization = "Token ${tokenB}",
                                        requestID = generateUUIDv4Token(),
                                        correlationID = generateUUIDv4Token(),
                                        sender = sender,
                                        receiver = receiver
                                ),
                        body = changedClientInfo,
                        urlPath = "${changedClientInfo.countryCode}/${changedClientInfo.partyID}"
                )

        val (url, headers) =
                routingService.prepareLocalPlatformRequest(requestVariables, proxied = false)

        try {
            httpClientComponent.makeOcpiRequest<Unit>(url, headers, requestVariables)
        } catch (e: Exception) { // fire and forget; catch any error and log
            logger.warn("Error notifying $receiver of client info change: ${e.message}")
        }
    }

    /** Send a notification of a ClientInfo change to other nodes on the network */
    fun notifyNodesOfClientInfoChange(changedClientInfo: ClientInfo) {
        val requestBodyString = httpClientComponent.mapper.writeValueAsString(changedClientInfo)
        val signature = walletService.sign(requestBodyString)

        val nodes = registryService.getNodes(omitMine = true)

        for (node in nodes) {
            try {
                httpClientComponent.putOcnClientInfo(node.url, signature, changedClientInfo)
            } catch (e: Exception) { // fire and forget; catch any error and log
                logger.warn("Error notifying $node of client info change: ${e.message}")
            }
        }
    }

    /** Confirm the online status of the client corresponding to a role */
    @Async
    fun renewClientConnection(sender: BasicRole) {
        val role =
                roleRepo.findFirstByCountryCodeAndPartyIDAllIgnoreCaseOrderByIdAsc(
                        countryCode = sender.country,
                        partyID = sender.id
                )
                        ?: throw IllegalArgumentException("sender could not be found")

        val client = platformRepo.findById(role.platformID).get()
        client.renewConnection(Instant.now())
        platformRepo.save(client)
    }

    fun getIndexedParties(): List<snc.openchargingnetwork.node.models.Party> {
        logger.debug("Fetching indexed parties from registry...")
        val parties = ocnRegistryComponent.getRegistry().parties
        logger.debug("Retrieved ${parties.size} parties from registry indexer")
        return parties
    }

    @Async
    fun checkForNewPartiesFromRegistry(parties: List<snc.openchargingnetwork.node.models.Party>) {
        logger.info("Starting registry party discovery...")

        for (party in parties) {
            for (role in party.roles) {
                val partyId = BasicRole(party.partyId, party.countryCode)

                // Check if this party/role combination already exists
                if (!networkClientInfoRepo.existsByPartyAndRole(partyId, role)) {
                    val networkClientInfo =
                            NetworkClientInfoEntity(
                                    party = partyId.uppercase(),
                                    role = role,
                                    status = ConnectionStatus.PLANNED,
                                    lastUpdated = getTimestamp()
                            )

                    // Mark as newly discovered - this triggers PlannedRoleFoundDomainEvent
                    networkClientInfo.foundNewlyPlannedRole()
                    networkClientInfoRepo.save(networkClientInfo)

                    logger.info(
                            "Discovered new party: ${partyId.id} (${partyId.country}) with role: $role"
                    )
                }
            }
        }
    }

    @Async
    fun checkForSuspendedUpdates(parties: List<snc.openchargingnetwork.node.models.Party>) {
        logger.info("Starting role update check - looking for parties to suspend...")

        // Create a set of all party/role combinations from the indexed registry
        val indexedPartyRoles = mutableSetOf<Pair<BasicRole, Role>>()
        for (party in parties) {
            val partyId = BasicRole(party.partyId, party.countryCode)
            for (role in party.roles) {
                indexedPartyRoles.add(Pair(partyId, role))
            }
        }

        // Check all existing network client info entries
        val allExistingClientInfo = networkClientInfoRepo.findAll()
        var suspendedCount = 0

        for (existingClientInfo in allExistingClientInfo) {
            val partyRolePair = Pair(existingClientInfo.party, existingClientInfo.role)

            // If this party/role combination is not in the indexed registry anymore
            if (!indexedPartyRoles.contains(partyRolePair)) {
                // Only suspend if it's not already suspended
                if (existingClientInfo.status != ConnectionStatus.SUSPENDED) {
                    existingClientInfo.apply {
                        status = ConnectionStatus.SUSPENDED
                        lastUpdated = getTimestamp()
                    }

                    existingClientInfo.foundSuspendedRole()
                    networkClientInfoRepo.save(existingClientInfo)
                    suspendedCount++

                    logger.info(
                            "Suspended party: ${existingClientInfo.party.id} (${existingClientInfo.party.country}) with role: ${existingClientInfo.role} - no longer in registry"
                    )
                }
            }
        }

        logger.info(
                "Role update check completed. Suspended $suspendedCount parties that are no longer in registry."
        )
    }

    @Async
    fun syncHubClientInfo() {
        logger.info("Starting comprehensive hub client info sync...")
        try {
            val indexedParties = getIndexedParties()
            checkForNewPartiesFromRegistry(indexedParties)
            checkForSuspendedUpdates(indexedParties)
        } catch (e: Exception) {
            logger.error("Error during hub client info sync: ${e.message}", e)
            throw e
        }
    }

    /** Get all hub client info for a requesting platform (alias for getList for consistency) */
    fun getHubClientInfoList(fromAuthorization: String): List<ClientInfo> {
        return getList(fromAuthorization)
    }

    /** Get all connected parties (both local and network) as ClientInfo objects */
    fun getAllRegisteredParties(): List<ClientInfo> {
        val allParties = mutableListOf<ClientInfo>()

        for (role in networkClientInfoRepo.findAll()) {
            allParties.add(
                    ClientInfo(
                            partyID = role.party.id,
                            countryCode = role.party.country,
                            role = role.role,
                            status = role.status,
                            lastUpdated = role.lastUpdated
                    )
            )
        }

        return allParties
    }

    fun updateClientInfo(clientInfo: ClientInfo) {
        val basicRole = BasicRole(clientInfo.partyID, clientInfo.countryCode)
        val existingClientInfo =
                networkClientInfoRepo.findByPartyAndRole(basicRole, clientInfo.role)
        if (existingClientInfo != null) {
            existingClientInfo.apply {
                status = clientInfo.status
                lastUpdated = clientInfo.lastUpdated
            }
            networkClientInfoRepo.save(existingClientInfo)
        } else {
            logger.warn(
                    "Client info not found for party: ${clientInfo.partyID} (${clientInfo.countryCode}) with role: ${clientInfo.role}"
            )
        }
    }
}
