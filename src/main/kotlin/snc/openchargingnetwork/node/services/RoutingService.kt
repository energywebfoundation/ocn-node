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

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.web3j.crypto.Credentials
import org.web3j.crypto.Keys
import snc.openchargingnetwork.contracts.Permissions
import snc.openchargingnetwork.contracts.Registry
import snc.openchargingnetwork.node.config.NodeProperties
import snc.openchargingnetwork.node.models.*
import snc.openchargingnetwork.node.models.entities.*
import snc.openchargingnetwork.node.models.entities.OcnRules
import snc.openchargingnetwork.node.models.exceptions.*
import snc.openchargingnetwork.node.models.ocpi.BasicRole
import snc.openchargingnetwork.node.models.ocpi.InterfaceRole
import snc.openchargingnetwork.node.models.ocpi.ModuleID
import snc.openchargingnetwork.node.models.ocpi.OcpiRequestVariables
import snc.openchargingnetwork.node.repositories.*
import snc.openchargingnetwork.node.tools.checksum
import snc.openchargingnetwork.node.tools.extractToken
import snc.openchargingnetwork.node.tools.generateUUIDv4Token
import snc.openchargingnetwork.node.tools.urlJoin
import java.util.concurrent.CompletableFuture

@Service
class RoutingService(private val platformRepo: PlatformRepository,
                     private val roleRepo: RoleRepository,
                     private val endpointRepo: EndpointRepository,
                     private val proxyResourceRepo: ProxyResourceRepository,
                     private val registry: Registry,
                     private val httpService: HttpService,
                     private val walletService: WalletService,
                     private val ocnRulesService: OcnRulesService,
                     private val properties: NodeProperties) {

    companion object {
        private var logger: Logger = LoggerFactory.getLogger(RoutingService::class.java)
    }

    /**
     * serialize a data class (with @JsonProperty annotations) as a JSON string
     */
    private fun stringify(body: Any): String {
        return httpService.mapper.writeValueAsString(body)
    }

    /**
     * Get nodes listed in registry
     */
    fun getNodesListedInRegistry(omitMine: Boolean = false): List<RegistryNode> {
        val nodes = registry.nodeOperators.sendAsync().get()
                .map {
                    val url = registry.getNode(it as String).sendAsync().get()
                    RegistryNode(operator = it.checksum(), url = url)
                }

        return if (omitMine) {
            val myAddress = Credentials.create(properties.privateKey).address.checksum()
            nodes.filter { it.operator != myAddress }
        } else {
            nodes
        }
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

        val (operator, domain) = registry.getOperatorByOcpi(country, id).sendAsync().get()
        if (belongsToMe) {
            val myKey = Credentials.create(properties.privateKey).address
            return domain == properties.url && Keys.toChecksumAddress(operator) == Keys.toChecksumAddress(myKey)
        }

        return domain != ""
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
    fun getPlatformEndpoint(platformID: Long?, module: ModuleID, interfaceRole: InterfaceRole): EndpointEntity {
        return endpointRepo.findByPlatformIDAndIdentifierAndRole(platformID, module.id, interfaceRole)
                ?: throw OcpiClientInvalidParametersException("Receiver does not support the requested module")
    }


    /**
     * get the OCN Node URL as registered by the basic role in the OCN Registry
     */
    fun getRemoteNodeUrl(receiver: BasicRole): String {
        val (_, domain) = registry.getOperatorByOcpi(receiver.country.toByteArray(), receiver.id.toByteArray()).sendAsync().get()
        if (domain == "") {
            throw OcpiHubUnknownReceiverException("Recipient not registered on OCN")
        }
        return domain
    }


    /**
     * Only returns party and operator addresses so far; add as needed
     */
    fun getPartyDetails(party: BasicRole): RegistryPartyDetailsBasic {
        val result = registry.getPartyDetailsByOcpi(party.country.toByteArray(), party.id.toByteArray()).sendAsync().get()
        return RegistryPartyDetailsBasic(address = result.component1(), operator = result.component5())
    }


    /**
     * Check sender is known to this node using only the authorization header token
     */
    fun validateSender(authorization: String) {
        if (!platformRepo.existsByAuth_TokenC(authorization.extractToken())) {
            throw OcpiClientInvalidParametersException("Invalid CREDENTIALS_TOKEN_C")
        }
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
    fun validateWhitelisted(sender: BasicRole, receiver: BasicRole, module: ModuleID) {
        val platform = getPlatform(receiver)
        val whitelisted = ocnRulesService.isWhitelisted(platform, sender, module)
        if (!whitelisted) {
            throw OcpiClientGenericException("Message receiver not in sender's whitelist.")
        }
    }


    /**
     * Used after validating a receiver: find the url of the local recipient for the given OCPI module/interface
     * and set the correct headers, replacing the X-Request-ID and Authorization token.
     */
    fun prepareLocalPlatformRequest(request: OcpiRequestVariables, proxied: Boolean = false): Pair<String, OcnHeaders> {

        logger.info("preparing local platform request")

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

        logger.info("prepared local platform request")

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
