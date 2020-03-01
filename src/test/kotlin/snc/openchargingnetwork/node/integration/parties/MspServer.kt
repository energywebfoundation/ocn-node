package snc.openchargingnetwork.node.integration.parties

import com.fasterxml.jackson.module.kotlin.readValue
import io.javalin.http.Context
import khttp.responses.Response
import org.web3j.crypto.Credentials as KeyPair
import shareandcharge.openchargingnetwork.notary.Notary
import shareandcharge.openchargingnetwork.notary.SignableHeaders
import shareandcharge.openchargingnetwork.notary.ValuesToSign
import snc.openchargingnetwork.node.data.exampleCDR
import snc.openchargingnetwork.node.data.exampleLocation1
import snc.openchargingnetwork.node.data.exampleToken
import snc.openchargingnetwork.node.integration.objectMapper
import snc.openchargingnetwork.node.integration.privateKey
import snc.openchargingnetwork.node.integration.toMap
import snc.openchargingnetwork.node.models.ocpi.*
import snc.openchargingnetwork.node.tools.generateUUIDv4Token

class MspServer(private val credentials: KeyPair, val party: BasicRole, port: Int): PartyServer(credentials, party, port) {

    // could be nicer to use e.g. RxJava instead
    var asyncCommandsResponse: CommandResult? = null

    init {
        app.get("/ocpi/versions/2.2") {
            it.json(OcpiResponse(
                    statusCode = 1000,
                    data = VersionDetail(version = "2.2", endpoints = listOf(
                            Endpoint(
                                    identifier = "credentials",
                                    role = InterfaceRole.RECEIVER,
                                    url = urlBuilder("/ocpi/msp/2.2/credentials")),
                            Endpoint(
                                    identifier = "cdrs",
                                    role = InterfaceRole.RECEIVER,
                                    url = urlBuilder("/ocpi/msp/2.2/cdrs")),
                            Endpoint(
                                    identifier = "commands",
                                    role = InterfaceRole.SENDER,
                                    url = urlBuilder("/ocpi/msp/2.2/commands"))))))
        }

        app.get("/ocpi/msp/2.2/cdrs/1") {
            val body = OcpiResponse(statusCode = 1000, data = exampleCDR)
            val valuesToSign = ValuesToSign(body = body)
            body.signature = Notary().sign(valuesToSign, credentials.privateKey()).serialize()
            it.json(body)
        }

        app.post("/ocpi/msp/2.2/cdrs") {
            val headers = SignableHeaders(location = urlBuilder("/ocpi/msp/2.2/cdrs/1"))
            val body = OcpiResponse(statusCode = 1000, data = null)
            val valuesToSign = ValuesToSign(headers = headers, body = body)
            body.signature = Notary().sign(valuesToSign, credentials.privateKey()).serialize()
            it.header("location", headers.location!!).json(body)
        }

        app.post("/ocpi/msp/2.2/commands/START_SESSION/1") {
            asyncCommandsResponse = it.body<CommandResult>()
            val body = OcpiResponse(statusCode = 1000, data = null)
            val valuesToSign = ValuesToSign(body = body)
            body.signature = Notary().sign(valuesToSign, credentials.privateKey()).serialize()
            it.json(body)
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

    fun sendStartSession(to: BasicRole): Response {
        val body = StartSession(
                responseURL = urlBuilder("/ocpi/msp/2.2/commands/START_SESSION/1"),
                token = exampleToken,
                locationID = exampleLocation1.id,
                evseUID = exampleLocation1.evses!![0].uid)
        val headers = getSignableHeaders(to)
        val request = ValuesToSign(headers = headers, body = body)
        val signature = Notary().sign(request, credentials.privateKey()).serialize()
        val json: Map<String, Any?> = objectMapper.readValue(objectMapper.writeValueAsString(body))
        return khttp.post("$node/ocpi/receiver/2.2/commands/START_SESSION", headers = headers.toMap(tokenC, signature), json = json)
    }

}