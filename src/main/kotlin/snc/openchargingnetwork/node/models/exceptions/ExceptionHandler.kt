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
import snc.openchargingnetwork.node.models.ocpi.OcpiStatus
import snc.openchargingnetwork.node.models.ocpi.OcpiResponse
import java.net.ConnectException
import java.net.SocketTimeoutException

@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
class ExceptionHandler: ResponseEntityExceptionHandler() {

    override fun handleHttpMessageNotReadable(e: HttpMessageNotReadableException, headers: HttpHeaders, status: HttpStatus, request: WebRequest): ResponseEntity<Any> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(OcpiResponse<Nothing>(
                statusCode = OcpiStatus.CLIENT_INVALID_PARAMETERS.code,
                statusMessage = e.message))
    }

    override fun handleMissingServletRequestParameter(e: MissingServletRequestParameterException, headers: HttpHeaders, status: HttpStatus, request: WebRequest): ResponseEntity<Any> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(OcpiResponse<Nothing>(
                statusCode = OcpiStatus.CLIENT_INVALID_PARAMETERS.code,
                statusMessage = e.message))
    }

    @ExceptionHandler(MissingRequestHeaderException::class)
    fun handleMissingRequestHeaderException(e: MissingRequestHeaderException): ResponseEntity<OcpiResponse<Nothing>> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(OcpiResponse(
                statusCode = OcpiStatus.CLIENT_INVALID_PARAMETERS.code,
                statusMessage = e.message))
    }

    @ExceptionHandler(SocketTimeoutException::class)
    fun handleSocketTimeoutException(e: SocketTimeoutException): ResponseEntity<OcpiResponse<Nothing>> {
        return ResponseEntity.status(HttpStatus.OK).body(OcpiResponse(
                statusCode = OcpiStatus.HUB_REQUEST_TIMEOUT.code,
                statusMessage = e.message))
    }

    @ExceptionHandler(ConnectException::class)
    fun handleConnectException(e: ConnectException): ResponseEntity<OcpiResponse<Nothing>> {
        return ResponseEntity.status(HttpStatus.OK).body(OcpiResponse(
                statusCode = OcpiStatus.HUB_CONNECTION_PROBLEM.code,
                statusMessage = e.message))
    }

    /**
     * OCPI EXCEPTIONS
     */

    @ExceptionHandler(OcpiClientGenericException::class)
    fun handleOcpiClientGenericException(e: OcpiClientGenericException): ResponseEntity<OcpiResponse<Nothing>> {
        return ResponseEntity.status(e.httpStatus).body(OcpiResponse(
                statusCode = e.ocpiStatus.code,
                statusMessage = e.message))
    }

    @ExceptionHandler(OcpiClientInvalidParametersException::class)
    fun handleOcpiClientInvalidParametersException(e: OcpiClientInvalidParametersException): ResponseEntity<OcpiResponse<Nothing>> {
        return ResponseEntity.status(e.httpStatus).body(OcpiResponse(
                statusCode = e.ocpiStatus.code,
                statusMessage = e.message))
    }

    @ExceptionHandler(OcpiClientNotEnoughInformationException::class)
    fun handleOcpiClientNotEnoughInformationException(e: OcpiClientNotEnoughInformationException): ResponseEntity<OcpiResponse<Nothing>> {
        return ResponseEntity.status(e.httpStatus).body(OcpiResponse(
                statusCode = e.ocpiStatus.code,
                statusMessage = e.message))
    }

    @ExceptionHandler(OcpiClientUnknownLocationException::class)
    fun handleOcpiClientUnknownLocationException(e: OcpiClientUnknownLocationException): ResponseEntity<OcpiResponse<Nothing>> {
        return ResponseEntity.status(e.httpStatus).body(OcpiResponse(
                statusCode = e.ocpiStatus.code,
                statusMessage = e.message))
    }

    @ExceptionHandler(OcpiServerGenericException::class)
    fun handleOcpiServerGenericException(e: OcpiServerGenericException): ResponseEntity<OcpiResponse<Nothing>> {
        return ResponseEntity.status(e.httpStatus).body(OcpiResponse(
                statusCode = e.ocpiStatus.code,
                statusMessage = e.message))
    }

    @ExceptionHandler(OcpiServerUnusableApiException::class)
    fun handleOcpiServerUnusableApiException(e: OcpiServerUnusableApiException): ResponseEntity<OcpiResponse<Nothing>> {
        return ResponseEntity.status(e.httpStatus).body(OcpiResponse(
                statusCode = e.ocpiStatus.code,
                statusMessage = e.message))
    }

    @ExceptionHandler(OcpiServerNoMatchingEndpointsException::class)
    fun handleOcpiServerNoMatchingEndpointsException(e: OcpiServerNoMatchingEndpointsException): ResponseEntity<OcpiResponse<Nothing>> {
        return ResponseEntity.status(e.httpStatus).body(OcpiResponse(
                statusCode = e.ocpiStatus.code,
                statusMessage = e.message))
    }

    @ExceptionHandler(OcpiServerUnsupportedVersionException::class)
    fun handleOcpiServerUnsupportedVersionException(e: OcpiServerUnsupportedVersionException): ResponseEntity<OcpiResponse<Nothing>> {
        return ResponseEntity.status(e.httpStatus).body(OcpiResponse(
                statusCode = e.ocpiStatus.code,
                statusMessage = e.message))
    }

    @ExceptionHandler(OcpiHubConnectionProblemException::class)
    fun handleOcpiHubConnectionProblemException(e: OcpiHubConnectionProblemException): ResponseEntity<OcpiResponse<Nothing>> {
        return ResponseEntity.status(e.httpStatus).body(OcpiResponse(
                statusCode = e.ocpiStatus.code,
                statusMessage = e.message))
    }

    @ExceptionHandler(OcpiHubTimeoutOnRequestException::class)
    fun handleOcpiHubTimeoutOnRequestException(e: OcpiHubTimeoutOnRequestException): ResponseEntity<OcpiResponse<Nothing>> {
        return ResponseEntity.status(e.httpStatus).body(OcpiResponse(
                statusCode = e.ocpiStatus.code,
                statusMessage = e.message))
    }

    @ExceptionHandler(OcpiHubUnknownReceiverException::class)
    fun handleOcpiHubUnknownReceiverException(e: OcpiHubUnknownReceiverException): ResponseEntity<OcpiResponse<Nothing>> {
        return ResponseEntity.status(e.httpStatus).body(OcpiResponse(
                statusCode = e.ocpiStatus.code,
                statusMessage = e.message))
    }

}