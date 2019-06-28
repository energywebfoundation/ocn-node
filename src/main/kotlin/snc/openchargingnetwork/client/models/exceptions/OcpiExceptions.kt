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
