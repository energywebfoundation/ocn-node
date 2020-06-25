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

package snc.openchargingnetwork.node.scheduledTasks

import org.web3j.crypto.Credentials
import snc.openchargingnetwork.contracts.Registry
import snc.openchargingnetwork.node.config.NodeProperties
import snc.openchargingnetwork.node.models.RegistryPartyDetails
import snc.openchargingnetwork.node.models.entities.NetworkClientInfoEntity
import snc.openchargingnetwork.node.models.ocpi.BasicRole
import snc.openchargingnetwork.node.models.ocpi.ConnectionStatus
import snc.openchargingnetwork.node.models.ocpi.Role
import snc.openchargingnetwork.node.repositories.NetworkClientInfoRepository
import snc.openchargingnetwork.node.repositories.RoleRepository
import snc.openchargingnetwork.node.tools.checksum


class PlannedPartySearch(private val registry: Registry,
                         private val roleRepo: RoleRepository,
                         private val networkClientInfoRepo: NetworkClientInfoRepository,
                         private val properties: NodeProperties): Runnable {

    override fun run() {
        val myAddress = Credentials.create(properties.privateKey).address.checksum()

        // registry.getParties() returns list of party ethereum addresses which can be used to get full party details
        val plannedParties = registry.parties.sendAsync().get()
                .asSequence()
                .map {
                    val (country, id, _, _, roles, operator, _) = registry.getPartyDetailsByAddress(it as String).sendAsync().get()
                    RegistryPartyDetails(
                            BasicRole(
                                    country = country.toString(Charsets.UTF_8),
                                    id = id.toString(Charsets.UTF_8)),
                            roles = roles.map { index -> Role.getByIndex(index) },
                            nodeOperator = operator.checksum())
                }
                .filter {
                    val isMyParty = it.nodeOperator == myAddress
                    val hasCompletedRegistration = roleRepo.existsByCountryCodeAndPartyIDAllIgnoreCase(it.party.country, it.party.id)
                    isMyParty && !hasCompletedRegistration
                }

        for (party in plannedParties) {
            for (role in party.roles) {
                if (!networkClientInfoRepo.existsByPartyAndRole(party.party, role)) {
                    val networkClientInfo = NetworkClientInfoEntity(
                            party = party.party.toUpperCase(),
                            role = role,
                            status = ConnectionStatus.PLANNED)
                    networkClientInfo.foundNewlyPlannedRole()
                    networkClientInfoRepo.save(networkClientInfo)
                }
            }
        }

    }

}