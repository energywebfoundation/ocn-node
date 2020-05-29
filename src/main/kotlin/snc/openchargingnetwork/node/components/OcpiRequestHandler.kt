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

package snc.openchargingnetwork.node.components

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import snc.openchargingnetwork.node.config.NodeProperties
import snc.openchargingnetwork.node.models.HttpResponse
import snc.openchargingnetwork.node.models.Receiver
import snc.openchargingnetwork.node.models.exceptions.OcpiHubUnknownReceiverException
import snc.openchargingnetwork.node.models.ocpi.OcpiRequestVariables
import snc.openchargingnetwork.node.services.*
import snc.openchargingnetwork.node.tools.generateUUIDv4Token
import snc.openchargingnetwork.node.tools.urlJoin
import kotlin.properties.Delegates.observable


/**
 * Spring boot service that instantiates RequestHandler objects.
 */
@Component
class OcpiRequestHandlerBuilder(private val routingService: RoutingService,
                                private val registryService: RegistryService,
                                private val httpService: HttpService,
                                private val walletService: WalletService,
                                private val hubClientInfoService: HubClientInfoService,
                                private val asyncTaskService: AsyncTaskService,
                                private val responseHandlerBuilder: OcpiResponseHandlerBuilder,
                                private val properties: NodeProperties) {

    /**
     * Build a RequestHandler object from an OcpiRequestVariables object.
     */
    fun <T: Any> build(requestVariables: OcpiRequestVariables): OcpiRequestHandler<T> {
        return OcpiRequestHandler(requestVariables, routingService, registryService, httpService, hubClientInfoService,
                walletService, asyncTaskService, responseHandlerBuilder, properties)
    }

    /**
     * Build a RequestHandler object from a JSON-serialized string of an OcpiRequestVariables object.
     */
    fun <T: Any> build(requestVariablesString: String): OcpiRequestHandler<T> {
        val requestVariables = httpService.convertToRequestVariables(requestVariablesString)
        return OcpiRequestHandler(requestVariables, routingService, registryService, httpService, hubClientInfoService,
                walletService, asyncTaskService, responseHandlerBuilder, properties)
    }

}


/**
 * Handle an individual OCPI HTTP request. Instantiated with OcpiRequestVariables. The RequestHandler supports method
 * chaining to cleanly handle incoming messages, however, this comes at the cost of needing to manage/know its state.
 *
 * There is a common way of handling core requests (peer-to-peer OCPI requests).
 *      1. validate the request (authorization, sender, receiver, signature [optional])
 *      2. forward the request
 *
 * To avoid operating on responses that don't exist this order of operations needs to be respected. If not, an
 * UnsupportedOperationException will be raised.
 *
 * E.g.
 *
 * requestHandler.validateSender().forwardRequest().getResponse()
 * requestHandler.validateSender().forwardRequest(proxied = true).getResponseWithPaginatedHeaders()
 * requestHandler.validateOcnMessage(signature).forwardRequest().getResponseWithAllHeaders()
 *
 * @property request the OCPI HTTP request as OcpiRequestVariables.
 */
