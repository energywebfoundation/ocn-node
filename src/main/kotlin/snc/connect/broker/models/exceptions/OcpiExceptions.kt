package snc.connect.broker.models.exceptions

import snc.connect.broker.enums.Status

// 2xxx: Client errors
class OcpiClientGenericException(message: String, val statusCode: Int = Status.SUCCESS.code): Exception(message)
class OcpiClientInvalidParametersException(message: String = "Invalid or missing parameters", val statusCode: Int = Status.CLIENT_INVALID_PARAMETERS.code): Exception(message)
class OcpiClientNotEnoughInformationException(message: String = "Not enough information", val statusCode: Int = Status.CLIENT_NOT_ENOUGH_INFO.code): Exception(message)
class OcpiClientUnknownLocationException(message: String = "Unknown location", val statusCode: Int = Status.CLIENT_UNKNOWN_LOCATION.code): Exception(message)

// 3xxx: Server errors
class OcpiServerGenericException(message: String, val statusCode: Int = Status.SERVER_ERROR.code): Exception(message)
class OcpiServerUnusableApiException(message: String = "Unable to use client's API", val statusCode: Int = Status.SERVER_UNUSABLE_API.code): Exception(message)
class OcpiServerUnsupportedVersionException(message: String = "Unsupported version", val statusCode: Int = Status.SERVER_UNSUPPORTED_VERSION.code): Exception(message)
class OcpiServerNoMatchingEndpointsException(message: String = "No matching endpoints or expected endpoints missing between parties", val statusCode: Int = Status.SERVER_NO_MATCHING_ENDPOINTS.code): Exception(message)

// 4xxx: Hub errors
class OcpiHubUnknownReceiverException(message: String = "Unknown receiver", val statusCode: Int = 4001): Exception(message)
class OcpiHubTimeoutOnRequestException(message: String = "Timeout on forwarded request", val statusCode: Int = 4002): Exception(message)
class OcpiHubConnectionProblemException(message: String = "Connection problem", val statusCode: Int = 4003): Exception(message)
