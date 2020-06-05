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

package snc.openchargingnetwork.node.models.exceptions

import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MissingRequestHeaderException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import shareandcharge.openchargingnetwork.notary.Notary
import shareandcharge.openchargingnetwork.notary.ValuesToSign
import snc.openchargingnetwork.node.config.NodeProperties
import snc.openchargingnetwork.node.models.ocpi.OcpiStatus
import snc.openchargingnetwork.node.models.ocpi.OcpiResponse
import java.net.ConnectException
import java.net.SocketTimeoutException

@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
class ExceptionHandler(private val properties: NodeProperties): ResponseEntityExceptionHandler() {

    private fun signError(body: OcpiResponse<Unit>): String {
        return Notary().sign(ValuesToSign(body = body), properties.privateKey!!).serialize()
    }

    /**
     * GENERIC EXCEPTIONS
     */

    override fun handleHttpMessageNotReadable(e: HttpMessageNotReadableException,
                                              headers: HttpHeaders,
                                              status: HttpStatus,
                                              request: WebRequest): ResponseEntity<Any> {
        val body = OcpiResponse<Unit>(statusCode = OcpiStatus.CLIENT_INVALID_PARAMETERS.code, statusMessage = e.message)
        body.signature = signError(body)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body)
    }

    override fun handleMissingServletRequestParameter(e: MissingServletRequestParameterException, headers: HttpHeaders, status: HttpStatus, request: WebRequest): ResponseEntity<Any> {
        val body = OcpiResponse<Unit>(statusCode = OcpiStatus.CLIENT_INVALID_PARAMETERS.code, statusMessage = e.message)
        body.signature = signError(body)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body)
    }

    @ExceptionHandler(MissingRequestHeaderException::class)
    fun handleMissingRequestHeaderException(e: MissingRequestHeaderException): ResponseEntity<OcpiResponse<Unit>> {
        val body = OcpiResponse<Unit>(
                statusCode = OcpiStatus.CLIENT_INVALID_PARAMETERS.code,
                statusMessage = e.message)
        body.signature = signError(body)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body)
    }

    @ExceptionHandler(SocketTimeoutException::class)
    fun handleSocketTimeoutException(e: SocketTimeoutException): ResponseEntity<OcpiResponse<Unit>> {
        val body = OcpiResponse<Unit>(
                statusCode = OcpiStatus.HUB_REQUEST_TIMEOUT.code,
                statusMessage = e.message)
        body.signature = signError(body)
        return ResponseEntity.status(HttpStatus.OK).body(body)
    }

    @ExceptionHandler(ConnectException::class)
    fun handleConnectException(e: ConnectException): ResponseEntity<OcpiResponse<Unit>> {
        val body = OcpiResponse<Unit>(
                statusCode = OcpiStatus.HUB_CONNECTION_PROBLEM.code,
                statusMessage = e.message)
        body.signature = signError(body)
        return ResponseEntity.status(HttpStatus.OK).body(body)
    }

    /**
     * OCPI EXCEPTIONS
     */

    private fun ocpiErrorToResponseEntity(httpStatus: HttpStatus, ocpiStatus: OcpiStatus, message: String?)
            : ResponseEntity<OcpiResponse<Unit>> {
        val body = OcpiResponse<Unit>(statusCode = ocpiStatus.code, statusMessage = message)
        body.signature = signError(body)
        return ResponseEntity.status(httpStatus).body(body)
    }

    @ExceptionHandler(OcpiClientGenericException::class)
    fun handleOcpiClientGenericException(e: OcpiClientGenericException) = ocpiErrorToResponseEntity(
            httpStatus = e.httpStatus,
            ocpiStatus = e.ocpiStatus,
            message = e.message)

    @ExceptionHandler(OcpiClientInvalidParametersException::class)
    fun handleOcpiClientInvalidParametersException(e: OcpiClientInvalidParametersException) = ocpiErrorToResponseEntity(
            httpStatus = e.httpStatus,
            ocpiStatus = e.ocpiStatus,
            message = e.message)

    @ExceptionHandler(OcpiClientNotEnoughInformationException::class)
    fun handleOcpiClientNotEnoughInformationException(e: OcpiClientNotEnoughInformationException) = ocpiErrorToResponseEntity(
            httpStatus = e.httpStatus,
            ocpiStatus = e.ocpiStatus,
            message = e.message)

    @ExceptionHandler(OcpiClientUnknownLocationException::class)
    fun handleOcpiClientUnknownLocationException(e: OcpiClientUnknownLocationException) = ocpiErrorToResponseEntity(
            httpStatus = e.httpStatus,
            ocpiStatus = e.ocpiStatus,
            message = e.message)

    @ExceptionHandler(OcpiServerGenericException::class)
    fun handleOcpiServerGenericException(e: OcpiServerGenericException) = ocpiErrorToResponseEntity(
            httpStatus = e.httpStatus,
            ocpiStatus = e.ocpiStatus,
            message = e.message)

    @ExceptionHandler(OcpiServerUnusableApiException::class)
    fun handleOcpiServerUnusableApiException(e: OcpiServerUnusableApiException) = ocpiErrorToResponseEntity(
            httpStatus = e.httpStatus,
            ocpiStatus = e.ocpiStatus,
            message = e.message)

    @ExceptionHandler(OcpiServerNoMatchingEndpointsException::class)
    fun handleOcpiServerNoMatchingEndpointsException(e: OcpiServerNoMatchingEndpointsException) = ocpiErrorToResponseEntity(
            httpStatus = e.httpStatus,
            ocpiStatus = e.ocpiStatus,
            message = e.message)

    @ExceptionHandler(OcpiServerUnsupportedVersionException::class)
    fun handleOcpiServerUnsupportedVersionException(e: OcpiServerUnsupportedVersionException) = ocpiErrorToResponseEntity(
            httpStatus = e.httpStatus,
            ocpiStatus = e.ocpiStatus,
            message = e.message)

    @ExceptionHandler(OcpiHubConnectionProblemException::class)
    fun handleOcpiHubConnectionProblemException(e: OcpiHubConnectionProblemException) = ocpiErrorToResponseEntity(
            httpStatus = e.httpStatus,
            ocpiStatus = e.ocpiStatus,
            message = e.message)

    @ExceptionHandler(OcpiHubTimeoutOnRequestException::class)
    fun handleOcpiHubTimeoutOnRequestException(e: OcpiHubTimeoutOnRequestException) = ocpiErrorToResponseEntity(
            httpStatus = e.httpStatus,
            ocpiStatus = e.ocpiStatus,
            message = e.message)

    @ExceptionHandler(OcpiHubUnknownReceiverException::class)
    fun handleOcpiHubUnknownReceiverException(e: OcpiHubUnknownReceiverException) = ocpiErrorToResponseEntity(
            httpStatus = e.httpStatus,
            ocpiStatus = e.ocpiStatus,
            message = e.message)

    /**
     * OCN Exceptions
     */

    @ExceptionHandler(InvalidOcnSignatureException::class)
    fun handleInvalidOcnSignatureException(e: InvalidOcnSignatureException): ResponseEntity<String> {
        return ResponseEntity.status(400).body(e.message)
    }

}