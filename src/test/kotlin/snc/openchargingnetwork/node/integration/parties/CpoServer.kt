package snc.openchargingnetwork.node.integration.parties

import com.fasterxml.jackson.module.kotlin.readValue
import khttp.responses.Response
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import shareandcharge.openchargingnetwork.notary.Notary
import shareandcharge.openchargingnetwork.notary.SignableHeaders
import shareandcharge.openchargingnetwork.notary.ValuesToSign
import snc.openchargingnetwork.node.data.exampleCDR
import org.web3j.crypto.Credentials as KeyPair
import snc.openchargingnetwork.node.data.exampleLocation1
import snc.openchargingnetwork.node.integration.objectMapper
import snc.openchargingnetwork.node.integration.privateKey
import snc.openchargingnetwork.node.integration.toMap
import snc.openchargingnetwork.node.models.ocpi.*

class CpoServer(private val credentials: KeyPair, party: BasicRole, val port: Int): PartyServer(credentials, party, port) {

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
                                    url = urlBuilder("/ocpi/cpo/2.2/locations")),
                            Endpoint(
                                    identifier = "commands",
                                    role = InterfaceRole.RECEIVER,
                                    url = urlBuilder("/ocpi/cpo/2.2/commands"))))))
        }

        app.get("/ocpi/cpo/2.2/locations/1") {
            val body = OcpiResponse(statusCode = 1000, data = exampleLocation1)
            body.signature = Notary().sign(ValuesToSign(body = body), credentials.privateKey()).serialize()
            it.json(body)
        }

        app.get("/ocpi/cpo/2.2/locations") {
            val limit = it.queryParam("limit") ?: "5"
            val next = urlBuilder("/ocpi/cpo/2.2/locations")
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

        app.post("/ocpi/cpo/2.2/commands/START_SESSION") {
            val body = OcpiResponse(statusCode = 1000, data = CommandResponse(result = CommandResponseType.ACCEPTED, timeout = 10))
            val valuesToSign = ValuesToSign(body = body)
            body.signature = Notary().sign(valuesToSign, credentials.privateKey()).serialize()

            val url = it.body<StartSession>().responseURL

            // send async POST /START_SESSION
            val asyncHeaders = getSignableHeaders(BasicRole("MSP", "DE"))
            val asyncBody = CommandResult(result = CommandResultType.ACCEPTED)
            val asyncValues = ValuesToSign(headers = asyncHeaders, body = asyncBody)
            val asyncSignature = Notary().sign(asyncValues, credentials.privateKey()).serialize()
            val asyncJson: Map<String, Any?> = objectMapper.readValue(objectMapper.writeValueAsString(asyncBody))
            khttp.post(url, headers = asyncHeaders.toMap(tokenC, asyncSignature), json = asyncJson)

            it.json(body)
        }

    }

    fun sendCdr(to: BasicRole): Response {
        val headers = getSignableHeaders(to)
        val valuesToSign = ValuesToSign(headers = headers, body = exampleCDR)
        val signature = Notary().sign(valuesToSign, credentials.privateKey()).serialize()
        val json: Map<String, Any?> = objectMapper.readValue(objectMapper.writeValueAsString(exampleCDR))
        return khttp.post("$node/ocpi/receiver/2.2/cdrs", headers = headers.toMap(tokenC, signature), json = json)
    }

    fun getCdr(to: BasicRole, location: String): Response {
        val headers = getSignableHeaders(to)
        val valuesToSign = ValuesToSign(headers = headers, body = null)
        val signature = Notary().sign(valuesToSign, credentials.privateKey()).serialize()
        return khttp.get(location, headers = headers.toMap(tokenC, signature))
    }

}