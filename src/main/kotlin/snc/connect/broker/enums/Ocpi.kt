package snc.connect.broker.enums

enum class StatusCode(val code: Int, val message: String? = null) {
    SUCCESS(1000),
    CLIENT_ERROR(2000),
    CLIENT_INVALID_PARAMETERS(2001, "Invalid or missing parameters"),
    CLIENT_NOT_ENOUGH_INFO(2002, "Not enough information"),
    CLIENT_UNKNOWN_LOCATION(2003, "Unknown location"),
    SERVER_ERROR(3000),
    SERVER_UNUSABLE_API(3001),
    SERVER_UNSUPPORTED_VERSION(3002),
    SERVER_NO_MATCHING_ENDPOINTS(3003),
    HUB_UNKNOWN_RECEIVER(4001, "Unknown receiver"),
    HUB_REQUEST_TIMEOUT(4002, "Timeout on forwarded request"),
    HUB_CONNECTION_PROBLEM(4003, "Connection problem")
}

enum class RoleType {
    CPO,
    EMSP,
    NSP,
    OTHER
}

enum class ConnectionStatusType {
    CONNECTED,
    OFFLINE,
    PLANNED,
    SUSPENDED
}

enum class ModuleID(val value: String) {
    Cdrs("cdrs"),
    ChargingProfiles("chargingprofiles"),
    Commands("commands"),
    Credentials("credentials"),
    HubClientInfo("hubclientinfo"),
    Locations("locations"),
    Sessions("sessions"),
    Tariffs("tariffs"),
    Tokens("tokens")
}

enum class InterfaceRole {
    CPO,
    MSP
}