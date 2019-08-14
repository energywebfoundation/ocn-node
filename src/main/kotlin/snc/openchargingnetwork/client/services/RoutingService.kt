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

import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.web3j.crypto.Keys
import org.web3j.crypto.Sign
import org.web3j.utils.Numeric
import snc.openchargingnetwork.client.models.*
import snc.openchargingnetwork.client.models.entities.CdrEntity
import snc.openchargingnetwork.client.models.entities.CommandResponseUrlEntity
import snc.openchargingnetwork.client.models.exceptions.OcpiClientInvalidParametersException
import snc.openchargingnetwork.client.models.exceptions.OcpiHubConnectionProblemException
import snc.openchargingnetwork.client.models.exceptions.OcpiHubUnknownReceiverException
import snc.openchargingnetwork.client.models.ocpi.*
import snc.openchargingnetwork.client.repositories.*
import snc.openchargingnetwork.client.tools.extractToken
import snc.openchargingnetwork.client.tools.generateUUIDv4Token
import snc.openchargingnetwork.client.tools.urlJoin
import snc.openchargingnetwork.contracts.RegistryFacade
import java.nio.charset.StandardCharsets

@Service
class RoutingService(private val platformRepo: PlatformRepository,
                     private val roleRepo: RoleRepository,
                     private val endpointRepo: EndpointRepository,
                     private val cdrRepo: CdrRepository,
                     private val commandResponseUrlRepo: CommandResponseUrlRepository,
                     private val httpService: HttpRequestService,
                     private val registry: RegistryFacade,
                     private val credentialsService: CredentialsService) {

    fun <T: Any> forwardRequest(module: ModuleID,
                                interfaceRole: InterfaceRole,
                                method: HttpMethod,
                                headers: HubRequestHeaders,
                                urlEncodedParameters: HubRequestParameters? = null,
                                urlPathVariables: String? = null,
                                body: Any? = null,
                                responseBodyType: HubRequestResponseType<T>): HttpResponse<T> {

        // validate OCPI routing headers
        val sender = BasicRole(headers.ocpiFromPartyID, headers.ocpiFromCountryCode)
        val receiver = BasicRole(headers.ocpiToPartyID, headers.ocpiToCountryCode)

        // validate sender has OCPI connection with this OCN Client
        validateSender(headers.authorization!!, sender)

        // check receiver has OCPI connection with this OCN Client
        getPlatformID(receiver)?.let {

            // find the module endpoint as implemented by the receiver
            val endpoint = getPlatformEndpoint(it, module.toString(), interfaceRole)

            // create new headers for the request to be forwarded
            val clientHeaders = makeHeaders(it, headers.correlationID, sender, receiver)

            // forward the request to the "local" receiver (sharing the same OCN Client)
            return httpService.makeRequest(
                    method = method,
                    url = endpoint.url,
                    headers = clientHeaders,
                    params = urlEncodedParameters?.encode(),
                    expectedDataType = responseBodyType.type)
        }

        // look up Client URL of receiver in OCN Registry
        val url = findRemoteClientUrl(receiver)

        // create the OCN message request body
        val clientBody = HubGenericRequest(
                module = module.toString(),
                method = method.toString(),
                role = interfaceRole,
                headers = headers,
                params = urlEncodedParameters,
                path = urlPathVariables,
                body = body,
                expectedResponseType = responseBodyType)

        // forward the request to the remote receiver's OCN client
        return httpService.makeRequest(
                method = HttpMethod.POST,
                url = urlJoin(url, "/ocn/message"),
                headers = mapOf(
                        "X-Request-ID" to generateUUIDv4Token(),
                        "OCN-Signature" to signRequest(clientBody)),
                body = clientBody,
                expectedDataType = responseBodyType.type)
    }


    fun isRoleKnown(role: BasicRole) = roleRepo.existsByCountryCodeAndPartyIDAllIgnoreCase(role.country, role.id)

    fun getPlatformID(role: BasicRole) = roleRepo.findByCountryCodeAndPartyIDAllIgnoreCase(role.country, role.id)?.platformID

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

    fun makeHeaders(requestID: String, correlationID: String, sender: BasicRole, receiver: BasicRole): HubRequestHeaders {
        return HubRequestHeaders(
                requestID = requestID,
                correlationID = correlationID,
                ocpiFromCountryCode = sender.country,
                ocpiFromPartyID = sender.id,
                ocpiToCountryCode = receiver.country,
                ocpiToPartyID = receiver.id)
    }

    fun makeHeaders(receiverPlatformID: Long?, correlationID: String, sender: BasicRole, receiver: BasicRole): Map<String, String> {
        val token = platformRepo.findById(receiverPlatformID!!).get().auth.tokenB
        return mapOf(
                "Authorization" to "Token $token",
                "X-Request-ID" to generateUUIDv4Token(),
                "X-Correlation-ID" to correlationID,
                "OCPI-From-Country-Code" to sender.country,
                "OCPI-From-Party-ID" to sender.id,
                "OCPI-To-Country-Code" to receiver.country,
                "OCPI-To-Party-ID" to receiver.id)
    }

    fun makeHeaders(correlationID: String, sender: BasicRole, receiver: BasicRole): Map<String, String> {
        return mapOf(
                "X-Request-ID" to generateUUIDv4Token(),
                "X-Correlation-ID" to correlationID,
                "OCPI-From-Country-Code" to sender.country,
                "OCPI-From-Party-ID" to sender.id,
                "OCPI-To-Country-Code" to receiver.country,
                "OCPI-To-Party-ID" to receiver.id)
    }

    fun findRemoteClientUrl(receiver: BasicRole): String {
        val url = registry.clientURLOf(receiver.country.toByteArray(), receiver.id.toByteArray()).sendAsync().get()
        if (url == "") {
            throw OcpiHubUnknownReceiverException()
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

    fun signRequest(body: Any): String {
        val dataToSign = stringify(body).toByteArray(StandardCharsets.UTF_8)
        val signature = Sign.signPrefixedMessage(dataToSign, credentialsService.credentials.ecKeyPair)
        return Numeric.cleanHexPrefix(Numeric.toHexString(signature.r)) +
               Numeric.cleanHexPrefix(Numeric.toHexString(signature.s)) +
               Numeric.cleanHexPrefix(Numeric.toHexString(signature.v))
    }

    fun verifyRequest(body: Any, signature: String, sender: BasicRole) {
        val signedRequest = stringify(body).toByteArray(StandardCharsets.UTF_8)
        val cleanSignature = Numeric.cleanHexPrefix(signature)
        val r = cleanSignature.substring(0, 64)
        val s = cleanSignature.substring(64, 128)
        val v = cleanSignature.substring(128, 130)
        val signingKey = Sign.signedPrefixedMessageToKey(signedRequest, Sign.SignatureData(
                Numeric.hexStringToByteArray(v),
                Numeric.hexStringToByteArray(r),
                Numeric.hexStringToByteArray(s)))
        val signingAddress = "0x${Keys.getAddress(signingKey)}"
        val registeredClientAddress = registry.clientAddressOf(
                sender.country.toByteArray(),
                sender.id.toByteArray()).sendAsync().get()
        if (signingAddress != registeredClientAddress) {
            throw OcpiHubConnectionProblemException("Could not verify OCN-Signature of request")
        }
    }

}