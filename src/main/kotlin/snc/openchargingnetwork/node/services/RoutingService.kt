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

package snc.openchargingnetwork.node.services

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import snc.openchargingnetwork.node.models.OcnHeaders
import snc.openchargingnetwork.node.models.OcnMessageHeaders
import snc.openchargingnetwork.node.models.Receiver
import snc.openchargingnetwork.node.models.entities.EndpointEntity
import snc.openchargingnetwork.node.models.entities.OcnRules
import snc.openchargingnetwork.node.models.entities.PlatformEntity
import snc.openchargingnetwork.node.models.entities.ProxyResourceEntity
import snc.openchargingnetwork.node.models.exceptions.OcpiClientGenericException
import snc.openchargingnetwork.node.models.exceptions.OcpiClientInvalidParametersException
import snc.openchargingnetwork.node.models.exceptions.OcpiClientUnknownLocationException
import snc.openchargingnetwork.node.models.exceptions.OcpiHubUnknownReceiverException
import snc.openchargingnetwork.node.models.ocpi.BasicRole
import snc.openchargingnetwork.node.models.ocpi.InterfaceRole
import snc.openchargingnetwork.node.models.ocpi.OcpiRequestVariables
import snc.openchargingnetwork.node.repositories.EndpointRepository
import snc.openchargingnetwork.node.repositories.PlatformRepository
import snc.openchargingnetwork.node.repositories.ProxyResourceRepository
import snc.openchargingnetwork.node.repositories.RoleRepository
import snc.openchargingnetwork.node.tools.extractToken
import snc.openchargingnetwork.node.tools.generateUUIDv4Token
import snc.openchargingnetwork.node.tools.urlJoin

