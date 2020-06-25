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
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import snc.openchargingnetwork.node.config.NodeProperties
import snc.openchargingnetwork.node.models.HttpResponse
import snc.openchargingnetwork.node.models.exceptions.OcpiClientInvalidParametersException
import snc.openchargingnetwork.node.models.exceptions.OcpiServerGenericException
import snc.openchargingnetwork.node.models.ocpi.OcpiRequestVariables
import snc.openchargingnetwork.node.models.ocpi.OcpiResponse
import snc.openchargingnetwork.node.services.HubClientInfoService
import snc.openchargingnetwork.node.services.RegistryService
import snc.openchargingnetwork.node.services.RoutingService
import snc.openchargingnetwork.node.tools.extractNextLink
import snc.openchargingnetwork.node.tools.urlJoin


/**
 * Spring boot service that instantiates OcpiResponseHandler objects.
 */
@Component
class OcpiResponseHandlerBuilder(private val routingService: RoutingService,
                                 private val registryService: RegistryService,
                                 private val hubClientInfoService: HubClientInfoService,
                                 private val properties: NodeProperties) {

    /**
     * Build a ResponseHandler object from an request (OcpiRequestVariables) and response (HttpResponse) object.
     *
     * @param request the OCPI HTTP request as OcpiRequestVariables.
     * @param response the OCPI HTTP response as HttpResponse<T>
     * @param knownSender is the sender of the request known to this node?
     */
    fun <T: Any> build(request: OcpiRequestVariables,
                       response: HttpResponse<T>,
                       knownSender: Boolean = true): OcpiResponseHandler<T> {
        return OcpiResponseHandler(request, response, knownSender, routingService, registryService, properties,
                hubClientInfoService)
    }

}


/**
 * Handle an individual OCPI HTTP response, for example after having an OCPI Request Handler forward a request and
 * received the response. Can be created easily using the above OcpiResponseHandlerBuilder.
 */
class OcpiResponseHandler<T: Any>(request: OcpiRequestVariables,
                                  private val response: HttpResponse<T>,
                                  private val knownSender: Boolean,
                                  routingService: RoutingService,
                                  registryService: RegistryService,
                                  properties: NodeProperties,
                                  hubClientInfoService: HubClientInfoService):
        OcpiMessageHandler(request, properties, routingService, registryService) {

    companion object {
        private var logger: Logger = LoggerFactory.getLogger(OcpiResponseHandler::class.java)
    }

    init {
        validateResponseSignature()
        if (isOcpiSuccess() && routingService.isRoleKnown(request.headers.receiver)) { // only renew connection of known recipients
            hubClientInfoService.renewClientConnection(request.headers.receiver)
        }
    }

    /**
     * Get the ResponseEntity object after forwarding the request, expecting no headers from the receiver's response.
     */
    fun getResponse(): ResponseEntity<OcpiResponse<T>> {
        val headers = HttpHeaders()
        return ResponseEntity
                .status(response.statusCode)
                .headers(headers)
                .body(response.body)
    }

    /**
     * Get the ResponseEntity object after forwarding the request, expecting pagination headers which must be proxied.
     */
    fun getResponseWithPaginationHeaders(): ResponseEntity<OcpiResponse<T>> {
        return when (isOcpiSuccess()) {
            true -> {
                request.urlPath?.let {
                    routingService.deleteProxyResource(it)
                }

                val headers = HttpHeaders()

                response.headers["X-Total-Count"]?.let { headers["X-Total-Count"] = it }
                response.headers["X-Limit"]?.let { headers["X-Limit"] = it }
                response.headers["OCN-Signature"]?.let { headers["OCN-Signature"] = it }

                response.headers["Link"]?.let {
                    it.extractNextLink()?.let {next ->

                        val id = routingService.setProxyResource(next, request.headers.sender, request.headers.receiver)
                        val proxyPaginationEndpoint = "/ocpi/${request.interfaceRole.id}/2.2/${request.module.id}/page"
                        val link = urlJoin(properties.url, proxyPaginationEndpoint, id)
                        headers["Link"] = "<$link>; rel=\"next\""

                        if (isSigningActive(request.headers.sender)) {
                            response.body.signature = null
                            val valuesToSign = response.copy(headers = headers.toSingleValueMap()).toSignedValues()
                            val rewriteFields = mapOf("$['headers']['link']" to it)
                            response.body.signature = rewriteAndSign(valuesToSign, rewriteFields)
                        }
                    }
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
     * Get the ResponseEntity object after forwarding the request, expecting a Location link which must be proxied.
     */
    fun getResponseWithLocationHeader(proxyPath: String): ResponseEntity<OcpiResponse<T>> {
        return when (isOcpiSuccess()) {
            true -> {
                val headers = HttpHeaders()
                response.headers["Location"]?.let {
                    val resourceId = routingService.setProxyResource(it, request.headers.sender, request.headers.receiver)
                    val newLocation = urlJoin(properties.url, proxyPath, resourceId)
                    headers["Location"] = newLocation

                    if (isSigningActive(request.headers.sender)) {
                        response.body.signature = null
                        val valuesToSign = response.copy(headers = headers.toSingleValueMap())
                        val rewriteFields = mapOf("$['headers']['location']" to it)
                        response.body.signature = rewriteAndSign(valuesToSign.toSignedValues(), rewriteFields)
                    }
                }

                ResponseEntity
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
        return when (isOcpiSuccess()) {
            true -> {
                val responseHeaders = HttpHeaders()
                response.headers["location"]?.let { responseHeaders.set("Location", it) }
                response.headers["Link"]?.let { responseHeaders.set("Link", it) }
                response.headers["X-Total-Count"]?.let { responseHeaders.set("X-Total-Count", it) }
                response.headers["X-Limit"]?.let { responseHeaders.set("X-Limit", it) }
                ResponseEntity
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
        return response.statusCode == 200 && response.body.statusCode == 1000
    }

    /**
     * Check response exists. Throws UnsupportedOperationException if request has not yet been forwarded.
     */
    private fun validateResponseSignature() {
        // set receiver based on if local/remote request
        val receiver = if (knownSender) { request.headers.sender } else { null }

        try {
            validateOcnSignature(
                    signature = response.body.signature,
                    signedValues = response.toSignedValues(),
                    signer = request.headers.receiver,
                    receiver = receiver)
        } catch (e: OcpiClientInvalidParametersException) {
            throw OcpiServerGenericException("Unable to verify response signature: ${e.message}")
        }
    }

}
