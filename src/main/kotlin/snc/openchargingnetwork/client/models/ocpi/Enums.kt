package snc.openchargingnetwork.client.models.ocpi

enum class OcpiStatus(val code: Int, val message: String? = null) {
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

enum class Role {
    CPO,
    EMSP,
    HUB,
    NSP,
    OTHER,
    SCSP
}

enum class ImageCategory {
    CHARGER,
    ENTRANCE,
    LOCATION,
    NETWORK,
    OPERATOR,
    OTHER,
    OWNER
}

enum class LocationType {
    ON_STREET,
    PARKING_GARAGE,
    UNDERGROUND_GARAGE,
    PARKING_LOT,
    OTHER,
    UNKNOWN
}

enum class Facility {
    HOTEL,
    RESTAURANT,
    CAFE,
    MALL,
    SUPERMARKET,
    SPORT,
    RECREATION_AREA,
    NATURE,
    MUSEUM,
    BIKE_SHARING,
    BUS_STOP,
    TAXI_STAND,
    TRAM_STOP,
    METRO_STATION,
    TRAIN_STATION,
    AIRPORT,
    PARKING_LOT,
    CARPOOL_PARKING,
    FUEL_STATION,
    WIFI
}

enum class EnergySourceCategory {
    NUCLEAR,
    GENERAL_FOSSIL,
    COAL,
    GAS,
    GENERAL_GREEN,
    SOLAR,
    WIND,
    WATER
}

enum class EnvironmentalImpactCategory {
    NUCLEAR_WASTE,
    CARBON_DIOXIDE
}

enum class EvseStatus {
    AVAILABLE,
    BLOCKED,
    CHARGING,
    INOPERATIVE,
    OUTOFORDER,
    PLANNED,
    REMOVED,
    RESERVED,
    UNKNOWN
}

enum class Capability {
    CHARGING_PROFILE_CAPABLE,
    CHARGING_PREFERENCES_CAPABLE,
    CREDIT_CARD_PAYABLE,
    DEBIT_CARD_PAYABLE,
    REMOTE_START_STOP_CAPABLE,
    RESERVABLE,
    RFID_READER,
    TOKEN_GROUP_CAPABLE,
    UNLOCK_CAPABLE
}

enum class ParkingRestriction {
    EV_ONLY,
    PLUGGED,
    DISABLED,
    CUSTOMERS,
    MOTORCYCLES
}

enum class ConnectorType {
    CHADEMO,
    DOMESTIC_A,
    DOMESTIC_B,
    DOMESTIC_C,
    DOMESTIC_D,
    DOMESTIC_E,
    DOMESTIC_F,
    DOMESTIC_G,
    DOMESTIC_H,
    DOMESTIC_I,
    DOMESTIC_J,
    DOMESTIC_K,
    DOMESTIC_L,
    IEC_60309_2_single_16,
    IEC_60309_2_three_16,
    IEC_60309_2_three_32,
    IEC_60309_2_three_64,
    IEC_62196_T1,
    IEC_62196_T1_COMBO,
    IEC_62196_T2,
    IEC_62196_T2_COMBO,
    IEC_62196_T3A,
    IEC_62196_T3C,
    PANTOGRAPH_BOTTOM_UP,
    PANTOGRAPH_TOP_DOWN,
    TESLA_R,
    TESLA_S
}

enum class ConnectorFormat {
    SOCKET,
    CABLE
}

enum class PowerType {
    AC_1_PHASE,
    AC_3_PHASE,
    DC
}