@Service
class RoutingService(private val platformRepo: PlatformRepository,
                     private val roleRepo: RoleRepository,
                     private val endpointRepo: EndpointRepository,
                     private val proxyResourceRepo: ProxyResourceRepository,
                     private val registryService: RegistryService,
                     private val httpService: HttpService,
                     private val walletService: WalletService,
                     private val ocnRulesService: OcnRulesService) {

    /**
     * check database to see if basic role is connected to the node
     */
    fun isRoleKnown(role: BasicRole) = roleRepo.existsByCountryCodeAndPartyIDAllIgnoreCase(role.country, role.id)

    /**
     * get platform by role
     * TODO: review getPlatform* methods below
     */
    fun getPlatform(role: BasicRole): PlatformEntity {
        val platformID = getPlatformID(role)
        return platformRepo.findByIdOrNull(platformID)
                ?: throw IllegalStateException("Platform with id=$platformID does not exist, but has roles.")
    }

    /**
     * get platform ID - used as foreign key in endpoint and roles repositories
     */
    fun getPlatformID(role: BasicRole): Long {
        return roleRepo.findAllByCountryCodeAndPartyIDAllIgnoreCase(role.country, role.id).firstOrNull()?.platformID
                ?: throw OcpiHubUnknownReceiverException("Could not find platform ID of $role")
    }

    /**
     * get the rules (signature, white/blacklist) implemented by a role/platform
     */
    fun getPlatformRules(role: BasicRole): OcnRules {
        val platformID = getPlatformID(role)
        return platformRepo.findByIdOrNull(platformID)?.rules
                ?: throw IllegalStateException("Platform with id=$platformID does not exist, but has roles.")
    }


    /**
     * get OCPI platform endpoint information using platform ID (from above)
     */
    fun getPlatformEndpoint(platformID: Long?, moduleID: String, interfaceRole: InterfaceRole): EndpointEntity {
        return endpointRepo.findByPlatformIDAndIdentifierAndRole(platformID, moduleID, interfaceRole)
                ?: throw OcpiClientInvalidParametersException("Receiver does not support the requested module")
    }


    /**
     * Check sender is known to this node using only the authorization header token
     */
    fun checkSenderKnown(authorization: String) {
        if (!platformRepo.existsByAuth_TokenC(authorization.extractToken())) {
            throw OcpiClientInvalidParametersException("Invalid CREDENTIALS_TOKEN_C")
        }
    }


    /**
     * Check sender is known to this node using the authorization header token and role provided as sender
     */
    fun checkSenderKnown(authorization: String, sender: BasicRole) {

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
     * @return Receiver - defines the whether receiver is LOCAL (on this node) or REMOTE (on different node)
     */
    fun getReceiverType(receiver: BasicRole): Receiver {
        return when {
            isRoleKnown(receiver) -> Receiver.LOCAL
            registryService.isRoleKnown(receiver, belongsToMe = false) -> Receiver.REMOTE
            else -> throw OcpiHubUnknownReceiverException("Receiver not registered on Open Charging Network")
        }
    }

    /**
     * Check receiver has allowed sender to send them messages
     */
    fun checkSenderWhitelisted(sender: BasicRole, receiver: BasicRole, moduleID: String) {
        val platform = getPlatform(receiver)
        val whitelisted = ocnRulesService.isWhitelisted(platform, sender, moduleID)
        if (!whitelisted) {
            throw OcpiClientGenericException("Message receiver not in sender's whitelist.")
        }
    }


    /**
     * Used after validating a receiver: find the url of the local recipient for the given OCPI module/interface
     * and set the correct headers, replacing the X-Request-ID and Authorization token.
     */
    fun prepareLocalPlatformRequest(request: OcpiRequestVariables, proxied: Boolean = false): Pair<String, OcnHeaders> {

        val platformID = getPlatformID(request.headers.receiver)

        val url = when {

            // local sender is requesting a resource/url via a proxy
            // returns the resource behind the proxy
            proxied -> getProxyResource(request.urlPath, request.headers.sender, request.headers.receiver)

            // remote sender is requesting a resource/url via a proxy
            // return the proxied resource as defined by the sender
            request.proxyUID == null && request.proxyResource != null -> request.proxyResource

            // remote sender has defined an identifiable resource to be proxied
            // save the resource and return standard OCPI module URL of recipient
            request.proxyUID != null && request.proxyResource != null -> {
                setProxyResource(
                        resource = request.proxyResource,
                        sender = request.headers.receiver,
                        receiver = request.headers.sender,
                        alternativeUID = request.proxyUID)
                val endpoint = getPlatformEndpoint(platformID, request.resolveModuleId(), request.interfaceRole)
                urlJoin(endpoint.url, request.urlPath)
            }

            // return standard OCPI module URL of recipient
            else -> {
                val endpoint = getPlatformEndpoint(platformID, request.resolveModuleId(), request.interfaceRole) // could replace with request.resolveModuleId()...
                urlJoin(endpoint.url, request.urlPath)
            }

        }

        val tokenB = platformRepo.findById(platformID).get().auth.tokenB

        val headers = request.headers.copy(authorization = "Token $tokenB", requestID = generateUUIDv4Token())

        return Pair(url, headers)
    }


    /**
     * Used after validating a receiver: find the remote recipient's OCN Node server address (url) and prepare
     * the OCN message body and headers (containing the new X-Request-ID and the signature of the OCN message body).
     */
    fun prepareRemotePlatformRequest(request: OcpiRequestVariables, proxied: Boolean = false, alterBody: ((url: String) -> OcpiRequestVariables)? = null): Triple<String, OcnMessageHeaders, String> {

        val url = registryService.getRemoteNodeUrlOf(request.headers.receiver)

        var modifiedBody = request.copy()

        // if callback function present, use it
        alterBody?.let { modifiedBody = it(url) }

        // if proxied request, add the proxy resource to the body
        if (proxied) {
            modifiedBody = modifiedBody.run {
                copy(proxyResource = getProxyResource(urlPath, headers.sender, headers.receiver))
            }
        }

        // strip authorization
        modifiedBody = modifiedBody.copy(headers = modifiedBody.headers.copy(authorization = ""))

        val bodyString = httpService.mapper.writeValueAsString(modifiedBody)

        val headers = OcnMessageHeaders(
                requestID = generateUUIDv4Token(),
                signature = walletService.sign(bodyString))

        return Triple(url, headers, bodyString)
    }


    /**
     * Get a generic proxy resource by its ID
     */
    fun getProxyResource(id: String?, sender: BasicRole, receiver: BasicRole): String {
        try {
            id?.let {
                // first check by proxy UID (sender and receiver should be reversed in this case) then by ID
                return proxyResourceRepo.findByAlternativeUIDAndSenderAndReceiver(it, sender, receiver)?.resource
                        ?: proxyResourceRepo.findByIdAndSenderAndReceiver(it.toLong(), sender, receiver)?.resource
                        ?: throw Exception()
            }
            throw Exception()
        } catch (_: Exception) {
            throw OcpiClientUnknownLocationException("Proxied resource not found")
        }
    }


    /**
     * Save a given resource in order to proxy it (identified by the entity's generated ID).
     */
    fun setProxyResource(resource: String, sender: BasicRole, receiver: BasicRole, alternativeUID: String? = null): String {
        val proxyResource = ProxyResourceEntity(
                resource = resource,
                sender = sender,
                receiver = receiver,
                alternativeUID = alternativeUID)
        val savedEntity = proxyResourceRepo.save(proxyResource)
        return alternativeUID ?: savedEntity.id!!.toString()
    }


    /**
     * Delete a resource once used (TODO: define what resource can/should be deleted)
     */
    fun deleteProxyResource(resourceID: String) {
        proxyResourceRepo.deleteById(resourceID.toLong())
    }

}
