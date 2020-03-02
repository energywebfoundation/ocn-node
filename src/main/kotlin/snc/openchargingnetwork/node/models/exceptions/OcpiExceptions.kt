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

import org.springframework.http.HttpStatus
import snc.openchargingnetwork.node.models.ocpi.OcpiStatus

// 2xxx: Client errors
class OcpiClientGenericException(message: String,
                                 val httpStatus: HttpStatus = HttpStatus.BAD_REQUEST,
                                 val ocpiStatus: OcpiStatus = OcpiStatus.CLIENT_ERROR): Exception(message)

class OcpiClientInvalidParametersException(message: String = "Invalid or missing parameters",
                                           val httpStatus: HttpStatus = HttpStatus.BAD_REQUEST,
                                           val ocpiStatus: OcpiStatus = OcpiStatus.CLIENT_INVALID_PARAMETERS): Exception(message)

class OcpiClientNotEnoughInformationException(message: String = "Not enough information",
                                              val httpStatus: HttpStatus = HttpStatus.BAD_REQUEST,
                                              val ocpiStatus: OcpiStatus = OcpiStatus.CLIENT_NOT_ENOUGH_INFO): Exception(message)

class OcpiClientUnknownLocationException(message: String = "Unknown location",
                                         val httpStatus: HttpStatus = HttpStatus.NOT_FOUND,
                                         val ocpiStatus: OcpiStatus = OcpiStatus.CLIENT_UNKNOWN_LOCATION): Exception(message)

// 3xxx: Server errors
class OcpiServerGenericException(message: String,
                                 val httpStatus: HttpStatus = HttpStatus.OK,
                                 val ocpiStatus: OcpiStatus = OcpiStatus.SERVER_ERROR): Exception(message)

class OcpiServerUnusableApiException(message: String = "Unable to use client's API",
                                     val httpStatus: HttpStatus = HttpStatus.OK,
                                     val ocpiStatus: OcpiStatus = OcpiStatus.SERVER_UNUSABLE_API): Exception(message)

class OcpiServerUnsupportedVersionException(message: String = "Unsupported version",
                                            val httpStatus: HttpStatus = HttpStatus.OK,
                                            val ocpiStatus: OcpiStatus = OcpiStatus.SERVER_UNSUPPORTED_VERSION): Exception(message)

class OcpiServerNoMatchingEndpointsException(message: String = "No matching endpoints or expected endpoints missing between parties",
                                             val httpStatus: HttpStatus = HttpStatus.OK,
                                             val ocpiStatus: OcpiStatus = OcpiStatus.SERVER_NO_MATCHING_ENDPOINTS): Exception(message)

// 4xxx: Hub errors
class OcpiHubUnknownReceiverException(message: String = "Unknown receiver",
                                      val httpStatus: HttpStatus = HttpStatus.OK,
                                      val ocpiStatus: OcpiStatus = OcpiStatus.HUB_UNKNOWN_RECEIVER): Exception(message)

class OcpiHubTimeoutOnRequestException(message: String = "Timeout on forwarded request",
                                       val httpStatus: HttpStatus = HttpStatus.OK,
                                       val ocpiStatus: OcpiStatus = OcpiStatus.HUB_REQUEST_TIMEOUT): Exception(message)

class OcpiHubConnectionProblemException(message: String = "Connection problem",
                                        val httpStatus: HttpStatus = HttpStatus.OK,
                                        val ocpiStatus: OcpiStatus = OcpiStatus.HUB_CONNECTION_PROBLEM): Exception(message)
