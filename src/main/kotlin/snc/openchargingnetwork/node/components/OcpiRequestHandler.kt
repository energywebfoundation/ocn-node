/*
    Copyright 2019-2020 eMobility GmbH

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

package snc.openchargingnetwork.node.components

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import snc.openchargingnetwork.node.config.HaasProperties
import snc.openchargingnetwork.node.components.HttpClientComponent
import snc.openchargingnetwork.node.config.NodeProperties
import snc.openchargingnetwork.node.models.OcpiHttpResponse
import snc.openchargingnetwork.node.models.Receiver
import snc.openchargingnetwork.node.models.exceptions.OcpiHubUnknownReceiverException
import snc.openchargingnetwork.node.models.ocpi.BasicRole
import snc.openchargingnetwork.node.models.ocpi.ModuleID
import snc.openchargingnetwork.node.models.ocpi.OcpiRequestVariables
import snc.openchargingnetwork.node.services.*
import snc.openchargingnetwork.node.tools.generateUUIDv4Token
import snc.openchargingnetwork.node.tools.urlJoin


/**
 * Spring boot component that instantiates RequestHandler objects.
 */
@Component
class OcpiRequestHandlerBuilder(
    private val routingService: RoutingService,
    private val registryService: RegistryService,
    private val httpClientComponent: HttpClientComponent,
    private val walletService: WalletService,
    private val hubClientInfoService: HubClientInfoService,
    private val asyncTaskService: AsyncTaskService,
    private val responseHandlerBuilder: OcpiResponseHandlerBuilder,
    private val properties: NodeProperties,
    private val haasProperties: HaasProperties,
    private val coroutineScope: CoroutineScope
) {

    /**
     * Build a RequestHandler object from an OcpiRequestVariables object.
     */
    fun <T : Any> build(requestVariables: OcpiRequestVariables): OcpiRequestHandler<T> {
        return OcpiRequestHandler(
            requestVariables, routingService, registryService, httpClientComponent, hubClientInfoService,
            walletService, asyncTaskService, responseHandlerBuilder, properties, haasProperties, coroutineScope
        )
    }

    /**
     * Build a RequestHandler object from a JSON-serialized string of an OcpiRequestVariables object.
     */
    fun <T : Any> build(requestVariablesString: String): OcpiRequestHandler<T> {
        val requestVariables = httpClientComponent.convertToRequestVariables(requestVariablesString)
        return OcpiRequestHandler(
            requestVariables, routingService, registryService, httpClientComponent, hubClientInfoService,
            walletService, asyncTaskService, responseHandlerBuilder, properties, haasProperties, coroutineScope
        )
    }

}


/**
 * Handle an individual OCPI HTTP request, instantiated with OcpiRequestVariables. The RequestHandler supports method
 * chaining to cleanly handle incoming messages.
 *
 * Generally speaking, all incoming peer-to-peer OCPI requests follow a very similar trajectory:
 *
 *      1. validate the message:
 *          - sender is registered on this node
 *          - [optional] sender's signature is valid
 *
 *      2. find location of sender on network
 *
 *      3. [optional] modify request (i.e. if containing a response_url which the recipient will be unable to read)
 *
 *      4. forward the request
 *
 * On a successful forwarding of the request, the handler will return an OcpiResponseHandler which can be used to
 * validate and extract the response.
 */
