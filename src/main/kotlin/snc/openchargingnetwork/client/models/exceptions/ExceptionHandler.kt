/*
    Copyright 2019 Share&Charge Foundation

    This file is part of Open Charging Network Client.

    Open Charging Network Client is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Open Charging Network Client is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Open Charging Network Client.  If not, see <https://www.gnu.org/licenses/>.
*/

package snc.openchargingnetwork.client.models.exceptions

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
import snc.openchargingnetwork.client.models.ocpi.OcpiStatus
import snc.openchargingnetwork.client.models.ocpi.OcpiResponse
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
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(OcpiResponse(
                statusCode = OcpiStatus.HUB_REQUEST_TIMEOUT.code,
                statusMessage = e.message))
    }

    @ExceptionHandler(ConnectException::class)
    fun handleConnectException(e: ConnectException): ResponseEntity<OcpiResponse<Nothing>> {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(OcpiResponse(
                statusCode = OcpiStatus.HUB_CONNECTION_PROBLEM.code,
                statusMessage = e.message))
    }

    /**
     * OCPI EXCEPTIONS
     */

    @ExceptionHandler(OcpiClientGenericException::class)
    fun handleOcpiClientGenericException(e: OcpiClientGenericException): ResponseEntity<OcpiResponse<Nothing>> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(OcpiResponse(
                statusCode = OcpiStatus.CLIENT_ERROR.code,
                statusMessage = e.message))
    }

    @ExceptionHandler(OcpiClientInvalidParametersException::class)
    fun handleOcpiClientInvalidParametersException(e: OcpiClientInvalidParametersException): ResponseEntity<OcpiResponse<Nothing>> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(OcpiResponse(
                statusCode = OcpiStatus.CLIENT_INVALID_PARAMETERS.code,
                statusMessage = e.message))
    }

    @ExceptionHandler(OcpiClientNotEnoughInformationException::class)
    fun handleOcpiClientNotEnoughInformationException(e: OcpiClientNotEnoughInformationException): ResponseEntity<OcpiResponse<Nothing>> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(OcpiResponse(
                statusCode = OcpiStatus.CLIENT_NOT_ENOUGH_INFO.code,
                statusMessage = e.message))
    }

    @ExceptionHandler(OcpiClientUnknownLocationException::class)
    fun handleOcpiClientUnknownLocationException(e: OcpiClientUnknownLocationException): ResponseEntity<OcpiResponse<Nothing>> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(OcpiResponse(
                statusCode = OcpiStatus.CLIENT_UNKNOWN_LOCATION.code,
                statusMessage = e.message))
    }

    @ExceptionHandler(OcpiServerGenericException::class)
    fun handleOcpiServerGenericException(e: OcpiServerGenericException): ResponseEntity<OcpiResponse<Nothing>> {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(OcpiResponse(
                statusCode = OcpiStatus.SERVER_ERROR.code,
                statusMessage = e.message))
    }

    @ExceptionHandler(OcpiServerUnusableApiException::class)
    fun handleOcpiServerUnusableApiException(e: OcpiServerUnusableApiException): ResponseEntity<OcpiResponse<Nothing>> {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(OcpiResponse(
                statusCode = OcpiStatus.SERVER_UNUSABLE_API.code,
                statusMessage = e.message))
    }

    @ExceptionHandler(OcpiServerNoMatchingEndpointsException::class)
    fun handleOcpiServerNoMatchingEndpointsException(e: OcpiServerNoMatchingEndpointsException): ResponseEntity<OcpiResponse<Nothing>> {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(OcpiResponse(
                statusCode = OcpiStatus.SERVER_NO_MATCHING_ENDPOINTS.code,
                statusMessage = e.message))
    }

    @ExceptionHandler(OcpiServerUnsupportedVersionException::class)
    fun handleOcpiServerUnsupportedVersionException(e: OcpiServerUnsupportedVersionException): ResponseEntity<OcpiResponse<Nothing>> {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(OcpiResponse(
                statusCode = OcpiStatus.SERVER_UNSUPPORTED_VERSION.code,
                statusMessage = e.message))
    }

    @ExceptionHandler(OcpiHubConnectionProblemException::class)
    fun handleOcpiHubConnectionProblemException(e: OcpiHubConnectionProblemException): ResponseEntity<OcpiResponse<Nothing>> {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(OcpiResponse(
                statusCode = OcpiStatus.HUB_CONNECTION_PROBLEM.code,
                statusMessage = e.message))
    }

    @ExceptionHandler(OcpiHubTimeoutOnRequestException::class)
    fun handleOcpiHubTimeoutOnRequestException(e: OcpiHubTimeoutOnRequestException): ResponseEntity<OcpiResponse<Nothing>> {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(OcpiResponse(
                statusCode = OcpiStatus.HUB_REQUEST_TIMEOUT.code,
                statusMessage = e.message))
    }

    @ExceptionHandler(OcpiHubUnknownReceiverException::class)
    fun handleOcpiHubUnknownReceiverException(e: OcpiHubUnknownReceiverException): ResponseEntity<OcpiResponse<Nothing>> {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(OcpiResponse(
                statusCode = OcpiStatus.HUB_UNKNOWN_RECEIVER.code,
                statusMessage = e.message))
    }

}