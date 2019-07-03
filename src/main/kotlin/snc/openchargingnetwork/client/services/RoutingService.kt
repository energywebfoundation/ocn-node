package snc.openchargingnetwork.client.services

import org.springframework.stereotype.Service
import org.web3j.crypto.Credentials
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.Sign
import org.web3j.crypto.TransactionEncoder
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameter
import org.web3j.protocol.core.DefaultBlockParameterName
import snc.openchargingnetwork.client.config.Configuration
import snc.openchargingnetwork.client.config.Properties
import snc.openchargingnetwork.client.models.HttpResponse
import snc.openchargingnetwork.client.models.entities.CdrEntity
import snc.openchargingnetwork.client.models.entities.CommandResponseUrlEntity
import snc.openchargingnetwork.client.models.entities.RoleEntity
import snc.openchargingnetwork.client.models.exceptions.OcpiClientGenericException
import snc.openchargingnetwork.client.models.exceptions.OcpiClientInvalidParametersException
import snc.openchargingnetwork.client.models.exceptions.OcpiHubUnknownReceiverException
import snc.openchargingnetwork.client.models.ocpi.*
import snc.openchargingnetwork.client.repositories.*
import snc.openchargingnetwork.client.tools.extractToken
import snc.openchargingnetwork.client.tools.generateUUIDv4Token
import snc.openchargingnetwork.contracts.RegistryFacade
import java.math.BigInteger
import kotlin.reflect.KClass

