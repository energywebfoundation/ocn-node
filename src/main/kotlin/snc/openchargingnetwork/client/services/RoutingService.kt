package snc.openchargingnetwork.client.services

import org.springframework.stereotype.Service
import snc.openchargingnetwork.client.models.HttpResponse
import snc.openchargingnetwork.client.models.exceptions.OcpiClientInvalidParametersException
import snc.openchargingnetwork.client.models.exceptions.OcpiHubConnectionProblemException
import snc.openchargingnetwork.client.models.exceptions.OcpiHubUnknownReceiverException
import snc.openchargingnetwork.client.models.ocpi.BasicRole
import snc.openchargingnetwork.client.models.ocpi.InterfaceRole
import snc.openchargingnetwork.client.models.ocpi.OcpiResponse
import snc.openchargingnetwork.client.repositories.EndpointRepository
import snc.openchargingnetwork.client.repositories.PlatformRepository
import snc.openchargingnetwork.client.repositories.RoleRepository
import snc.openchargingnetwork.client.tools.extractToken
import snc.openchargingnetwork.client.tools.generateUUIDv4Token
import snc.openchargingnetwork.contracts.RegistryFacade
import kotlin.reflect.KClass

@Service
class RoutingService(private val platformRepo: PlatformRepository,
                     private val roleRepo: RoleRepository,
                     private val endpointRepo: EndpointRepository,
                     private val httpService: HttpRequestService,
                     private val registry: RegistryFacade) {

    fun isRoleKnown(role: BasicRole) = roleRepo.existsByCountryCodeAndPartyIDAllIgnoreCase(role.country, role.id)

    fun getPlatformID(role: BasicRole) = roleRepo.findByCountryCodeAndPartyIDAllIgnoreCase(role.country, role.id)!!.platformID

    fun getPlatformEndpoint(platformID: Long?, identifier: String, interfaceRole: InterfaceRole)
            = endpointRepo.findByPlatformIDAndIdentifierAndRole(platformID, identifier, interfaceRole)
            ?: throw OcpiClientInvalidParametersException("Receiver does not support the requested module")

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

    // check sender is authorized on this message broker AND that sender is original client-owned object owner
    fun validateSender(authorization: String, sender: BasicRole, clientOwnedObjectOwner: BasicRole) {

        // as above
        this.validateSender(authorization, sender)

        // check sender and client-owned object owner are the same
        if (sender.toLowerCase() != clientOwnedObjectOwner.toLowerCase()) {
            throw OcpiClientInvalidParametersException("Client-owned object does not belong to this sender")
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

}