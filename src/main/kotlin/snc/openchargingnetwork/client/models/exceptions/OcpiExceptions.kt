/*
    Copyright 2019 Energy Web Foundation

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
