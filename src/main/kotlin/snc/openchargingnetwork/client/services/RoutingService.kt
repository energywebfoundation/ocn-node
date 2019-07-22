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

package snc.openchargingnetwork.client.services

import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.stereotype.Service
import org.web3j.crypto.*
import org.web3j.crypto.Credentials
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
import java.nio.charset.StandardCharsets
import kotlin.reflect.KClass

@Service
class RoutingService(private val platformRepo: PlatformRepository,
                     private val roleRepo: RoleRepository,
                     private val endpointRepo: EndpointRepository,
                     private val cdrRepo: CdrRepository,
                     private val commandResponseUrlRepo: CommandResponseUrlRepository,
                     private val httpService: HttpRequestService,
                     private val registry: RegistryFacade,
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
        var jsonBody: Map<String,Any>? = null
        if (body != null) {
            val jsonString = httpService.mapper.writeValueAsString(body)
            println(jsonString)
            jsonBody = httpService.mapper.readValue(jsonString)
        }
        return httpService.makeRequest(method, url, headers, params, jsonBody, expectedDataType)
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

    fun findClientInfo(): List<ClientInfo> {
        val allClientInfo = mutableListOf<ClientInfo>()
        for (platform in platformRepo.findAll()) {
            for (role in roleRepo.findAllByPlatformID(platform.id)) {
                allClientInfo.add(ClientInfo(
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
        val rolesToRegister = mutableListOf<RoleEntity>()
        for (role in roles) {
            val address = registry.addressOf(role.countryCode.toByteArray(), role.partyID.toByteArray()).sendAsync().get()
            if (address != "0x0000000000000000000000000000000000000000") {
                val broker = registry.brokerOf(address).sendAsync().get()
                if (broker != properties.url) {
                    throw OcpiClientGenericException("Party with party_id=${role.partyID} and country_code=${role.countryCode} already registered on Open Charging Network.")
                }
            } else {
                rolesToRegister.add(role)
            }
        }
        for (role in rolesToRegister) {
            val credentials = Credentials.create(role.privateKey)
            println("${role.countryCode} ${role.partyID}: ${role.privateKey.length}")
            val message = role.countryCode + role.partyID + properties.url
            val hash = Hash.sha3(message.toByteArray(StandardCharsets.UTF_8))
            val signature = Sign.signPrefixedMessage(hash, credentials.ecKeyPair)
            val tx = registry.register(
                    role.countryCode.toByteArray(),
                    role.partyID.toByteArray(),
                    properties.url,
                    BigInteger(signature.v),
                    signature.r,
                    signature.s).sendAsync().get()
            println("Registered ${role.countryCode} ${role.partyID}: $tx")
        }
    }

    fun deleteFromRegistry(roles: List<RoleEntity>) {
        for (role in roles) {
            val credentials = Credentials.create(role.privateKey)
            val message = role.countryCode + role.partyID
            val hash = Hash.sha3(message.toByteArray(StandardCharsets.UTF_8))
            val signature = Sign.signPrefixedMessage(hash, credentials.ecKeyPair)
            val tx = registry.deregister(
                    role.countryCode.toByteArray(),
                    role.partyID.toByteArray(),
                    BigInteger(signature.v),
                    signature.r,
                    signature.s).sendAsync().get()
            println("Deregistered ${role.countryCode} ${role.partyID}: $tx")
        }
    }

}