class OcpiRequestHandler<T: Any>(request: OcpiRequestVariables,
                                 routingService: RoutingService,
                                 registryService: RegistryService,
                                 private val httpService: HttpService,
                                 private val hubClientInfoService: HubClientInfoService,
                                 private val walletService: WalletService,
                                 private val asyncTaskService: AsyncTaskService,
                                 private val responseHandlerBuilder: OcpiResponseHandlerBuilder,
                                 properties: NodeProperties): OcpiMessageHandler(request, properties, routingService, registryService) {

    companion object {
        private var logger: Logger = LoggerFactory.getLogger(OcpiRequestHandler::class.java)
    }

    /**
     * Is sender/receiver known to this OCN Node?
     */
    private var knownSender: Boolean = true

    /**
     * Sender's message is valid (sender, receiver and signature have all been validated)
     * Once valid, we can run the async task to find ocn apps the request can be forwarded to
     */
    private var requestIsValid: Boolean by observable<Boolean>(false) { _, validBefore, validNow ->
        if (!validBefore && validNow) { // task should only run when requestIsValid becomes true for the first time
            asyncTaskService.forwardToLinkedApps(request)
        }
    }

    /**
     * Assert the sender is allowed to send OCPI requests to this OCN Node.
     * If message signing is required, or OCN-Signature header present, verifies the signature.
     */
    fun validateSender(): OcpiRequestHandler<T> {
        routingService.checkSenderKnown(request.headers.authorization, request.headers.sender)
        hubClientInfoService.renewClientConnection(request.headers.sender)
        return this
    }

    /**
     * Asserts the sender exists in the Registry and is connected to the OCN Node which has sent the request.
     * Asserts the receiver is connected to this OCN Node.
     * If message signing is required, or OCN-Signature header present, verifies the signature.
     */
    fun validateOcnMessage(signature: String): OcpiRequestHandler<T> {
        knownSender = false
        if (!registryService.isRoleKnown(request.headers.sender, belongsToMe = false)) {
            throw OcpiHubUnknownReceiverException("Sending party not registered on Open Charging Network")
        }

        if (!routingService.isRoleKnown(request.headers.receiver)) {
            throw OcpiHubUnknownReceiverException("Recipient unknown to OCN Node entered in Registry")
        }

        val requestString = httpService.mapper.writeValueAsString(request)
        walletService.verify(requestString, signature, request.headers.sender)
        return this
    }

    /**
     * Forward the request to the receiver.
     * Checks whether receiver is LOCAL or REMOTE.
     *      - If LOCAL, makes direct request to receiver.
     *      - If REMOTE, forwards request to receiver's OCN Node as written in the Registry.
     *
     * @param proxied tells the RequestHandler that this request requires a proxied resource that was previously
     * saved by the OCN Node.
     */
    fun forwardRequest(proxied: Boolean = false): OcpiResponseHandler<T> {
        val response: HttpResponse<T> = when (routingService.getReceiverType(request.headers.receiver)) {

            Receiver.LOCAL -> {
                routingService.checkSenderWhitelisted(
                        sender = request.headers.sender,
                        receiver = request.headers.receiver,
                        module = request.module)

                validateOcnSignature(
                        signature = request.headers.signature,
                        signedValues = request.toSignedValues(),
                        signer = request.headers.sender,
                        receiver = request.headers.receiver)
                val (url, headers) = routingService.prepareLocalPlatformRequest(request, proxied)

                requestIsValid = true // trigger side-effect (forward to linked apps)
                httpService.makeOcpiRequest(url, headers, request)
            }

            Receiver.REMOTE -> {
                validateOcnSignature(
                        signature = request.headers.signature,
                        signedValues = request.toSignedValues(),
                        signer = request.headers.sender)
                val (url, headers, body) = routingService.prepareRemotePlatformRequest(request, proxied)

                requestIsValid = true // trigger side-effect (forward to linked apps)
                httpService.postOcnMessage(url, headers, body)
            }
        }

        return responseHandlerBuilder.build(request, response)
    }

    /**
     * Used by the commands module's RECEIVER interface, which requires the modifying of the "response_url".
     * Checks whether receiver is LOCAL or REMOTE.
     *      - If LOCAL, makes direct request to receiver.
     *      - If REMOTE, forwards request to receiver's OCN Node as written in the Registry.
     *
     * @param responseUrl the original response_url as defined by the sender
     * @param modifyRequest callback which allows the request (OcpiRequestVariables) used by this RequestHandler to
     * be modified with the new response_url which will be sent to the receiver.
     */
    fun forwardModifiableRequest(responseUrl: String, modifyRequest: (newResponseUrl: String) -> OcpiRequestVariables): OcpiResponseHandler<T> {
        val proxyPath = "/ocpi/sender/2.2/commands/${request.urlPathVariables}"
        val rewriteFields = mapOf("$['body']['response_url']" to responseUrl)

        val response: HttpResponse<T> = when (routingService.getReceiverType(request.headers.receiver)) {

            Receiver.LOCAL -> {
                routingService.checkSenderWhitelisted(
                        sender = request.headers.sender,
                        receiver = request.headers.receiver,
                        module = request.module)
                validateOcnSignature(
                        signature = request.headers.signature,
                        signedValues = request.toSignedValues(),
                        signer = request.headers.sender,
                        receiver = request.headers.receiver)

                // save the original resource (response_url), returning a uid pointing to its location
                val resourceID = routingService.setProxyResource(responseUrl, request.headers.receiver, request.headers.sender)
                // use the callback to modify the original request with the new response_url
                val modifiedRequest = modifyRequest(urlJoin(properties.url, proxyPath, resourceID))
                // use the notary to securely modify the request signature
                modifiedRequest.headers.signature = rewriteAndSign(modifiedRequest.toSignedValues(), rewriteFields)
                // send the request with the modified body
                val (url, headers) = routingService.prepareLocalPlatformRequest(request)
                requestIsValid = true // trigger side-effect (forward to linked apps)
                httpService.makeOcpiRequest(url, headers, modifiedRequest)
            }

            Receiver.REMOTE -> {
                validateOcnSignature(
                        signature = request.headers.signature,
                        signedValues = request.toSignedValues(),
                        signer = request.headers.sender)

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

                requestIsValid = true // trigger side-effect (forward to linked apps)
                httpService.postOcnMessage(url, headers, body)
            }

        }

        return responseHandlerBuilder.build(request, response)
    }

}
