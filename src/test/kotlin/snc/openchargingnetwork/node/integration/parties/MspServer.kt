package snc.openchargingnetwork.node.integration.parties

import com.fasterxml.jackson.module.kotlin.readValue
import khttp.responses.Response
import shareandcharge.openchargingnetwork.notary.SignableHeaders
import snc.openchargingnetwork.node.data.exampleCDR
import snc.openchargingnetwork.node.data.exampleLocation1
import snc.openchargingnetwork.node.data.exampleToken
import snc.openchargingnetwork.node.integration.utils.OcnContracts
import snc.openchargingnetwork.node.integration.utils.PartyDefinition
import snc.openchargingnetwork.node.integration.utils.objectMapper
import snc.openchargingnetwork.node.integration.utils.toMap
import snc.openchargingnetwork.node.models.ocpi.*

class MspServer(config: PartyDefinition, contracts: OcnContracts): PartyServer(config, contracts) {

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
                                    identifier = "hubclientinfo",
                                    role = InterfaceRole.RECEIVER,
                                    url = urlBuilder("/ocpi/2.2/clientinfo")),
                            Endpoint(
                                    identifier = "commands",
                                    role = InterfaceRole.SENDER,
                                    url = urlBuilder("/ocpi/msp/2.2/commands")),
                            Endpoint(
                                    identifier = "enriched-locations",
                                    role = InterfaceRole.RECEIVER,
                                    url = urlBuilder("/ocpi/msp/2.2/enriched-locations")
                            )
                    ))))
        }

        app.get("/ocpi/msp/2.2/cdrs/1") {
            val body = OcpiResponse(statusCode = 1000, data = exampleCDR)
            body.signature = sign(body = body)
            it.json(body)
        }

        app.post("/ocpi/msp/2.2/cdrs") {
            val headers = SignableHeaders(location = urlBuilder("/ocpi/msp/2.2/cdrs/1"))
            val body = OcpiResponse(statusCode = 1000, data = null)
            body.signature = sign(headers = headers, body = body)
            it.header("location", headers.location!!).json(body)
        }

        app.post("/ocpi/msp/2.2/commands/START_SESSION/1") {
            asyncCommandsResponse = it.body<CommandResult>()
            val body = OcpiResponse(statusCode = 1000, data = null)
            body.signature = sign(body = body)
            it.json(body)
        }
    }

    fun getLocation(to: BasicRole): Response {
        val headers = getSignableHeaders(to)
        val signature = sign(headers = headers)
        return khttp.get("$node/ocpi/sender/2.2/locations/1", headers = headers.toMap(tokenC, signature))
    }

    fun getLocationList(to: BasicRole): Response {
        val headers = getSignableHeaders(to)
        val params = mapOf("limit" to "4")
        val signature = sign(headers = headers, params = params)
        return khttp.get("$node/ocpi/sender/2.2/locations", params=params, headers = headers.toMap(tokenC, signature))
    }

    fun getHubClientInfoList(to: BasicRole): Response {
        val headers = getSignableHeaders(to)
        val params = mapOf("limit" to "4")
        val signature = sign(headers = headers, params = params)
        return khttp.get("$node/ocpi/2.2/hubclientinfo", params=params, headers = headers.toMap(tokenC, signature))
    }

    fun getNextLink(to: BasicRole, next: String): Response {
        val headers = getSignableHeaders(to)
        val signature = sign(headers = headers)
        return khttp.get(next, headers = headers.toMap(tokenC, signature))
    }

    fun sendStartSession(to: BasicRole): Response {
        val body = StartSession(
                responseURL = urlBuilder("/ocpi/msp/2.2/commands/START_SESSION/1"),
                token = exampleToken,
                locationID = exampleLocation1.id,
                evseUID = exampleLocation1.evses!![0].uid)
        val headers = getSignableHeaders(to)
        val signature = sign(headers = headers, body = body)
        val json: Map<String, Any?> = objectMapper.readValue(objectMapper.writeValueAsString(body))
        return khttp.post("$node/ocpi/receiver/2.2/commands/START_SESSION", headers = headers.toMap(tokenC, signature), json = json)
    }

    fun sendCustomModuleRequest(to: BasicRole): Response { // TODO: test with different parameters (path, query, json)
        val headers = getSignableHeaders(to)
        val signature = sign(headers = headers)
        return khttp.get("$node/ocpi/custom/sender/lite-locations", headers = headers.toMap(tokenC, signature))
    }

}