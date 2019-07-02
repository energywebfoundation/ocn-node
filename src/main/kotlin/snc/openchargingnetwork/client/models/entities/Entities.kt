package snc.openchargingnetwork.client.models.entities

import snc.openchargingnetwork.client.models.ocpi.*
import snc.openchargingnetwork.client.tools.generateUUIDv4Token
import snc.openchargingnetwork.client.tools.getTimestamp
import javax.persistence.*

@Entity
@Table(name = "platforms")
class PlatformEntity(
        var status: ConnectionStatusType = ConnectionStatusType.PLANNED,
        var lastUpdated: String = getTimestamp(),
        var versionsUrl: String? = null,
        @Embedded var auth: Auth = Auth(),
        @Id @GeneratedValue var id: Long? = null
)

// Tokens for authorization on OCPI party servers
//      tokenA = generated by admin; used in registration to broker
//      tokenB = generated by party; used by broker as authorization on party's server
//      tokenC = generated by broker; used by party for subsequent requests on broker's server
@Embeddable
class Auth(
        var tokenA: String? = generateUUIDv4Token(),
        var tokenB: String? = null,
        var tokenC: String? = null
)

@Entity
@Table(name = "roles")
class RoleEntity(
        var platformID: Long,
        @Enumerated(EnumType.STRING) var role: Role,
        @Embedded var businessDetails: BusinessDetails,
        var partyID: String,
        var countryCode: String,
        @Id @GeneratedValue var id: Long? = null
)

@Entity
@Table(name = "endpoints")
class EndpointEntity(
        var platformID: Long,
        var identifier: String,
        @Enumerated(EnumType.STRING) var role: InterfaceRole,
        var url: String,
        @Id @GeneratedValue var id: Long? = null
)

@Entity
@Table(name = "cdrs")
class CdrEntity(
        val cdrID: String,
        val ownerID: String,
        val ownerCountry: String,
        val creatorID: String,
        val creatorCountry: String,
        val location: String,
        @Id @GeneratedValue var id: Long? = null
)

@Entity
@Table(name = "command_response_urls")
class CommandResponseUrlEntity(
        val url: String,
        @Enumerated(EnumType.STRING) var type: CommandType,
        val uid: String,
        val senderID: String,
        val senderCountry: String,
        val receiverID: String,
        val receiverCountry: String,
        @Id @GeneratedValue var id: Long? = null
)