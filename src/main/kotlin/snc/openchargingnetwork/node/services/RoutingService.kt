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
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import snc.openchargingnetwork.node.config.NodeProperties
import snc.openchargingnetwork.node.models.*
import snc.openchargingnetwork.node.models.entities.EndpointEntity
import snc.openchargingnetwork.node.models.entities.ProxyResourceEntity
import snc.openchargingnetwork.node.models.ocpi.*
import snc.openchargingnetwork.node.repositories.*
import snc.openchargingnetwork.node.tools.extractNextLink
import snc.openchargingnetwork.node.tools.extractToken
import snc.openchargingnetwork.node.tools.generateUUIDv4Token
import snc.openchargingnetwork.node.tools.urlJoin
import snc.openchargingnetwork.contracts.RegistryFacade
import snc.openchargingnetwork.node.models.entities.OcnRules
import snc.openchargingnetwork.node.models.entities.PlatformEntity
import snc.openchargingnetwork.node.models.exceptions.*
import java.lang.Exception

@Service
class RoutingService(private val platformRepo: PlatformRepository,
                     private val roleRepo: RoleRepository,
                     private val endpointRepo: EndpointRepository,
                     private val proxyResourceRepo: ProxyResourceRepository,
                     private val ocnRulesListRepo: OcnRulesListRepository,
                     private val registry: RegistryFacade,
                     private val httpService: HttpService,
                     private val walletService: WalletService,
                     private val properties: NodeProperties) {


    /**
     * serialize a data class (with @JsonProperty annotations) as a JSON string
     */
    private fun stringify(body: Any): String {
        return httpService.mapper.writeValueAsString(body)
    }


    /**
     * check database to see if basic role is connected to the node
     */
    fun isRoleKnown(role: BasicRole) = roleRepo.existsByCountryCodeAndPartyIDAllIgnoreCase(role.country, role.id)


    /**
     * check OCN registry to see if basic role is registered
     */
    fun isRoleKnownOnNetwork(role: BasicRole, belongsToMe: Boolean = true): Boolean {
        val country = role.country.toByteArray()
        val id = role.id.toByteArray()

        val nodeServerURL = registry.nodeURLOf(country, id).sendAsync().get()
        if (belongsToMe) {
            val ethAddress = registry.nodeAddressOf(country, id).sendAsync().get()
            return nodeServerURL == properties.url && ethAddress == walletService.address
        }

        return nodeServerURL != ""
    }

    /**
     * get platform by role
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
        return roleRepo.findByCountryCodeAndPartyIDAllIgnoreCase(role.country, role.id)?.platformID
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
    fun getPlatformEndpoint(platformID: Long?, module: ModuleID, interfaceRole: InterfaceRole): EndpointEntity {
        return endpointRepo.findByPlatformIDAndIdentifierAndRole(platformID, module.id, interfaceRole)
                ?: throw OcpiClientInvalidParametersException("Receiver does not support the requested module")
    }


    /**
     * get the OCN Node URL as registered by the basic role in the OCN Registry
     */
    fun getRemoteNodeUrl(receiver: BasicRole): String {
        val url = registry.nodeURLOf(receiver.country.toByteArray(), receiver.id.toByteArray()).sendAsync().get()
        if (url == "") {
            throw OcpiHubUnknownReceiverException("Recipient not registered on OCN")
        }
        return url
    }


    /**
     * Check sender is known to this node using only the authorization header token
     */
    fun validateSender(authorization: String) {
        // TODO: check using existsByAuth_TokenC
        platformRepo.findByAuth_TokenC(authorization.extractToken())
                ?: throw OcpiClientInvalidParametersException("Invalid CREDENTIALS_TOKEN_C")
    }


    /**
     * Check sender is known to this node using the authorization header token and role provided as sender
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
     * @return Receiver - defines the whether receiver is LOCAL (on this node) or REMOTE (on different node)
     */
    fun validateReceiver(receiver: BasicRole): Receiver {
        return when {
            isRoleKnown(receiver) -> Receiver.LOCAL
            isRoleKnownOnNetwork(receiver, belongsToMe = false) -> Receiver.REMOTE
            else -> throw OcpiHubUnknownReceiverException("Receiver not registered on Open Charging Network")
        }
    }

    /**
     * Check receiver has allowed sender to send them messages
     */
    fun validateWhitelisted(sender: BasicRole, receiver: BasicRole) {
        val platform = getPlatform(receiver)
        val rulesList = ocnRulesListRepo.findAllByPlatformID(platform.id)
        val whitelisted = when {
            platform.rules.whitelist -> rulesList.any { it.counterparty == sender }
            platform.rules.blacklist -> rulesList.none { it.counterparty == sender }
            else -> true
        }
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
            proxied -> getProxyResource(request.urlPathVariables, request.headers.sender, request.headers.receiver)

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
                val endpoint = getPlatformEndpoint(platformID, request.module, request.interfaceRole)
                urlJoin(endpoint.url, request.urlPathVariables)
            }

            // return standard OCPI module URL of recipient
            else -> {
                val endpoint = getPlatformEndpoint(platformID, request.module, request.interfaceRole)
                urlJoin(endpoint.url, request.urlPathVariables)
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

        val url = getRemoteNodeUrl(request.headers.receiver)

        var modifiedBody = request.copy()

        // if callback function present, use it
        alterBody?.let { modifiedBody = it(url) }

        // if proxied request, add the proxy resource to the body
        if (proxied) {
            modifiedBody = modifiedBody.run {
                copy(proxyResource = getProxyResource(urlPathVariables, headers.sender, headers.receiver))
            }
        }

        // strip authorization
        modifiedBody = modifiedBody.copy(headers = modifiedBody.headers.copy(authorization = ""))

        val bodyString = stringify(modifiedBody)

        val headers = OcnMessageHeaders(
                requestID = generateUUIDv4Token(),
                signature = walletService.sign(bodyString))

        return Triple(url, headers, bodyString)
    }


    /**
     * Proxy the Link header in paginated requests (i.e. GET sender cdrs, sessions, tariffs, tokens) so that the
     * requesting platform is able to request the next page without needing authorization on the recipient's
     * system.
     */
    fun proxyPaginationHeaders(request: OcpiRequestVariables, responseHeaders: Map<String, String>): HttpHeaders {
        val headers = HttpHeaders()
        responseHeaders["Link"]?.let {
            it.extractNextLink()?.let {next ->
                val id = setProxyResource(next, request.headers.sender, request.headers.receiver)
                val proxyPaginationEndpoint = "/ocpi/${request.interfaceRole.id}/2.2/${request.module.id}/page"
                val link = urlJoin(properties.url, proxyPaginationEndpoint, id)
                headers.set("Link", "$link; rel=\"next\"")
            }
        }
        responseHeaders["X-Total-Count"]?.let { headers.set("X-Total-Count", it) }
        responseHeaders["X-Limit"]?.let { headers.set("X-Limit", it) }
        return headers
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
