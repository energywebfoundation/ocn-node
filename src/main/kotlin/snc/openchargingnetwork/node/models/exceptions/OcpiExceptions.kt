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

// 2xxx: Client errors
class OcpiClientGenericException(message: String): Exception(message)
class OcpiClientInvalidParametersException(message: String = "Invalid or missing parameters"): Exception(message)
class OcpiClientNotEnoughInformationException(message: String = "Not enough information"): Exception(message)
class OcpiClientUnknownLocationException(message: String = "Unknown location"): Exception(message)

// 3xxx: Server errors
class OcpiServerGenericException(message: String): Exception(message)
class OcpiServerUnusableApiException(message: String = "Unable to use client's API"): Exception(message)
class OcpiServerUnsupportedVersionException(message: String = "Unsupported version"): Exception(message)
class OcpiServerNoMatchingEndpointsException(message: String = "No matching endpoints or expected endpoints missing between parties"): Exception(message)

// 4xxx: Hub errors
class OcpiHubUnknownReceiverException(message: String = "Unknown receiver"): Exception(message)
class OcpiHubTimeoutOnRequestException(message: String = "Timeout on forwarded request"): Exception(message)
class OcpiHubConnectionProblemException(message: String = "Connection problem"): Exception(message)
