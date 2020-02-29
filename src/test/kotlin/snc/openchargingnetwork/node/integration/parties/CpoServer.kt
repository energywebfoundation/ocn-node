package snc.openchargingnetwork.node.integration.parties

import shareandcharge.openchargingnetwork.notary.Notary
import shareandcharge.openchargingnetwork.notary.SignableHeaders
import shareandcharge.openchargingnetwork.notary.ValuesToSign
import org.web3j.crypto.Credentials as KeyPair
import snc.openchargingnetwork.node.data.exampleLocation1
import snc.openchargingnetwork.node.data.exampleLocation2
import snc.openchargingnetwork.node.integration.privateKey
import snc.openchargingnetwork.node.models.ocpi.*

class CpoServer(credentials: KeyPair, party: BasicRole, port: Int): PartyServer(credentials, party, port) {

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
            val body = OcpiResponse(statusCode = 1000, data = exampleLocation1)
            body.signature = Notary().sign(ValuesToSign(body = body), credentials.privateKey()).serialize()
            it.json(body)
        }

        app.get("/ocpi/sender/2.2/locations") {
            val limit = it.queryParam("limit") ?: "5"
            val next = urlBuilder("/ocpi/sender/2.2/locations")
            val headers = SignableHeaders(limit = "5", totalCount = "50", link = "<$next>; rel=\"next\"")
            val data = mutableListOf<Location>()

            for (i in 1..limit.toInt()) {
                data.add(exampleLocation1)
            }

            val body = OcpiResponse(statusCode = 1000, data = data)
            body.signature = Notary().sign(ValuesToSign(headers, body = body), credentials.privateKey()).serialize()
            it.header("x-limit", "5")
                    .header("x-total-count", "50")
                    .header("link", "<$next>; rel=\"next\"")
                    .json(body)

        }
    }

}