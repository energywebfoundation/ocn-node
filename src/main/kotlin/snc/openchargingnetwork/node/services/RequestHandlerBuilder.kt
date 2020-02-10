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

import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import shareandcharge.openchargingnetwork.notary.Notary
import snc.openchargingnetwork.node.config.NodeProperties
import snc.openchargingnetwork.node.models.HttpResponse
import snc.openchargingnetwork.node.models.Receiver
import snc.openchargingnetwork.node.models.exceptions.OcpiClientInvalidParametersException
import snc.openchargingnetwork.node.models.exceptions.OcpiHubUnknownReceiverException
import snc.openchargingnetwork.node.models.ocpi.OcpiRequestVariables
import snc.openchargingnetwork.node.models.ocpi.OcpiResponse
import snc.openchargingnetwork.node.tools.generateUUIDv4Token
import snc.openchargingnetwork.node.tools.urlJoin


/**
 * Spring boot service that instantiates RequestHandler objects.
 */
@Service
class RequestHandlerBuilder(private val routingService: RoutingService,
                            private val httpService: HttpService,
                            private val walletService: WalletService,
                            private val properties: NodeProperties) {

    /**
     * Build a RequestHandler object from an OcpiRequestVariables object.
     */
    fun <T: Any> build(requestVariables: OcpiRequestVariables): RequestHandler<T> {
        return RequestHandler(requestVariables, routingService, httpService, walletService, properties)
    }

    /**
     * Build a RequestHandler object from a JSON-serialized string of an OcpiRequestVariables object.
     */
    fun <T: Any> build(requestVariablesString: String): RequestHandler<T> {
        val requestVariables = httpService.convertToRequestVariables(requestVariablesString)
        return RequestHandler(requestVariables, routingService, httpService, walletService, properties)
    }

}


/**
 * Handle an individual OCPI HTTP request. Instantiated with OcpiRequestVariables. The RequestHandler supports method
 * chaining to cleanly handle incoming messages, however, this comes at the cost of needing to manage/know its state.
 *
 * There is a common way of handling core requests (peer-to-peer OCPI requests).
 *      1. validate the request (authorization, sender, receiver, signature [optional])
 *      2. forward the request
 *      3. get the response
 *
 * To avoid operating on responses that don't exist this order of operations needs to be respected. If not, an
 * UnsupportedOperationException will be raised.
 *
 * E.g.
 *
 * requestHandler.validateSender().forwardRequest().getResponse()
 * requestHandler.validateSender().forwardRequest(proxied = true).getResponseWithPaginatedHeaders()
 * requestHandler.validateOcnMessage().forwardRequest().getResponseWithAllHeaders()
 *
 * @property request the OCPI HTTP request as OcpiRequestVariables.
 */