class OcpiRequestHandler<T : Any>(
    request: OcpiRequestVariables,
    routingService: RoutingService,
    registryService: RegistryService,
    private val httpClientComponent: HttpClientComponent,
    private val hubClientInfoService: HubClientInfoService,
    private val walletService: WalletService,
    private val asyncTaskService: AsyncTaskService,
    private val responseHandlerBuilder: OcpiResponseHandlerBuilder,
    private val integrationsRoutingService: IntegrationsRoutingService,
    properties: NodeProperties,
    haasProperties: HaasProperties,
    private val coroutineScope: CoroutineScope
) : OcpiMessageHandler(request, properties, haasProperties, routingService, registryService) {

    companion object {
        private var logger: Logger = LoggerFactory.getLogger(OcpiRequestHandler::class.java)
    }

    /**
     * Forward an incoming request to the specified receiver.
     * @param proxied tells the RequestHandler that this request requires a proxied resource that was previously
     * saved by the OCN Node (e.g. a paginated "Link" response header).
     */
    fun forwardDefault(proxied: Boolean = false, fromLocalPlatform: Boolean = true): OcpiResponseHandler<T> {
        if (fromLocalPlatform) {
            assertSenderValid()
        }

        val response: OcpiHttpResponse<T> = when (routingService.getReceiverType(request.headers.receiver)) {
            Receiver.LOCAL -> {
                assertWhitelisted()
                assertValidSignature()
                val (url, headers) = routingService.prepareLocalPlatformRequest(request, proxied)

                asyncTaskService.forwardOcpiRequestToLinkedServices(this, fromLocalPlatform)
                httpClientComponent.makeOcpiRequest(url, headers, request)
            }

            Receiver.REMOTE -> {
                assertValidSignature(false)
                val (url, headers, body) = routingService.prepareRemotePlatformRequest(request, proxied)

                asyncTaskService.forwardOcpiRequestToLinkedServices(this, fromLocalPlatform)
                httpClientComponent.postOcnMessage(url, headers, body)
            }
        }

        return responseHandlerBuilder.build(request, response, knownSender = fromLocalPlatform)
    }


    fun forwardHaasAsync(): OcpiRequestHandler<T> {
        coroutineScope.launch {
            try {
                if (haasProperties.enabled) {
                    val uri = request.urlPath?.removePrefix("/")
                    val haasUrl = if (uri.isNullOrEmpty()) {
                        haasProperties.url + "/ocpi/" + request.module.toString().lowercase()
                    } else {
                        haasProperties.url + "/ocpi/" + request.module.toString().lowercase() + "/" + uri
                    }
                    val headers = request.headers

                    logger.info("Forwarding request to Haas: $haasUrl | module: ${request.module} | sender: ${request.headers.sender} | receiver: ${request.headers.receiver}")
                    val response: OcpiHttpResponse<T> = httpClientComponent.makeOcpiRequest(haasUrl, headers, request)
                    logger.info("Successfully forwarded request to Haas:  http status code: ${response.statusCode} | ocpi status: ${response.body?.statusCode} | ocpi status message: ${response.body?.statusMessage}")
                }
            } catch (e: Exception) {
                logger.error("Error forwarding request to Haas: ${e.message}")
            }
        }

        return this
    }

    fun forwardIntegrationsAsync(module: ModuleID): OcpiRequestHandler<T> {
        val currentContext = this;
        coroutineScope.launch {
            val receivingParties = integrationsRoutingService.getIntegrationReceivingParties(module, request.headers.sender);
            for (recevingParty in receivingParties) {
                val response: OcpiHttpResponse<T> = when (routingService.getReceiverType(recevingParty)) {
                    Receiver.LOCAL -> {
                        val (url, headers) = routingService.prepareLocalPlatformRequest(request, false)

                        asyncTaskService.forwardOcpiRequestToLinkedServices(currentContext, true)
                        httpClientComponent.makeOcpiRequest(url, headers, request)
                    }
                    Receiver.REMOTE -> {
                        val (url, headers, body) = routingService.prepareRemotePlatformRequest(request, false)
                        asyncTaskService.forwardOcpiRequestToLinkedServices(currentContext, true)
                        httpClientComponent.postOcnMessage(url, headers, body)
                    }
                }
            }
        }

        return this
    }


    /**
     * Forward requests from module interfaces which require the modifying of a "response_url" (i.e. commands, charging
     * profiles).
     * @param responseUrl the original response_url as defined by the sender
     * @param modifyRequest callback which allows the request (OcpiRequestVariables) used by this RequestHandler to
     * be modified with the new response_url which will be sent to the receiver.
     */
    fun forwardAsync(
        responseUrl: String,
        modifyRequest: (newResponseUrl: String) -> OcpiRequestVariables
    ): OcpiResponseHandler<T> {
        assertSenderValid()

        val proxyPath = "/ocpi/sender/2.2.1/${request.module.id}/${request.urlPath}"
        val rewriteFields = mapOf("$['body']['response_url']" to responseUrl)

        val response: OcpiHttpResponse<T> = when (routingService.getReceiverType(request.headers.receiver)) {

            Receiver.LOCAL -> {
                assertWhitelisted()
                assertValidSignature()

                // save the original resource (response_url), returning a uid pointing to its location
                val resourceID =
                    routingService.setProxyResource(responseUrl, request.headers.receiver, request.headers.sender)
                // use the callback to modify the original request with the new response_url
                val modifiedRequest =
                    modifyRequest(urlJoin(properties.url, properties.apiPrefix, proxyPath, resourceID))
                // use the notary to securely modify the request signature
                modifiedRequest.headers.signature = rewriteAndSign(modifiedRequest.toSignedValues(), rewriteFields)
                // send the request with the modified body
                val (url, headers) = routingService.prepareLocalPlatformRequest(request)

                asyncTaskService.forwardOcpiRequestToLinkedServices(this)
                httpClientComponent.makeOcpiRequest(url, headers, modifiedRequest)
            }

            Receiver.REMOTE -> {
                assertValidSignature(false)

                val (url, headers, body) = routingService.prepareRemotePlatformRequest(request) {
                    // create a uid that will point to the original response_url
                    val proxyUID = generateUUIDv4Token()
                    // use the callback to modify the original request with the new response_url
                    val modifiedRequest = modifyRequest(urlJoin(it, proxyPath, proxyUID))
                    // use the notary to securely modify the request signature
                    modifiedRequest.headers.signature = rewriteAndSign(modifiedRequest.toSignedValues(), rewriteFields)
                    // add the proxy uid and resource to the outgoing body (read by the recipient's OCN node)
                    modifiedRequest.copy(proxyUID = proxyUID, proxyResource = responseUrl)
                }

                asyncTaskService.forwardOcpiRequestToLinkedServices(this)
                httpClientComponent.postOcnMessage(url, headers, body)
            }

        }

        return responseHandlerBuilder.build(request, response)
    }

    /**
     * Forwards a message received over the network (containing an "OCN-Signature" from the sending node)
     * @param sendingNodeSignature the OCN-Signature header received from the sending node
     */
    fun forwardFromOcn(sendingNodeSignature: String): OcpiResponseHandler<T> {
        validateOcnMessage(sendingNodeSignature)
        return forwardDefault(fromLocalPlatform = false)
    }

    /**
     * Forwards a message to another recipient (i.e. a Service with the appropriate permissions).
     * @param newRecipient country_code and party_id of the Service
     */
    fun forwardAgain(newRecipient: BasicRole): OcpiResponseHandler<T> {
        val modifiedRequest = request.copy(headers = request.headers.copy(receiver = newRecipient))
        val rewriteFields = mapOf(
            "$['headers']['ocpi-to-country-code']" to request.headers.receiver.country,
            "$['headers']['ocpi-to-party-id']" to request.headers.receiver.id
        )

        modifiedRequest.headers.signature = rewriteAndSign(modifiedRequest.toSignedValues(), rewriteFields)

        val response: OcpiHttpResponse<T> = when (routingService.getReceiverType(newRecipient)) {
            Receiver.LOCAL -> {
                val (url, headers) = routingService.prepareLocalPlatformRequest(modifiedRequest)
                httpClientComponent.makeOcpiRequest(url, headers, modifiedRequest)
            }

            Receiver.REMOTE -> {
                val (url, headers, body) = routingService.prepareRemotePlatformRequest(modifiedRequest)
                httpClientComponent.postOcnMessage(url, headers, body)
            }
        }

        return responseHandlerBuilder.build(modifiedRequest, response)
    }

    /**
     * Assert the sender is allowed to send OCPI requests to this OCN Node.
     */
    private fun assertSenderValid() {
        routingService.checkSenderKnown(request.headers.authorization, request.headers.sender)
        hubClientInfoService.renewClientConnection(request.headers.sender)
    }

    /**
     * Wrapper around RoutingService.checkSenderWhitelisted (asserts sender is allowed to send messages to receiver)
     */
    private fun assertWhitelisted() {
        routingService.checkSenderWhitelisted(
            sender = request.headers.sender,
            receiver = request.headers.receiver,
            moduleID = request.resolveModuleId()
        )
    }

    /**
     * Wrapper around OcpiMessageHandler.validateOcnSignature (asserts signature is valid)
     * @param knownReceiver set to true if the request is to a local platform - this will also check if the receiver
     * platform requires a signature too.
     */
    private fun assertValidSignature(knownReceiver: Boolean = true) {
        val receiver = if (knownReceiver) {
            request.headers.receiver
        } else {
            null
        }
        validateOcnSignature(
            signature = request.headers.signature,
            signedValues = request.toSignedValues(),
            signer = request.headers.sender,
            receiver = receiver
        )
    }

    /**
     * Asserts the sender exists in the Registry and is connected to the OCN Node which has sent the request.
     * Asserts the receiver is connected to this OCN Node.
     */
    private fun validateOcnMessage(signature: String): OcpiRequestHandler<T> {
        if (!registryService.isRoleKnown(request.headers.sender, belongsToMe = false)) {
            throw OcpiHubUnknownReceiverException("Sending party not registered on Open Charging Network")
        }

        if (!routingService.isRoleKnown(request.headers.receiver)) {
            throw OcpiHubUnknownReceiverException("Recipient unknown to OCN Node entered in Registry")
        }

        val requestString = httpClientComponent.mapper.writeValueAsString(request)
        walletService.verify(requestString, signature, request.headers.sender)
        return this
    }

}
