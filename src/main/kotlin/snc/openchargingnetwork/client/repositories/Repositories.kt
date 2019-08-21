/*
    Copyright 2019 Energy Web Foundation

    This file is part of Open Charging Network Client.

    Open Charging Network Client is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Open Charging Network Client is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Open Charging Network Client.  If not, see <https://www.gnu.org/licenses/>.
*/

package snc.openchargingnetwork.client.repositories

import org.springframework.data.repository.CrudRepository
import snc.openchargingnetwork.client.models.entities.*
import snc.openchargingnetwork.client.models.ocpi.BasicRole
import snc.openchargingnetwork.client.models.ocpi.InterfaceRole

interface WalletRepository: CrudRepository<WalletEntity, Long> {}

interface PlatformRepository: CrudRepository<PlatformEntity, Long> {
    fun existsByAuth_TokenA(tokenA: String?): Boolean
    fun existsByAuth_TokenC(tokenC: String?): Boolean
    fun findByAuth_TokenA(tokenA: String?): PlatformEntity?
    fun findByAuth_TokenC(tokenC: String?): PlatformEntity?
}

interface RoleRepository: CrudRepository<RoleEntity, Long> {
    // used in registration to prevent multiple roles of the same country_code/party_id combination
    fun existsByCountryCodeAndPartyIDAllIgnoreCase(countryCode: String, partyID: String): Boolean
    // used to ensure sender's role is registered to a platform on the broker (hub)
    fun existsByPlatformIDAndCountryCodeAndPartyIDAllIgnoreCase(platformID: Long?, countryCode: String, partyID: String): Boolean
    // used in routing to find roles registered with broker (hub)
    fun findByCountryCodeAndPartyIDAllIgnoreCase(countryCode: String, partyID: String): RoleEntity?
    fun findAllByPlatformID(platformID: Long?): Iterable<RoleEntity>
    fun deleteByPlatformID(platformID: Long?)
}

interface EndpointRepository: CrudRepository<EndpointEntity, Long> {
    fun findByPlatformID(platformID: Long?): Iterable<EndpointEntity>
    fun findByPlatformIDAndIdentifierAndRole(platformID: Long?, identifier: String, Role: InterfaceRole): EndpointEntity?
    fun deleteByPlatformID(platformID: Long?)
}

interface ProxyResourceRepository: CrudRepository<ProxyResourceEntity, Long> {
    fun findByIdAndSenderAndReceiver(id: Long?, sender: BasicRole, receiver: BasicRole): ProxyResourceEntity?
}