class RequestHandler<T: Any>(private val request: OcpiRequestVariables,
                             private val routingService: RoutingService,
                             private val httpService: HttpService,
                             private val walletService: WalletService,
                             private val properties: NodeProperties) {

    /**
     * HttpResponse object instantiated after forwarding a request.
     * TODO: could operate on response in dedicated ResponseHandler
     */
    private var response: HttpResponse<T>? = null

    /**
     * Notary object instantiated after validating a request.
     * Only if message signing property (signatures) set to true OR request contains "OCN-Signature" header.
     */
    private var notary: Notary? = null


    /**
     * Assert the sender is allowed to send OCPI requests to this OCN Node.
     * If message signing is required, or OCN-Signature header present, verifies the signature.
     */
    fun validateSender(): RequestHandler<T> {
        routingService.validateSender(request.headers.authorization, request.headers.sender)

        if (isSigningActive()) {
            validateOcnSignature()
        }

        return this
    }

    /**
     * Asserts the sender exists in the Registry and is connected to the OCN Node which has sent the request.
     * Asserts the receiver is connected to this OCN Node.
     * If message signing is required, or OCN-Signature header present, verifies the signature.
     */
    fun validateOcnMessage(signature: String): RequestHandler<T> {
        if (!routingService.isRoleKnownOnNetwork(request.headers.sender, belongsToMe = false)) {
            throw OcpiHubUnknownReceiverException("Sending party not registered on Open Charging Network")
        }

        if (!routingService.isRoleKnown(request.headers.receiver)) {
            throw OcpiHubUnknownReceiverException("Recipient unknown to OCN Node entered in Registry")
        }

        val requestString = httpService.mapper.writeValueAsString(request)
        walletService.verify(requestString, signature, request.headers.sender)

        if (isSigningActive()) {
            validateOcnSignature()
        }

        return this
    }

    /**
     * Forward the request to the receiver.
     * Checks whether receiver is LOCAL or REMOTE.
     *      - If LOCAL, makes direct request to receiver.
     *      - If REMOTE, forwards request to reciever's OCN Node as written in the Registry.
     *
     * @param proxied tells the RequestHandler that this request requires a proxied resource that was previously
     * saved by the OCN Node.
     */
    fun forwardRequest(proxied: Boolean = false): RequestHandler<T> {
        response = when (routingService.validateReceiver(request.headers.receiver)) {
            Receiver.LOCAL -> {
                val (url, headers) = routingService.prepareLocalPlatformRequest(request, proxied)
                httpService.makeOcpiRequest(url, headers, request)
            }
            Receiver.REMOTE -> {
                val (url, headers, body) = routingService.prepareRemotePlatformRequest(request, proxied)
                httpService.postOcnMessage(url, headers, body)
            }
        }
        return this
    }

    /**
     * Used by the commands module's RECEIVER interface, which requires the modifying of the "response_url".
     * Checks whether receiver is LOCAL or REMOTE.
     *      - If LOCAL, makes direct request to receiver.
     *      - If REMOTE, forwards request to reciever's OCN Node as written in the Registry.
     *
     * @param responseUrl the original response_url as defined by the sender
     * @param modifyRequest callback which allows the request (OcpiRequestVariables) used by this RequestHandler to
     * be modified with the new response_url which will be sent to the receiver.
     */
    fun forwardModifiableRequest(responseUrl: String, modifyRequest: (newResponseUrl: String) -> OcpiRequestVariables): RequestHandler<T> {
        val proxyPath = "/ocpi/sender/2.2/commands/${request.urlPathVariables}"

        fun rewriteAndSign(modifiedRequest: OcpiRequestVariables): String? {
            if (isSigningActive()) {
                val rewriteFields = mapOf("$['body']['response_url']" to responseUrl)
                val notary = validateNotary()
                notary.stash(rewriteFields)
                notary.sign(modifiedRequest.toNotaryReadableVariables(), walletService.privateKey)
                return notary.serialize()
            }
            return null
        }

        response = when (routingService.validateReceiver(request.headers.receiver)) {

            Receiver.LOCAL -> {
                // save the original resource (response_url), returning a uid pointing to its location
                val resourceID = routingService.setProxyResource(responseUrl, request.headers.receiver, request.headers.sender)
                // use the callback to modify the original request with the new response_url
                val modifiedRequest = modifyRequest(urlJoin(properties.url, proxyPath, resourceID))
                // use the notary to securely modify the request signature
                modifiedRequest.headers.signature = rewriteAndSign(modifiedRequest)
                // send the request with the modified body
                val (url, headers) = routingService.prepareLocalPlatformRequest(request)
                httpService.makeOcpiRequest(url, headers, modifiedRequest)
            }

            Receiver.REMOTE -> {
                val (url, headers, body) = routingService.prepareRemotePlatformRequest(request) {
                    // create a uid that will point to the original response_url
                    val proxyUID = generateUUIDv4Token()
                    // use the callback to modify the original request with the new response_url
                    val modifiedRequest = modifyRequest(urlJoin(it, proxyPath, proxyUID))
                    // use the notary to securely modify the request signature
                    modifiedRequest.headers.signature = rewriteAndSign(modifiedRequest)
                    // add the proxy uid and resource to the outgoing body (read by the recipient's OCN node)
                    modifiedRequest.copy(proxyUID = proxyUID, proxyResource = responseUrl)
                }
                httpService.postOcnMessage(url, headers, body)
            }

        }

        return this
    }

    /**
     * Get the ResponseEntity object after forwarding the request, expecting no headers from the receiver's response.
     */
    fun getResponse(): ResponseEntity<OcpiResponse<T>> {
        val response = validateResponse()
        return ResponseEntity
                .status(response.statusCode)
                .body(response.body)
    }

    /**
     * Get the ResponseEntity object after forwarding the request, expecting pagination headers which must be proxied.
     */
    fun getResponseWithPaginationHeaders(): ResponseEntity<OcpiResponse<T>> {
        val response = validateResponse()
        return when (isOcpiSuccess()) {
            true -> {
                request.urlPathVariables?.let {
                    routingService.deleteProxyResource(it)
                }
                val headers = routingService.proxyPaginationHeaders(request, response.headers)
                return ResponseEntity
                        .status(response.statusCode)
                        .headers(headers)
                        .body(response.body)
            }
            false -> getResponse()
        }
    }

    /**
     * Get the ResponseEntity object after forwarding the request, expecting a Location link which must be proxied.
     */
    fun getResponseWithLocationHeader(proxyPath: String): ResponseEntity<OcpiResponse<T>> {
        val response = validateResponse()
        return when (isOcpiSuccess()) {
            true -> {
                val headers = HttpHeaders()
                response.headers["Location"]?.let {
                    val resourceId = routingService.setProxyResource(it, request.headers.sender, request.headers.receiver)
                    headers["Location"] = urlJoin(properties.url, proxyPath, resourceId)
                }
                return ResponseEntity
                        .status(response.statusCode)
                        .headers(headers)
                        .body(response.body)
            }
            false -> getResponse()
        }
    }

    /**
     * Used by the /ocn/message handler to forward all possible response headers.
     */
    fun getResponseWithAllHeaders(): ResponseEntity<OcpiResponse<T>> {
        val response = validateResponse()
        return when (isOcpiSuccess()) {
            true -> {
                val responseHeaders = HttpHeaders()
                response.headers["location"]?.let { responseHeaders.set("Location", it) }
                response.headers["Link"]?.let { responseHeaders.set("Link", it) }
                response.headers["X-Total-Count"]?.let { responseHeaders.set("X-Total-Count", it) }
                response.headers["X-Limit"]?.let { responseHeaders.set("X-Limit", it) }
                return ResponseEntity
                        .status(response.statusCode)
                        .headers(responseHeaders)
                        .body(response.body)
            }
            false -> getResponse()
        }
    }

    /**
     * Check ocpi request was success (i.e. before operating on headers)
     */
    private fun isOcpiSuccess(): Boolean {
        return response!!.statusCode == 200 && response!!.body.statusCode == 1000
    }

    /**
     * Check message signing is enabled. Can be enabled in two ways:
     * 1. ocn.node.signatures property is set to true
     * 2. request contains a signature header
     *
     * Disclaimer: the receiver of the request may require message signing even if the OCN Node does not.
     */
    private fun isSigningActive(): Boolean {
        return properties.signatures || request.headers.signature != null
    }

    /**
     * Check response exists. Throws UnsupportedOperationException if request has not yet been forwarded.
     */
    private fun validateResponse(): HttpResponse<T> {
        return response ?: throw UnsupportedOperationException("Non-canonical method chaining: must call a forwarding method first")
    }

    /**
     * Check notary exists. Throws UnsupportedOperationException if request has not yet been validated.
     */
    private fun validateNotary(): Notary {
        return notary ?: throw UnsupportedOperationException("Non-canonical method chaining: must call a validating method first")
    }

    /**
     * Use the OCN Notary to validate a request's "OCN-Signature" header. Only use if signing is active.
     */
    private fun validateOcnSignature() {
        val result = request.headers.signature?.let {
            notary = Notary.deserialize(it)
            notary?.verify(request.toNotaryReadableVariables())
        }
        when {
            result == null -> throw OcpiClientInvalidParametersException("Missing required header: \"OCN-Signature\"")
            !result.isValid -> throw OcpiClientInvalidParametersException("Unable to verify signature: ${result.error}")
        }
    }

}
