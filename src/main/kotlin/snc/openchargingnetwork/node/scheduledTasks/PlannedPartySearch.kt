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

import com.olisystems.ocnregistryv2_0.OcnRegistry
import org.web3j.crypto.Credentials
import snc.openchargingnetwork.node.config.NodeProperties
import snc.openchargingnetwork.node.models.NewRegistryPartyDetails
import snc.openchargingnetwork.node.models.entities.NetworkClientInfoEntity
import snc.openchargingnetwork.node.models.ocpi.BasicRole
import snc.openchargingnetwork.node.models.ocpi.ConnectionStatus
import snc.openchargingnetwork.node.models.ocpi.Role
import snc.openchargingnetwork.node.repositories.NetworkClientInfoRepository
import snc.openchargingnetwork.node.repositories.RoleRepository
import snc.openchargingnetwork.node.tools.checksum


class PlannedPartySearch(private val registry: OcnRegistry,
                         private val roleRepo: RoleRepository,
                         private val networkClientInfoRepo: NetworkClientInfoRepository,
                         private val properties: NodeProperties): Runnable {

    override fun run() {
        val myAddress = Credentials.create(properties.privateKey).address.checksum()

        // registry.getParties() returns list of party ethereum addresses which can be used to get full party details
        val plannedParties = registry.parties.sendAsync().get()
                .asSequence()
                .map {
                    val details = registry.getPartyDetailsByAddress(it as String).sendAsync().get()
                    val (address, country, id, roles, roleIndex, operator, name, url, active) = details
                    NewRegistryPartyDetails(
                        nodeOperator = operator.checksum(),
                        BasicRole(
                            country = country.toString(Charsets.UTF_8),
                            id = id.toString(Charsets.UTF_8)),
                        roles = roles.map { index -> Role.getByIndex(index) }
                        )
                }
                .filter {
                    val isMyParty = it.nodeOperator == myAddress
                    isMyParty
                }
                .filter {
                    val partyHasBeenDeleted = it.nodeOperator == "0x0000000000000000000000000000000000000000"
                    !partyHasBeenDeleted
                }
                .filter {
                    // Doing completed registration check after deleted party check as null countryCode and partyId
                    // from deleted parties may cause an issue with query for postgresql
                    val hasCompletedRegistration = roleRepo.existsByCountryCodeAndPartyIDAllIgnoreCase(it.party.country, it.party.id)
                    !hasCompletedRegistration
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