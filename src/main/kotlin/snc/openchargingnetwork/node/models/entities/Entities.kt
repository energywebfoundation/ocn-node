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

package snc.openchargingnetwork.node.models.entities

import org.springframework.data.domain.AbstractAggregateRoot
import snc.openchargingnetwork.node.models.events.*
import snc.openchargingnetwork.node.models.ocpi.*
import snc.openchargingnetwork.node.tools.generateUUIDv4Token
import snc.openchargingnetwork.node.tools.getTimestamp
import java.time.Instant
import javax.persistence.*


/**
 * Stores an OCPI platform equivalent to a single OCPI connection to an OCN Node
 */
@Entity
@Table(name = "platforms")
class PlatformEntity(var status: ConnectionStatus = ConnectionStatus.PLANNED,
                     var lastUpdated: String = getTimestamp(),
                     var versionsUrl: String? = null,
                     @Embedded var auth: Auth = Auth(),
                     @Embedded var rules: OcnRules = OcnRules(),
                     @Id @GeneratedValue var id: Long? = null)
        : AbstractAggregateRoot<PlatformEntity>() {

        fun register(roles: Iterable<RoleEntity>) {
                registerEvent(PlatformRegisteredDomainEvent(this, roles))
        }

        fun unregister(roles: Iterable<RoleEntity>) {
                this.status = ConnectionStatus.SUSPENDED
                this.lastUpdated = getTimestamp()
                registerEvent(PlatformUnregisteredDomainEvent(this, roles))
        }

        fun renewConnection(connectionInstant: Instant) {
                this.lastUpdated = getTimestamp(connectionInstant)
                if (this.status != ConnectionStatus.CONNECTED) {
                        this.status = ConnectionStatus.CONNECTED

                        registerEvent(PlatformReconnectedDomainEvent(this))
                }
        }

        fun disconnect(disconnectionInstant: Instant) {
                this.lastUpdated = getTimestamp(disconnectionInstant)
                if (this.status != ConnectionStatus.OFFLINE) {
                        this.status = ConnectionStatus.OFFLINE
                        registerEvent(PlatformDisconnectedDomainEvent(this))
                }
        }
}

/**
 * Tokens for authorization on OCPI party servers
 * - tokenA = generated by admin; used in registration to broker
 * - tokenB = generated by party; used by broker as authorization on party's server
 * - tokenC = generated by broker; used by party for subsequent requests on broker's server
 */
@Embeddable
class Auth(var tokenA: String? = generateUUIDv4Token(),
           var tokenB: String? = null,
           var tokenC: String? = null)

@Embeddable
class OcnRules(@Column(columnDefinition = "boolean default false") var signatures: Boolean = false,
               @Column(columnDefinition = "boolean default false") var blacklist: Boolean = false,
               @Column(columnDefinition = "boolean default false") var whitelist: Boolean = false)

/**
 * Store a role linked to an OCPI platform (i.e. a platform can implement both EMSP and CPO roles)
 */
@Entity
@Table(name = "roles")
class RoleEntity(var platformID: Long,
                 @Enumerated(EnumType.STRING) var role: Role,
                 @Embedded var businessDetails: BusinessDetails,
                 var partyID: String,
                 var countryCode: String,
                 @Id @GeneratedValue var id: Long? = null)


/**
 * Store endpoints associated with an OCPI platform retreived during the Versions/Credentials registration handshake
 */
@Entity
@Table(name = "endpoints")
class EndpointEntity(var platformID: Long,
                     var identifier: String,
                     @Enumerated(EnumType.STRING) var role: InterfaceRole,
                     var url: String,
                     @Id @GeneratedValue var id: Long? = null)


/**
 * Store a resource (URL) which will be proxied by the controller of the request
 */
@Entity
@Table(name = "proxy_resources")
class ProxyResourceEntity(
        @AttributeOverrides(
                AttributeOverride(name = "id", column = Column(name ="sender_id")),
                AttributeOverride(name = "country", column = Column(name ="sender_country"))
        )
        @Embedded
        val sender: BasicRole,

        @AttributeOverrides(
                AttributeOverride(name = "id", column = Column(name = "receiver_id")),
                AttributeOverride(name = "country", column = Column(name = "receiver_country"))
        )
        @Embedded
        var receiver: BasicRole,

        val resource: String,

        val alternativeUID: String? = null,

        @Id @GeneratedValue var id: Long? = null)

@Entity
@Table(name = "ocn_rules_list")
class OcnRulesListEntity(
        val platformID: Long,

        @AttributeOverrides(
                AttributeOverride(name = "id", column = Column(name ="sender_id")),
                AttributeOverride(name = "country", column = Column(name ="sender_country"))
        )
        @Embedded
        val counterparty: BasicRole,

        @Column(columnDefinition = "boolean default false") var cdrs: Boolean = false,
        @Column(columnDefinition = "boolean default false") var chargingprofiles: Boolean = false,
        @Column(columnDefinition = "boolean default false") var commands: Boolean = false,
        @Column(columnDefinition = "boolean default false") var sessions: Boolean = false,
        @Column(columnDefinition = "boolean default false") var locations: Boolean = false,
        @Column(columnDefinition = "boolean default false") var tariffs: Boolean = false,
        @Column(columnDefinition = "boolean default false") var tokens: Boolean = false,


        @Id @GeneratedValue val id: Long? = null
)

/**
 * Store the client info object of a party/role from the network (other nodes; planned party from OCN Registry)
 */
@Entity
@Table(name = "network_client_info")
class NetworkClientInfoEntity(

        @AttributeOverrides(
                AttributeOverride(name = "id", column = Column(name ="sender_id")),
                AttributeOverride(name = "country", column = Column(name ="sender_country"))
        )
        @Embedded
        val party: BasicRole,

        @Enumerated(EnumType.STRING) var role: Role,
        @Enumerated(EnumType.STRING) var status: ConnectionStatus,
        var lastUpdated: String = getTimestamp(),
        @Id @GeneratedValue var id: Long? = null

): AbstractAggregateRoot<NetworkClientInfoEntity>() {
        fun foundNewlyPlannedRole() {
                registerEvent(PlannedRoleFoundDomainEvent(this))
        }
}
