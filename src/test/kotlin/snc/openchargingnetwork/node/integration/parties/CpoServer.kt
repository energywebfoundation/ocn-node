package snc.openchargingnetwork.node.integration.parties

import shareandcharge.openchargingnetwork.notary.Notary
import shareandcharge.openchargingnetwork.notary.ValuesToSign
import org.web3j.crypto.Credentials as KeyPair
import snc.openchargingnetwork.node.data.exampleLocation1
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
    }

}