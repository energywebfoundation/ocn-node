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
import snc.openchargingnetwork.client.models.entities.EndpointEntity
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
                     private val httpService: HttpService,
                     private val walletService: WalletService,
                     private val properties: Properties) {


    /**
     * serialize a data class (with @JsonProperty annotations) as a JSON string
     */
    private fun stringify(body: Any): String {
        return httpService.mapper.writeValueAsString(body)
    }


    /**
     * check database to see if basic role is connected to the client
     */
    fun isRoleKnown(role: BasicRole) = roleRepo.existsByCountryCodeAndPartyIDAllIgnoreCase(role.country, role.id)


    /**
     * check OCN registry to see if basic role is registered
     */
    fun isRoleKnownOnNetwork(role: BasicRole): Boolean {
        val broker = registry.clientURLOf(role.country.toByteArray(), role.id.toByteArray()).sendAsync().get()
        return broker != ""
    }


    /**
     * get platform ID - used as foreign key in endpoint and roles repositories
     */
    fun getPlatformID(role: BasicRole): Long {
        return roleRepo.findByCountryCodeAndPartyIDAllIgnoreCase(role.country, role.id)?.platformID
                ?: throw OcpiHubUnknownReceiverException("Could not find platform ID of $role")
    }


    /**
     * get OCPI platform endpoint information using platform ID (from above)
     */
    fun getPlatformEndpoint(platformID: Long?, module: ModuleID, interfaceRole: InterfaceRole): EndpointEntity {
        return endpointRepo.findByPlatformIDAndIdentifierAndRole(platformID, module.id, interfaceRole)
                ?: throw OcpiClientInvalidParametersException("Receiver does not support the requested module")
    }


    /**
     * get the OCN client URL as registered by the basic role in the OCN Registry
     */
    fun getRemoteClientUrl(receiver: BasicRole): String {
        val url = registry.clientURLOf(receiver.country.toByteArray(), receiver.id.toByteArray()).sendAsync().get()
        if (url == "") {
            throw OcpiHubUnknownReceiverException("Recipient not registered on OCN")
        }
        return url
    }


    /**
     * Check sender is known to this client using only the authorization header token
     */
    fun validateSender(authorization: String) {
        // TODO: check using existsByAuth_TokenC
        platformRepo.findByAuth_TokenC(authorization.extractToken())
                ?: throw OcpiClientInvalidParametersException("Invalid CREDENTIALS_TOKEN_C")
    }


    /**
     * Check sender is known to this client using the authorization header token and role provided as sender
     */
    fun validateSender(authorization: String, sender: BasicRole) {

        // sender platform exists by auth token
        val senderPlatform = platformRepo.findByAuth_TokenC(authorization.extractToken())
                ?: throw OcpiClientInvalidParametersException("Invalid CREDENTIALS_TOKEN_C")

        // role exists on registered platform
        if (!roleRepo.existsByPlatformIDAndCountryCodeAndPartyIDAllIgnoreCase(senderPlatform.id, sender.country, sender.id)) {
            throw OcpiClientInvalidParametersException("Could not find role on sending platform using OCPI-from-* headers")
        }
    }


    /**
     * Check receiver is registered on the Open Charging Network / known locally via database
     * @return OcpiRequestType - defines the type of request the client should make
     */
    fun validateReceiver(receiver: BasicRole): OcpiRequestType {
        return when {
            isRoleKnown(receiver) -> OcpiRequestType.LOCAL
            isRoleKnownOnNetwork(receiver) -> OcpiRequestType.REMOTE
            else -> throw OcpiHubUnknownReceiverException("Receiver not registered on Open Charging Network")
        }
    }


    /**
     * Used after validating a receiver: find the url of the local recipient for the given OCPI module/interface
     * and set the correct headers, replacing the X-Request-ID and Authorization token.
     */
    fun prepareLocalPlatformRequest(request: OcpiRequestVariables, proxied: Boolean = false): Pair<String, OcpiRequestHeaders> {

        val platformID = getPlatformID(request.receiver)

        val url = when {
            proxied -> getProxyResource(request.urlPathVariables, request.sender, request.receiver)
            request.proxyResource != null -> request.proxyResource
            else -> {
                val endpoint = getPlatformEndpoint(platformID, request.module, request.interfaceRole)
                urlJoin(endpoint.url, request.urlPathVariables)
            }
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


    /**
     * Used after validating a receiver: find the remote recipient's OCN Client server address (url) and prepare
     * the OCN message body and headers (containing the new X-Request-ID and the signature of the OCN message body).
     */
    fun prepareRemotePlatformRequest(request: OcpiRequestVariables, proxied: Boolean = false): Triple<String, OcnMessageHeaders, OcnMessageRequestBody> {

        val url = getRemoteClientUrl(request.receiver)

        // TODO: reuse OcpiRequestVariables / with headers
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
                signature = walletService.sign(stringify(body)))

        return Triple(url, headers, body)
    }


    /**
     * Proxy the Link header in paginated requests (i.e. GET sender cdrs, sessions, tariffs, tokens) so that the
     * requesting platform is able to request the next page without needing authorization on the recipient's
     * system.
     */
    fun proxyPaginationHeaders(request: OcpiRequestVariables, responseHeaders: Map<String, String>): HttpHeaders {
        val headers = HttpHeaders()
        responseHeaders["Link"]?.let {
            val id = setProxyResource(it, request.sender, request.receiver)
            val proxyPaginationEndpoint = "/ocpi/${request.interfaceRole.id}/2.2/${request.module.id}/page"
            val link = urlJoin(properties.url, proxyPaginationEndpoint, id.toString())
            headers.set("Link", "$link; rel=\"next\"")
        }
        responseHeaders["X-Total-Count"]?.let { headers.set("X-Total-Count", it) }
        responseHeaders["X-Limit"]?.let { headers.set("X-Limit", it) }
        return headers
    }


    /**
     * Get a generic proxy resource by its ID
     */
    fun getProxyResource(id: String?, sender: BasicRole, receiver: BasicRole): String {
        id?.let {
            // TODO: check by sender/receiver (roles are reversed)
            return proxyResourceRepo.findByIdOrNull(id.toLong())?.resource
                    ?: throw OcpiClientUnknownLocationException("Proxied resource not found")
        }
        throw OcpiClientUnknownLocationException("Proxied resource not found")
    }


    /**
     * Save a given resource in order to proxy it (identified by the entity's generated ID).
     */
    fun setProxyResource(resource: String, sender: BasicRole, receiver: BasicRole): Long {
        val proxyResource = ProxyResourceEntity(
                resource = resource,
                sender = sender,
                receiver = receiver)
        val savedEntity = proxyResourceRepo.save(proxyResource)
        return savedEntity.id!!
    }


    /**
     * Delete a resource once used (TODO: define what resource can/should be deleted)
     */
    fun deleteProxyResource(resourceID: String) {
        proxyResourceRepo.deleteById(resourceID.toLong())
    }

}