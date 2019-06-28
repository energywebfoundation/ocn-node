package snc.openchargingnetwork.client.data

import snc.openchargingnetwork.client.models.ocpi.*
import snc.openchargingnetwork.client.tools.getTimestamp

val exampleLocation1 = Location(
        countryCode = "ABC",
        partyID = "DE",
        id = "LOC1",
        type = LocationType.ON_STREET,
        address = "Flamingoweg 89",
        city = "Bochum",
        country = "DE",
        coordinates = GeoLocation(
                latitude = "52.235",
                longitude = "0.809"),
        evses = arrayOf(Evse(
                uid = "abc123",
                status = EvseStatus.AVAILABLE,
                connectors = arrayOf(Connector(
                        id = "1",
                        standard = ConnectorType.IEC_62196_T2,
                        format = ConnectorFormat.SOCKET,
                        powerType = PowerType.AC_3_PHASE,
                        maxVoltage = 400,
                        maxAmperage = 32,
                        lastUpdated = getTimestamp())),
                lastUpdated = getTimestamp())),
        lastUpdated = getTimestamp())

val exampleLocation2 = Location(
        countryCode = "XYZ",
        partyID = "NL",
        id = "LOC2",
        type = LocationType.ON_STREET,
        address = "Flamingostraat 89",
        city = "Amsterdam",
        country = "NL",
        coordinates = GeoLocation(
                latitude = "52.235",
                longitude = "0.809"),
        evses = arrayOf(Evse(
                uid = "abc123",
                status = EvseStatus.AVAILABLE,
                connectors = arrayOf(Connector(
                        id = "1",
                        standard = ConnectorType.IEC_62196_T2,
                        format = ConnectorFormat.SOCKET,
                        powerType = PowerType.AC_3_PHASE,
                        maxVoltage = 400,
                        maxAmperage = 32,
                        lastUpdated = getTimestamp())),
                lastUpdated = getTimestamp())),
        lastUpdated = getTimestamp())