package snc.openchargingnetwork.node.integration.parties

import khttp.responses.Response
import snc.openchargingnetwork.node.models.ocpi.*

class MspServer(private val party: BasicRole, port: Int): PartyServer(party, port) {

    init {
        app.get("/ocpi/versions/2.2") {
            it.json(OcpiResponse(
                    statusCode = 1000,
                    data = VersionDetail(version = "2.2", endpoints = listOf(
                            Endpoint(
                                    identifier = "credentials",
                                    role = InterfaceRole.RECEIVER,
                                    url = urlBuilder("/ocpi/cpo/2.2/credentials"))))
            ))
        }
    }

    fun getLocation(to: BasicRole): Response {
        return khttp.get("$node/ocpi/sender/2.2/locations/1", headers = getHeaders(to))
    }

}