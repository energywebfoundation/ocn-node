package snc.openchargingnetwork.node.integration.parties

import khttp.responses.Response
import org.web3j.crypto.Credentials as KeyPair
import shareandcharge.openchargingnetwork.notary.Notary
import shareandcharge.openchargingnetwork.notary.ValuesToSign
import snc.openchargingnetwork.node.integration.privateKey
import snc.openchargingnetwork.node.integration.toMap
import snc.openchargingnetwork.node.models.ocpi.*

class MspServer(private val credentials: KeyPair, party: BasicRole, port: Int): PartyServer(credentials, party, port) {

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
        val headers = getSignableHeaders(to)
        val request = ValuesToSign(headers = headers, body = null)
        val signature = Notary().sign(request, credentials.privateKey()).serialize()
        return khttp.get("$node/ocpi/sender/2.2/locations/1", headers = headers.toMap(tokenC, signature))
    }

    fun getLocationList(to: BasicRole): Response {
        val headers = getSignableHeaders(to)
        val params = mapOf("limit" to "4")
        val request = ValuesToSign(headers = headers, params = params, body = null)
        val signature = Notary().sign(request, credentials.privateKey()).serialize()
        return khttp.get("$node/ocpi/sender/2.2/locations", params=params, headers = headers.toMap(tokenC, signature))
    }

    fun getNextLink(to: BasicRole, next: String): Response {
        val headers = getSignableHeaders(to)
        val params = mapOf("offset" to "4")
        val request = ValuesToSign(headers = headers, params = params, body = null)
        val signature = Notary().sign(request, credentials.privateKey()).serialize()
        return khttp.get(next, params = params, headers = headers.toMap(tokenC, signature))
    }

}