@Service
class RoutingService(private val platformRepo: PlatformRepository,
                     private val roleRepo: RoleRepository,
                     private val endpointRepo: EndpointRepository,
                     private val cdrRepo: CdrRepository,
                     private val commandResponseUrlRepo: CommandResponseUrlRepository,
                     private val httpService: HttpRequestService,
                     private val registry: RegistryFacade,
                     private val config: Configuration,
                     private val properties: Properties) {

    fun isRoleKnown(role: BasicRole) = roleRepo.existsByCountryCodeAndPartyIDAllIgnoreCase(role.country, role.id)

    fun getPlatformID(role: BasicRole) = roleRepo.findByCountryCodeAndPartyIDAllIgnoreCase(role.country, role.id)!!.platformID

    fun getPlatformEndpoint(platformID: Long?, identifier: String, interfaceRole: InterfaceRole)
            = endpointRepo.findByPlatformIDAndIdentifierAndRole(platformID, identifier, interfaceRole)
            ?: throw OcpiClientInvalidParametersException("Receiver does not support the requested module")

    fun validateSender(authorization: String) {
        platformRepo.findByAuth_TokenC(authorization.extractToken())
                ?: throw OcpiClientInvalidParametersException("Invalid CREDENTIALS_TOKEN_C")
    }

    // check sender is authorized to send requests via this message broker
    fun validateSender(authorization: String, sender: BasicRole) {

        // sender platform exists by auth token
        val senderPlatform = platformRepo.findByAuth_TokenC(authorization.extractToken())
                ?: throw OcpiClientInvalidParametersException("Invalid CREDENTIALS_TOKEN_C")

        // role exists on registered platform
        if (!roleRepo.existsByPlatformIDAndCountryCodeAndPartyIDAllIgnoreCase(senderPlatform.id, sender.country, sender.id)) {
            throw OcpiClientInvalidParametersException("Could not find role on sending platform using OCPI-from-* headers")
        }

    }

    // check sender is authorized on this message broker AND that sender is original client-owned object creator
    fun validateSender(authorization: String, sender: BasicRole, objectCreator: BasicRole) {

        // as above
        this.validateSender(authorization, sender)

        // check sender and client-owned object owner are the same
        if (sender.toLowerCase() != objectCreator.toLowerCase()) {
            throw OcpiClientInvalidParametersException("Client-owned object does not belong to this sender")
        }

    }

    // check sender is authorized on msg broker, sender is creator of object AND object contains the same sender role
    fun validateSender(authorization: String, sender: BasicRole, objectCreator: BasicRole, objectData: BasicRole) {

        // as above
        this.validateSender(authorization, sender, objectCreator)

        if (objectCreator.toLowerCase() != objectData.toLowerCase()) {
            throw OcpiClientInvalidParametersException("Object country_code and party_id do not match request parameters")
        }
    }

    fun makeHeaders(receiverPlatformID: Long?, correlationID: String, sender: BasicRole, receiver: BasicRole): Map<String, String> {
        val token = platformRepo.findById(receiverPlatformID!!).get().auth.tokenB
        return mapOf(
                "Authorization" to "Token $token",
                "X-Request-ID" to generateUUIDv4Token(),
                "X-Correlation-ID" to correlationID,
                "OCPI-from-country-code" to sender.country,
                "OCPI-from-party-id" to sender.id,
                "OCPI-to-country-code" to receiver.country,
                "OCPI-to-party-id" to receiver.id)
    }

    fun makeHeaders(correlationID: String, sender: BasicRole, receiver: BasicRole): Map<String, String> {
        return mapOf(
                "X-Request-ID" to generateUUIDv4Token(),
                "X-Correlation-ID" to correlationID,
                "OCPI-from-country-code" to sender.country,
                "OCPI-from-party-id" to sender.id,
                "OCPI-to-country-code" to receiver.country,
                "OCPI-to-party-id" to receiver.id)
    }

    fun <T: Any> forwardRequest(method: String,
                                url: String,
                                headers: Map<String, String>,
                                params: Map<String, String>? = null,
                                body: Any? = null,
                                expectedDataType: KClass<T>): HttpResponse<T> {

        return httpService.makeRequest(method, url, headers, params, body, expectedDataType)
    }

    fun findBrokerUrl(receiver: BasicRole): String {
        val address = registry.addressOf(receiver.country.toByteArray(), receiver.id.toByteArray()).sendAsync().get()
        val broker = registry.brokerOf(address).sendAsync().get()
        if (broker == "") {
            throw OcpiHubUnknownReceiverException()
        }
        return broker
    }

    fun isRoleKnownOnNetwork(role: BasicRole): Boolean {
        val address = registry.addressOf(role.country.toByteArray(), role.id.toByteArray()).sendAsync().get()
        val broker = registry.brokerOf(address).sendAsync().get()
        return broker != ""
    }

    fun stringify(body: Any): String {
        return httpService.mapper.writeValueAsString(body)
    }

    fun saveCDR(id: String, location: String, sender: BasicRole, receiver: BasicRole) {
        cdrRepo.save(CdrEntity(
                cdrID = id,
                ownerID = receiver.id,
                ownerCountry = receiver.country,
                creatorID = sender.id,
                creatorCountry = sender.country,
                location = location
        ))
    }

    fun findCDR(id: String, sender: BasicRole, receiver: BasicRole): String {
        val result = cdrRepo.findByCdrIDAndOwnerIDAndOwnerCountryAndCreatorIDAndCreatorCountryAllIgnoreCase(
                cdrID = id,
                ownerCountry = receiver.country,
                ownerID = receiver.id,
                creatorCountry = sender.country,
                creatorID = sender.id) ?: throw OcpiClientInvalidParametersException("cdr_id not found")
        return result.location

    }

    fun saveResponseURL(url: String, type: CommandType, sender: BasicRole, receiver: BasicRole): String {
        val uid = generateUUIDv4Token()

        commandResponseUrlRepo.save(CommandResponseUrlEntity(
                url = url,
                type = type,
                uid = uid,
                senderCountry = sender.country,
                senderID = sender.id,
                receiverCountry = receiver.country,
                receiverID = receiver.id))

        return uid
    }

    fun findResponseURL(type: CommandType, uid: String, sender: BasicRole, receiver: BasicRole): String {
        val result = commandResponseUrlRepo.findByUidAndTypeAndSenderIDAndSenderCountryAndReceiverIDAndReceiverCountryAllIgnoreCase(
                uid = uid,
                type = type,
                senderCountry = receiver.country,
                senderID = receiver.id,
                receiverCountry = sender.country,
                receiverID = sender.id) ?: throw OcpiClientInvalidParametersException("Async response for given uid not permitted")
        return result.url
    }

    fun findClientInfo(): Array<ClientInfo> {
        var allClientInfo = arrayOf<ClientInfo>()
        for (platform in platformRepo.findAll()) {
            for (role in roleRepo.findAllByPlatformID(platform.id)) {
                allClientInfo = allClientInfo.plus(ClientInfo(
                        partyID = role.partyID,
                        countryCode = role.countryCode,
                        role = role.role,
                        status = platform.status,
                        lastUpdated = platform.lastUpdated))
            }
        }
        return allClientInfo
    }

    fun writeToRegistry(roles: List<RoleEntity>) {
        for (role in roles) {
            val address = registry.addressOf(role.countryCode.toByteArray(), role.partyID.toByteArray()).sendAsync().get()
            if (address != "0x0000000000000000000000000000000000000000") {
                val broker = registry.brokerOf(address).sendAsync().get()
                if (broker != properties.url) {
                    throw OcpiClientGenericException("Party with party_id=${role.partyID} and country_code=${role.countryCode} already registered on Open Charging Network.")
                } else {
                    return
                }
            }
        }
        for (role in roles) {
            val credentials = Credentials.create(role.privateKey)
            val prefix = "\\x19Ethereum Signed Message:\n32"
            val message = prefix.toByteArray()
                    .plus(role.countryCode.toByteArray())
                    .plus(role.partyID.toByteArray())
                    .plus(properties.url.toByteArray())
            val signedMessage = Sign.signMessage(message, credentials.ecKeyPair)
            println("signedMessage: $signedMessage")
            val tx = registry.register(
                    role.countryCode.toByteArray(),
                    role.partyID.toByteArray(),
                    properties.url,
                    BigInteger(signedMessage.v),
                    signedMessage.r,
                    signedMessage.s).sendAsync().get()
            println(tx)
        }
    }

    fun deleteFromRegistry(roles: List<RoleEntity>) {
        for (role in roles) {
            val credentials = Credentials.create(role.privateKey)
            val prefix = "\\x19Ethereum Signed Message:\n32"
            val message = prefix.toByteArray()
                    .plus(role.countryCode.toByteArray())
                    .plus(role.partyID.toByteArray())
            val signedMessage = Sign.signMessage(message, credentials.ecKeyPair)
            println("signedMessage: $signedMessage")
            val tx = registry.deregister(
                    role.countryCode.toByteArray(),
                    role.partyID.toByteArray(),
                    BigInteger(signedMessage.v),
                    signedMessage.r,
                    signedMessage.s).sendAsync().get()
            println(tx)
        }
    }

}