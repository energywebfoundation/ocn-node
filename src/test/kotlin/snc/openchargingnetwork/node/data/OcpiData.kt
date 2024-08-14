package snc.openchargingnetwork.node.data

import snc.openchargingnetwork.node.models.ocpi.*
import snc.openchargingnetwork.node.tools.generateUUIDv4Token
import snc.openchargingnetwork.node.tools.getTimestamp

val exampleLocation1 = Location(
        countryCode = "ABC",
        partyID = "DE",
        id = "LOC1",
        publish = true,
        address = "Flamingoweg 89",
        city = "Bochum",
        country = "DE",
        coordinates = GeoLocation(
                latitude = "52.235",
                longitude = "0.809"),
        evses = listOf(Evse(
                uid = "abc123",
                status = EvseStatus.AVAILABLE,
                connectors = listOf(Connector(
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
        publish = true,
        address = "Flamingostraat 89",
        city = "Amsterdam",
        country = "NL",
        coordinates = GeoLocation(
                latitude = "52.235",
                longitude = "0.809"),
        evses = listOf(Evse(
                uid = "abc123",
                status = EvseStatus.AVAILABLE,
                connectors = listOf(Connector(
                        id = "1",
                        standard = ConnectorType.IEC_62196_T2,
                        format = ConnectorFormat.SOCKET,
                        powerType = PowerType.AC_3_PHASE,
                        maxVoltage = 400,
                        maxAmperage = 32,
                        lastUpdated = getTimestamp())),
                lastUpdated = getTimestamp())),
        lastUpdated = getTimestamp())

val exampleSession = Session(
        countryCode = "DE",
        partyID = "XYZ",
        id = "1234",
        startDateTime = getTimestamp(),
        kwh = 4.2F,
        cdrToken = CdrToken(
                uid = "9874728973",
                tokenType = TokenType.AD_HOC_USER,
                contractID = "DE-ABC-1",
                countryCode = "DE",
                partyID = "XYZ"),
        authMethod = AuthMethod.WHITELIST,
        locationID = "LOC1",
        evseUID = "1234",
        connectorID = "1",
        currency = "EUR",
        status = SessionStatus.ACTIVE,
        lastUpdated = getTimestamp())

val exampleCDR = CDR(
        countryCode = "CH",
        partyID = "ZUG",
        id = "abcdef-98765",
        startDateTime = getTimestamp(),
        endDateTime = getTimestamp(),
        sessionID = "87694729743",
        cdrToken = CdrToken(
                uid = "abc223",
                tokenType = TokenType.APP_USER,
                contractID = "de-12342-x",
                countryCode = "DE",
                partyID = "XYZ"),
        authMethod = AuthMethod.COMMAND,
        cdrLocation = CdrLocation(
                id = "LOC65",
                address = "quite far away 42",
                city = "Zug",
                postalCode = "12345",
                countryCode = "CHE",
                coordinates = GeoLocation(
                        latitude = "50.876",
                        longitude = "1.283"
                ),
                evseUID = "1234",
                evseID = "su-987239",
                connectorID = "2",
                connectorStandard = ConnectorType.CHADEMO,
                connectorFormat = ConnectorFormat.CABLE,
                connectorPowerType = PowerType.DC),
        currency = "CHF",
        chargingPeriods = listOf(ChargingPeriod(
                startDateTime = getTimestamp(),
                dimensions = listOf(CdrDimension(
                        type = CdrDimensionType.PARKING_TIME,
                        volume = 23.2F
                ))
        )),
        totalCost = Price(
                excludingVat = 16.5F,
                includingVat = 19.2F
        ),
        totalEnergy = 0F,
        totalTime = 23.2F,
        lastUpdated = getTimestamp()
)

val exampleTariff = Tariff(
        countryCode = "DE",
        partyID = "BSE",
        id = "xxx-1234",
        currency = "EUR",
        elements = listOf(TariffElement(listOf(PriceComponent(
                type = TariffDimensionType.ENERGY,
                price = 0.28F,
                stepSize = 1000)))),
        lastUpdated = getTimestamp()
)

val exampleToken = Token(
        countryCode = "DE",
        partyID = "ZUI",
        uid = generateUUIDv4Token(),
        type = TokenType.AD_HOC_USER,
        contractID = "123324324-XE",
        issuer = "Greatest EMSP Co.",
        valid = true,
        whitelist = WhitelistType.ALWAYS,
        lastUpdated = getTimestamp())

val exampleClientInfo = ClientInfo(
        countryCode = "DE",
        partyID = "ZUI",
        lastUpdated = getTimestamp(),
        role = Role.EMSP,
        status = ConnectionStatus.CONNECTED)