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

package snc.openchargingnetwork.node.listeners

import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener
import snc.openchargingnetwork.node.models.entities.NetworkClientInfoEntity
import snc.openchargingnetwork.node.models.entities.PlatformEntity
import snc.openchargingnetwork.node.models.entities.RoleEntity
import snc.openchargingnetwork.node.models.events.*
import snc.openchargingnetwork.node.models.ocpi.*
import snc.openchargingnetwork.node.repositories.RoleRepository
import snc.openchargingnetwork.node.services.HubClientInfoService

@Component
class HubClientInfoListener(private val hubClientInfoService: HubClientInfoService,
                            private val roleRepo: RoleRepository) {

    @Async
    @TransactionalEventListener
    fun handlePlatformRegisteredDomainEvent(event: PlatformRegisteredDomainEvent) {
        notifyNetworkOfChanges(event.platform, event.roles)
    }

    @Async
    @TransactionalEventListener
    fun handlePlatformUnregisteredDomainEvent(event: PlatformUnregisteredDomainEvent) {
        notifyNetworkOfChanges(event.platform, event.roles)
    }

    @Async
    @TransactionalEventListener
    fun handlePlatformReconnectedDomainEvent(event: PlatformReconnectedDomainEvent) {
        val roles = roleRepo.findAllByPlatformID(event.platform.id)
        notifyNetworkOfChanges(event.platform, roles)
    }

    @Async
    @TransactionalEventListener
    fun handlePlatformDisconnectedDomainEvent(event: PlatformDisconnectedDomainEvent) {
        val roles = roleRepo.findAllByPlatformID(event.platform.id)
        notifyNetworkOfChanges(event.platform, roles)
    }

    @Async
    @TransactionalEventListener
    fun handlePlannedRoleFoundDomainEvent(event: PlannedRoleFoundDomainEvent) {
        notifyNetworkOfNewlyPlannedRole(event.role)
    }

    /**
     * Sends out ClientInfo updates to locally connected parties and nodes on network
     */
    private fun notifyNetworkOfChanges(changedPlatform: PlatformEntity, changedRoles: Iterable<RoleEntity>) {
        for (platformRole in changedRoles) {
            val updatedClientInfo = ClientInfo(
                    partyID = platformRole.partyID,
                    countryCode = platformRole.countryCode,
                    role = platformRole.role,
                    status = changedPlatform.status,
                    lastUpdated = changedPlatform.lastUpdated)

            val parties = hubClientInfoService.getPartiesToNotifyOfClientInfoChange(changedPlatform, updatedClientInfo)
            hubClientInfoService.notifyPartiesOfClientInfoChange(parties, updatedClientInfo)

            // TODO: handle connection issues
            hubClientInfoService.notifyNodesOfClientInfoChange(updatedClientInfo)
        }
    }

    private fun notifyNetworkOfNewlyPlannedRole(plannedRole: NetworkClientInfoEntity) {
        val clientInfo = ClientInfo(
                partyID = plannedRole.party.id,
                countryCode = plannedRole.party.country,
                role = plannedRole.role,
                status = ConnectionStatus.PLANNED,
                lastUpdated = plannedRole.lastUpdated)
        val parties = hubClientInfoService.getPartiesToNotifyOfClientInfoChange(clientInfo = clientInfo)
        hubClientInfoService.notifyPartiesOfClientInfoChange(parties, clientInfo)

        hubClientInfoService.notifyNodesOfClientInfoChange(clientInfo)
    }

}