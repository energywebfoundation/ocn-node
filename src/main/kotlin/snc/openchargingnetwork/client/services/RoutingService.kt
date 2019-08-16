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

import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import snc.openchargingnetwork.client.config.Properties
import snc.openchargingnetwork.client.models.*
import snc.openchargingnetwork.client.models.entities.ProxyResourceEntity
import snc.openchargingnetwork.client.models.exceptions.OcpiClientInvalidParametersException
import snc.openchargingnetwork.client.models.exceptions.OcpiClientUnknownLocationException
import snc.openchargingnetwork.client.models.exceptions.OcpiHubUnknownReceiverException
import snc.openchargingnetwork.client.models.ocpi.*
import snc.openchargingnetwork.client.repositories.*
import snc.openchargingnetwork.client.tools.extractToken
import snc.openchargingnetwork.client.tools.generateUUIDv4Token
import snc.openchargingnetwork.client.tools.urlJoin
import snc.openchargingnetwork.contracts.RegistryFacade

@Service
class RoutingService(private val platformRepo: PlatformRepository,
                     private val roleRepo: RoleRepository,
                     private val endpointRepo: EndpointRepository,
                     private val proxyResourceRepo: ProxyResourceRepository,
                     private val registry: RegistryFacade,
                     private val httpService: HttpRequestService,
                     private val credentialsService: CredentialsService,
                     private val properties: Properties) {


    fun <T: Any> prepareLocalPlatformRequest(request: OcpiRequestVariables<T>, proxied: Boolean = false): Pair<String, OcpiRequestHeaders> {

        val platformID = getPlatformID(request.receiver)

        val url = if (proxied) {
            getProxyResource(request.urlPathVariables, request.sender, request.receiver)
        } else {
            val endpoint = getPlatformEndpoint(platformID, request.module, request.interfaceRole)
            urlJoin(endpoint.url, request.urlPathVariables)
        }

        val tokenB = platformRepo.findById(platformID).get().auth.tokenB
        val headers = OcpiRequestHeaders(
                authorization = "Token $tokenB",
                requestID = generateUUIDv4Token(),
                correlationID = request.correlationID,
                ocpiFromCountryCode = request.sender.country,
                ocpiFromPartyID = request.sender.id,
                ocpiToCountryCode = request.receiver.country,
                ocpiToPartyID = request.receiver.id)

        return Pair(url, headers)
    }


    fun <T: Any> prepareRemotePlatformRequest(request: OcpiRequestVariables<T>, proxied: Boolean = false): Triple<String, OcnMessageHeaders, OcnMessageRequestBody<T>> {

        val url = getRemoteClientUrl(request.receiver)

        val body = OcnMessageRequestBody(
                method = request.method,
                interfaceRole = request.interfaceRole,
                module = request.module,
                headers = OcpiRequestHeaders(
                        requestID = request.requestID,
                        correlationID = request.correlationID,
                        ocpiFromCountryCode = request.sender.country,
                        ocpiFromPartyID = request.sender.id,
                        ocpiToCountryCode = request.receiver.country,
                        ocpiToPartyID = request.receiver.id),
                urlPathVariables = request.urlPathVariables,
                urlEncodedParameters = request.urlEncodedParameters,
                body = request.body,
                expectedResponseType = request.expectedResponseType)

        if (proxied) {
            body.proxyResource = getProxyResource(request.urlPathVariables, request.sender, request.receiver)
        }

        val headers = OcnMessageHeaders(
                requestID = generateUUIDv4Token(),
                signature = credentialsService.signRequest(stringify(body)))

        return Triple(url, headers, body)
    }


    fun proxyPaginationHeaders(responseHeaders: Map<String, String>, proxyEndpoint: String, sender: BasicRole, receiver: BasicRole): HttpHeaders {
        val headers = HttpHeaders()
        responseHeaders["Link"]?.let {
            val id = setProxyResource(it, sender, receiver)
            val link = urlJoin(properties.url, proxyEndpoint, id.toString())
            headers.add("Link", "$link; rel=\"next\"")
        }
        responseHeaders["X-Total-Count"]?.let { headers.add("X-Total-Count", it) }
        responseHeaders["X-Limit"]?.let { headers.add("X-Limit", it) }
        return headers
    }


    fun getProxyResource(id: String?, sender: BasicRole, receiver: BasicRole): String {
        id?.let {
            return proxyResourceRepo.findByIdOrNull(id.toLong())?.resource
                    ?: throw OcpiClientUnknownLocationException("Proxied resource not found")
        }
        throw OcpiClientUnknownLocationException("Error fetching proxied resource: internal platform ID not found")
    }


    fun setProxyResource(resource: String, sender: BasicRole, receiver: BasicRole): Long {
        val proxyResource = ProxyResourceEntity(
                resource = resource,
                sender = sender,
                receiver = receiver)
        val savedEntity = proxyResourceRepo.save(proxyResource)
        return savedEntity.id!!
    }


    fun deleteProxyResource(resourceID: String) {
        proxyResourceRepo.deleteById(resourceID.toLong())
    }


    fun isRoleKnown(role: BasicRole) = roleRepo.existsByCountryCodeAndPartyIDAllIgnoreCase(role.country, role.id)

    fun getPlatformID(role: BasicRole) = roleRepo.findByCountryCodeAndPartyIDAllIgnoreCase(role.country, role.id)?.platformID
            ?: throw OcpiHubUnknownReceiverException("Could not find platform ID of $role")

    fun getPlatformEndpoint(platformID: Long?, module: ModuleID, interfaceRole: InterfaceRole)
            = endpointRepo.findByPlatformIDAndIdentifierAndRole(platformID, module.value, interfaceRole)
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

    // check recipient exists local/remote
    fun validateReceiver(receiver: BasicRole): OcpiRequestType {
        return when {
            isRoleKnown(receiver) -> OcpiRequestType.LOCAL
            isRoleKnownOnNetwork(receiver) -> OcpiRequestType.REMOTE
            else -> throw OcpiHubUnknownReceiverException("Receiver not registered on Open Charging Network")
        }
    }

//    fun makeHeaders(requestID: String, correlationID: String, sender: BasicRole, receiver: BasicRole): OcpiRequestHeaders {
//        return OcpiRequestHeaders(
//                requestID = requestID,
//                correlationID = correlationID,
//                ocpiFromCountryCode = sender.country,
//                ocpiFromPartyID = sender.id,
//                ocpiToCountryCode = receiver.country,
//                ocpiToPartyID = receiver.id)
//    }

//    fun makeHeaders(receiverPlatformID: Long?, correlationID: String, sender: BasicRole, receiver: BasicRole): Map<String, String> {
//        val token = platformRepo.findById(receiverPlatformID!!).get().auth.tokenB
//        return mapOf(
//                "Authorization" to "Token $token",
//                "X-Request-ID" to generateUUIDv4Token(),
//                "X-Correlation-ID" to correlationID,
//                "OCPI-From-Country-Code" to sender.country,
//                "OCPI-From-Party-ID" to sender.id,
//                "OCPI-To-Country-Code" to receiver.country,
//                "OCPI-To-Party-ID" to receiver.id)
//    }
//
//    fun makeHeaders(correlationID: String, sender: BasicRole, receiver: BasicRole): Map<String, String> {
//        return mapOf(
//                "X-Request-ID" to generateUUIDv4Token(),
//                "X-Correlation-ID" to correlationID,
//                "OCPI-From-Country-Code" to sender.country,
//                "OCPI-From-Party-ID" to sender.id,
//                "OCPI-To-Country-Code" to receiver.country,
//                "OCPI-To-Party-ID" to receiver.id)
//    }

    fun getRemoteClientUrl(receiver: BasicRole): String {
        val url = registry.clientURLOf(receiver.country.toByteArray(), receiver.id.toByteArray()).sendAsync().get()
        if (url == "") {
            throw OcpiHubUnknownReceiverException("Recipient not registered on OCN")
        }
        return url
    }

    fun isRoleKnownOnNetwork(role: BasicRole): Boolean {
        val broker = registry.clientURLOf(role.country.toByteArray(), role.id.toByteArray()).sendAsync().get()
        return broker != ""
    }

    fun stringify(body: Any): String {
        return httpService.mapper.writeValueAsString(body)
    }

//    fun saveCDR(id: String, location: String, sender: BasicRole, receiver: BasicRole) {
//        cdrRepo.save(CdrEntity(
//                cdrID = id,
//                ownerID = receiver.id,
//                ownerCountry = receiver.country,
//                creatorID = sender.id,
//                creatorCountry = sender.country,
//                location = location
//        ))
//    }
//
//    fun findCDR(id: String, sender: BasicRole, receiver: BasicRole): String {
//        val result = cdrRepo.findByCdrIDAndOwnerIDAndOwnerCountryAndCreatorIDAndCreatorCountryAllIgnoreCase(
//                cdrID = id,
//                ownerCountry = receiver.country,
//                ownerID = receiver.id,
//                creatorCountry = sender.country,
//                creatorID = sender.id) ?: throw OcpiClientInvalidParametersException("cdr_id not found")
//        return result.location
//
//    }

//    fun saveResponseURL(url: String, type: CommandType, sender: BasicRole, receiver: BasicRole): String {
//        val uid = generateUUIDv4Token()
//
//        commandResponseUrlRepo.save(CommandResponseUrlEntity(
//                url = url,
//                type = type,
//                uid = uid,
//                senderCountry = sender.country,
//                senderID = sender.id,
//                receiverCountry = receiver.country,
//                receiverID = receiver.id))
//
//        return uid
//    }
//
//    fun findResponseURL(type: CommandType, uid: String, sender: BasicRole, receiver: BasicRole): String {
//        val result = commandResponseUrlRepo.findByUidAndTypeAndSenderIDAndSenderCountryAndReceiverIDAndReceiverCountryAllIgnoreCase(
//                uid = uid,
//                type = type,
//                senderCountry = receiver.country,
//                senderID = receiver.id,
//                receiverCountry = sender.country,
//                receiverID = sender.id) ?: throw OcpiClientInvalidParametersException("Async response for given uid not permitted")
//        return result.url
//    }

    // TODO: create client info service (polls status PUSH requests)
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

}