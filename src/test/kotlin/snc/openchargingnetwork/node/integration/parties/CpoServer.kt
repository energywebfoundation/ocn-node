package snc.openchargingnetwork.node.integration.parties

import snc.openchargingnetwork.node.data.exampleLocation1
import snc.openchargingnetwork.node.models.ocpi.*

class CpoServer(party: BasicRole, port: Int): PartyServer(party, port) {

    init {
        app.get("/ocpi/versions/2.2") {
            it.json(OcpiResponse(
                    statusCode = 1000,
                    data = VersionDetail(version = "2.2", endpoints = listOf(
                            Endpoint(
                                    identifier = "credentials",
                                    role = InterfaceRole.RECEIVER,
                                    url = urlBuilder("/ocpi/cpo/2.2/credentials")),
                            Endpoint(
                                    identifier = "locations",
                                    role = InterfaceRole.SENDER,
                                    url = urlBuilder("/ocpi/sender/2.2/locations"))
                    ))
            ))
        }

        app.get("/ocpi/sender/2.2/locations/1") {
            it.json(OcpiResponse(
                    statusCode = 1000,
                    data = exampleLocation1))
        }
    }

}