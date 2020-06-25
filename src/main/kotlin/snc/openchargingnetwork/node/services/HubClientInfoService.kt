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

import org.slf4j.LoggerFactory
import org.springframework.http.HttpMethod
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
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
import java.time.Instant

@Service
class HubClientInfoService(private val platformRepo: PlatformRepository,
                           private val roleRepo: RoleRepository,
                           private val endpointRepo: EndpointRepository,
                           private val networkClientInfoRepo: NetworkClientInfoRepository,
                           private val httpService: HttpService,
                           private val routingService: RoutingService,
                           private val walletService: WalletService,
                           private val ocnRulesService: OcnRulesService,
                           private val registryService: RegistryService) {

    companion object {
        private val logger = LoggerFactory.getLogger(HubClientInfoService::class.java)
    }

    /**
     * Get a HubClientInfo list of local and known network connections
     */
    fun getList(fromAuthorization: String): List<ClientInfo> {
        val clientInfoList = mutableListOf<ClientInfo>()

        val requestingPlatform = platformRepo.findByAuth_TokenC(fromAuthorization.extractToken())
                ?: throw IllegalStateException("Sender is validated but cannot find them by their authorization token")

        // add connected party roles
        for (platform in platformRepo.findAll()) {
            for (role in roleRepo.findAllByPlatformID(platform.id)) {
                // only if whitelisted
                val counterPartyBasicRole = BasicRole(id = role.partyID, country = role.countryCode)

                if (ocnRulesService.isWhitelisted(requestingPlatform, counterPartyBasicRole)) {
                    clientInfoList.add(ClientInfo(
                            partyID = role.partyID,
                            countryCode = role.countryCode,
                            role = role.role,
                            status = platform.status,
                            lastUpdated = platform.lastUpdated))
                }
            }
        }

        // add network party roles
        for (role in networkClientInfoRepo.findAll()) {
            // only if whitelisted
            if (ocnRulesService.isWhitelisted(requestingPlatform, role.party)) {
                clientInfoList.add(ClientInfo(
                        partyID = role.party.id,
                        countryCode = role.party.country,
                        role = role.role,
                        status = ConnectionStatus.PLANNED,
                        lastUpdated = role.lastUpdated
                ))
            }
        }

        return clientInfoList
    }

    /**
     * Get parties who should be sent a HubClientInfo Push notification (sans the changedPlatform if provided)
     */
    fun getPartiesToNotifyOfClientInfoChange(changedPlatform: PlatformEntity? = null, clientInfo: ClientInfo) : List<RoleEntity> {
        val clientsToNotify = mutableListOf<RoleEntity>()
        for (platform in platformRepo.findAll()) {

            // Only push the update if the platform is connected and it isn't the platform that triggered the event
            if (platform.status == ConnectionStatus.CONNECTED && platform.id != changedPlatform?.id) {

                // Only push the update if the platform has implemented the HubClientInfo Receiver endpoint
                val hubClientInfoPutEndpoint = endpointRepo.findByPlatformIDAndIdentifierAndRole(
                        platformID = platform.id,
                        identifier = ModuleID.HUB_CLIENT_INFO.id,
                        Role = InterfaceRole.RECEIVER
                )
                if (hubClientInfoPutEndpoint != null) {
                    for (clientRole in roleRepo.findAllByPlatformID(platform.id)) { //TODO: It could be redundant to notify each party. Perhaps it's better to assume single receiver interface

                        // Only push the update if the role has whitelisted the ClientInfo owner
                        val counterParty = BasicRole(id = clientInfo.partyID, country = clientInfo.countryCode)
                        if (ocnRulesService.isWhitelisted(platform, counterParty)) {
                            clientsToNotify.add(clientRole)
                        }
                    }
                }
            }
        }

        return clientsToNotify
    }

    /**
     * Save a client info object
     */
    fun saveClientInfo(clientInfo: ClientInfo) {
        val basicRole = BasicRole(id = clientInfo.partyID, country = clientInfo.countryCode)

        val updatedClientInfo: NetworkClientInfoEntity? = networkClientInfoRepo.findByPartyAndRole(basicRole, clientInfo.role)?.let {
            // check status has changed
            if (it.status != clientInfo.status) {
                it.apply {
                    status = clientInfo.status
                    lastUpdated = clientInfo.lastUpdated
                }
            } else {
                null
            }
        } ?: NetworkClientInfoEntity(
                party = basicRole,
                role = clientInfo.role,
                status = clientInfo.status,
                lastUpdated = clientInfo.lastUpdated)

        if (updatedClientInfo != null) {
            networkClientInfoRepo.save(updatedClientInfo)
        }
    }

    /**
     * Send a notification of a ClientInfo change to a list of parties
     */
    fun notifyPartiesOfClientInfoChange(parties: Iterable<RoleEntity>, changedClientInfo: ClientInfo) {
        for (party in parties) {
            val sender = BasicRole(id = "OCN", country = "CH") // TODO: put node platformID and countryCode in a shared, configurable location
            val receiver = BasicRole(party.partyID, party.countryCode)
            val requestVariables = OcpiRequestVariables(
                    module = ModuleID.HUB_CLIENT_INFO,
                    interfaceRole = InterfaceRole.RECEIVER,
                    method = HttpMethod.PUT,
                    headers = OcnHeaders(
                            authorization = "Token ${platformRepo.findById(party.platformID).get().auth.tokenB}",
                            requestID = generateUUIDv4Token(),
                            correlationID = generateUUIDv4Token(),
                            sender = sender,
                            receiver = receiver),
                    body = changedClientInfo,
                    urlPath = "${changedClientInfo.countryCode}/${changedClientInfo.partyID}")

            val (url, headers) = routingService.prepareLocalPlatformRequest(requestVariables, proxied = false)

            try {
                httpService.makeOcpiRequest<Unit>(url, headers, requestVariables)
            } catch (e: Exception) { // fire and forget; catch any error and log
                logger.warn("Error notifying $receiver of client info change: ${e.message}")
            }
        }
    }

    /**
     * Send a notification of a ClientInfo change to other nodes on the network
     */
    fun notifyNodesOfClientInfoChange(changedClientInfo: ClientInfo) {
        val requestBodyString = httpService.mapper.writeValueAsString(changedClientInfo)
        val signature = walletService.sign(requestBodyString)

        val nodes = registryService.getNodes(omitMine = true)

        for (node in nodes) {
            try {
                httpService.putOcnClientInfo(node.url, signature, changedClientInfo)
            } catch (e: Exception) { // fire and forget; catch any error and log
                logger.warn("Error notifying $node of client info change: ${e.message}")
            }
        }
    }

    /**
     * Confirm the online status of the client corresponding to a role
     */
    @Async
    fun renewClientConnection(sender: BasicRole) {
        val role = roleRepo.findByCountryCodeAndPartyIDAllIgnoreCase(countryCode = sender.country, partyID = sender.id)
                ?: throw IllegalArgumentException("sender could not be found")

        val client = platformRepo.findById(role.platformID).get()
        client.renewConnection(Instant.now())
        platformRepo.save(client)
    }
}
