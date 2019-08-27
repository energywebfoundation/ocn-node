package snc.openchargingnetwork.client.controllers.ocpi.v2_2

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.ninjasquad.springmockk.MockkBean
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import snc.openchargingnetwork.client.data.exampleLocation1
import snc.openchargingnetwork.client.data.exampleLocation2
import snc.openchargingnetwork.client.models.*
import snc.openchargingnetwork.client.models.ocpi.*
import snc.openchargingnetwork.client.services.HttpService
import snc.openchargingnetwork.client.services.RoutingService
import snc.openchargingnetwork.client.tools.generateUUIDv4Token
import snc.openchargingnetwork.client.tools.getTimestamp

@WebMvcTest(LocationsController::class)
class LocationsControllerTest(@Autowired val mockMvc: MockMvc) {

    @MockkBean
    lateinit var routingService: RoutingService

    @MockkBean
    lateinit var httpService: HttpService

    private val mapper = jacksonObjectMapper()

    @BeforeEach
    fun before() {
        every { httpService.mapper } returns mapper
    }


    @Test
    fun `When GET sender Locations return paginated location list`() {

        val dateFrom = getTimestamp()

        val sender = BasicRole("EMY", "DE")
        val receiver = BasicRole("IGY", "DE")

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.LOCATIONS,
                interfaceRole = InterfaceRole.SENDER,
                method = HttpMethod.GET,
                headers = OcpiRequestHeaders(
                    requestID = generateUUIDv4Token(),
                    correlationID = generateUUIDv4Token(),
                    sender = sender,
                    receiver = receiver),
                urlEncodedParams = OcpiRequestParameters(dateFrom = dateFrom))

        val url = "https://ocpi.emsp.com/2.2/locations"

        val forwardingHeaders = requestVariables.headers.copy(
                authorization = "Token token-b",
                requestID = generateUUIDv4Token())

        every { routingService.validateSender("Token token-c", sender) } just Runs
        every { routingService.validateReceiver(receiver) } returns Receiver.LOCAL
        every { routingService.prepareLocalPlatformRequest(requestVariables) } returns Pair(url, forwardingHeaders)

        val responseHeaders = mapOf(
                "Link" to "https://ocpi.cpo.com/locations/page/2?dateFrom=$dateFrom; rel=\"next\"",
                "X-Limit" to "25",
                "X-Total-Count" to "500")

        every {
            httpService.makeOcpiRequest<Array<Location>>(HttpMethod.GET, url, forwardingHeaders.toMap(), requestVariables.urlEncodedParams?.toMap()!!)
        } returns HttpResponse(
                statusCode = 200,
                headers = responseHeaders,
                body = OcpiResponse(statusCode = 1000, data = arrayOf(exampleLocation1, exampleLocation2)))

        val httpHeaders = HttpHeaders()
        httpHeaders["Link"] = "https://client.ocn.co/ocpi/sender/2.2/locations/page/189; rel=\"next\""
        httpHeaders["X-Limit"] = responseHeaders["X-Limit"]
        httpHeaders["X-Total-Count"] = responseHeaders["X-Total-Count"]

        every {
            routingService.proxyPaginationHeaders(
                    request = requestVariables,
                    responseHeaders = responseHeaders) } returns httpHeaders

        mockMvc.perform(get("/ocpi/sender/2.2/locations")
                .header("Authorization", "Token token-c")
                .header("X-Request-ID", requestVariables.headers.requestID)
                .header("X-Correlation-ID", requestVariables.headers.correlationID)
                .header("OCPI-from-country-code", sender.country)
                .header("OCPI-from-party-id", sender.id)
                .header("OCPI-to-country-code", receiver.country)
                .header("OCPI-to-party-id", receiver.id)
                .param("date_from", dateFrom))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(header().string("Link", "https://client.ocn.co/ocpi/sender/2.2/locations/page/189; rel=\"next\""))
                .andExpect(header().string("X-Limit", "25"))
                .andExpect(header().string("X-Total-Count", "500"))
                .andExpect(jsonPath("\$.status_code").value(OcpiStatus.SUCCESS.code))
                .andExpect(jsonPath("\$.status_message").doesNotExist())
                .andExpect(jsonPath("\$.timestamp").isString)
                .andExpect(jsonPath("\$.data").isArray)
                .andExpect(jsonPath("\$.data", hasSize<Array<Location>>(2)))
                .andExpect(jsonPath("\$.data[0].id").value(exampleLocation1.id))
                .andExpect(jsonPath("\$.data[0].party_id").value(exampleLocation1.partyID))
                .andExpect(jsonPath("\$.data[1].id").value(exampleLocation2.id))
                .andExpect(jsonPath("\$.data[1].party_id").value(exampleLocation2.partyID))
    }


    @Test
    fun `When GET sender Locations page should return proxied locations list page`() {

        val sender = BasicRole("EMY", "DE")
        val receiver = BasicRole("IGY", "DE")

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.LOCATIONS,
                interfaceRole = InterfaceRole.SENDER,
                method = HttpMethod.GET,
                headers = OcpiRequestHeaders(
                        requestID = generateUUIDv4Token(),
                        correlationID = generateUUIDv4Token(),
                        sender = sender,
                        receiver = receiver),
                urlPathVariables = "67")

        val url = "https://some.emsp.com/actual/locations/page/2"

        val forwardingHeaders = requestVariables.headers.copy(
                authorization = "Token token-b",
                requestID = generateUUIDv4Token())

        every { routingService.validateSender("Token token-c", sender) } just Runs
        every { routingService.validateReceiver(receiver) } returns Receiver.LOCAL
        every { routingService.prepareLocalPlatformRequest(requestVariables, proxied = true) } returns Pair(url, forwardingHeaders)

        val responseHeaders = mapOf(
                "Link" to "https://some.emsp.com/actual/locations/page/3; rel=\"next\"",
                "X-Limit" to "100")

        every {
            httpService.makeOcpiRequest<Array<Location>>(HttpMethod.GET, url, forwardingHeaders.toMap())
        } returns HttpResponse(
                statusCode = 200,
                headers = responseHeaders,
                body = OcpiResponse(statusCode = 1000, data = arrayOf(exampleLocation2)))

        val httpHeaders = HttpHeaders()
        httpHeaders["Link"] = "https://client.ocn.co/ocpi/sender/2.2/locations/page/68; rel=\"next\""
        httpHeaders["X-Limit"] = responseHeaders["X-Limit"]

        every { routingService.deleteProxyResource(requestVariables.urlPathVariables!!) } just Runs

        every { routingService.proxyPaginationHeaders(
                    request = requestVariables,
                    responseHeaders = responseHeaders) } returns httpHeaders

        mockMvc.perform(get("/ocpi/sender/2.2/locations/page/${requestVariables.urlPathVariables}")
                .header("Authorization", "Token token-c")
                .header("X-Request-ID", requestVariables.headers.requestID)
                .header("X-Correlation-ID", requestVariables.headers.correlationID)
                .header("OCPI-from-country-code", requestVariables.headers.sender.country)
                .header("OCPI-from-party-id", requestVariables.headers.sender.id)
                .header("OCPI-to-country-code", requestVariables.headers.receiver.country)
                .header("OCPI-to-party-id", requestVariables.headers.receiver.id)
                .param("limit", "100"))
                .andExpect(status().isOk)
                .andExpect(header().string("Link", "https://client.ocn.co/ocpi/sender/2.2/locations/page/68; rel=\"next\""))
                .andExpect(header().string("X-Limit", "100"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("\$.status_code").value(1000))
                .andExpect(jsonPath("\$.status_message").doesNotExist())
                .andExpect(jsonPath("\$.data").isArray)
                .andExpect(jsonPath("\$.data", hasSize<Array<Location>>(1)))
                .andExpect(jsonPath("\$.data[0].id").value(exampleLocation2.id))
                .andExpect(jsonPath("\$.data[0].party_id").value(exampleLocation2.partyID))
                .andExpect(jsonPath("\$.timestamp").isString)
    }


    @Test
    fun `When GET sender Locations return single location`() {

        val locationID = "LOC1"

        val sender = BasicRole("EMY", "DE")
        val receiver = BasicRole("IGY", "DE")

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.LOCATIONS,
                interfaceRole = InterfaceRole.SENDER,
                method = HttpMethod.GET,
                headers = OcpiRequestHeaders(
                        requestID = generateUUIDv4Token(),
                        correlationID = generateUUIDv4Token(),
                        sender = sender,
                        receiver = receiver),
                urlPathVariables = locationID)

        val url = "https://ocpi.emsp.com/2.2/locations/$locationID"

        val forwardingHeaders = requestVariables.headers.copy(
                authorization = "Token token-b",
                requestID = generateUUIDv4Token())

        every { routingService.validateSender("Token token-c", sender) } just Runs
        every { routingService.validateReceiver(receiver) } returns Receiver.LOCAL
        every { routingService.prepareLocalPlatformRequest(requestVariables) } returns Pair(url, forwardingHeaders)

        every {
            httpService.makeOcpiRequest<Location>(HttpMethod.GET, url, forwardingHeaders.toMap())
        } returns HttpResponse(
                statusCode = 200,
                headers = mapOf(),
                body = OcpiResponse(statusCode = 1000, data = exampleLocation1))

        mockMvc.perform(get("/ocpi/sender/2.2/locations/$locationID")
                .header("Authorization", "Token token-c")
                .header("X-Request-ID", requestVariables.headers.requestID)
                .header("X-Correlation-ID", requestVariables.headers.correlationID)
                .header("OCPI-from-country-code", sender.country)
                .header("OCPI-from-party-id", sender.id)
                .header("OCPI-to-country-code", receiver.country)
                .header("OCPI-to-party-id", receiver.id))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("\$.status_code").value(OcpiStatus.SUCCESS.code))
                .andExpect(jsonPath("\$.status_message").doesNotExist())
                .andExpect(jsonPath("\$.timestamp").isString)
                .andExpect(jsonPath("\$.data.id").value(exampleLocation1.id))
                .andExpect(jsonPath("\$.data.party_id").value(exampleLocation1.partyID))
    }


    @Test
    fun `When GET sender Locations return single evse`() {

        val locationID = "LOC1"
        val evseUID = "12345"

        val sender = BasicRole("EMY", "DE")
        val receiver = BasicRole("IGY", "DE")

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.LOCATIONS,
                interfaceRole = InterfaceRole.SENDER,
                method = HttpMethod.GET,
                headers = OcpiRequestHeaders(
                        requestID = generateUUIDv4Token(),
                        correlationID = generateUUIDv4Token(),
                        sender = sender,
                        receiver = receiver),
                urlPathVariables = "/$locationID/$evseUID")

        val url = "https://ocpi.emsp.com/2.2/locations/$locationID/$evseUID"

        val forwardingHeaders = requestVariables.headers.copy(
                authorization = "Token token-b",
                requestID = generateUUIDv4Token())

        every { routingService.validateSender("Token token-c", sender) } just Runs
        every { routingService.validateReceiver(receiver) } returns Receiver.LOCAL
        every { routingService.prepareLocalPlatformRequest(requestVariables) } returns Pair(url, forwardingHeaders)

        every {
            httpService.makeOcpiRequest<Evse>(HttpMethod.GET, url, forwardingHeaders.toMap())
        } returns HttpResponse(
                statusCode = 200,
                headers = mapOf(),
                body = OcpiResponse(statusCode = 1000, data = exampleLocation1.evses!![0]))

        mockMvc.perform(get("/ocpi/sender/2.2/locations/$locationID/$evseUID")
                .header("Authorization", "Token token-c")
                .header("X-Request-ID", requestVariables.headers.requestID)
                .header("X-Correlation-ID", requestVariables.headers.correlationID)
                .header("OCPI-from-country-code", sender.country)
                .header("OCPI-from-party-id", sender.id)
                .header("OCPI-to-country-code", receiver.country)
                .header("OCPI-to-party-id", receiver.id))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("\$.status_code").value(OcpiStatus.SUCCESS.code))
                .andExpect(jsonPath("\$.status_message").doesNotExist())
                .andExpect(jsonPath("\$.timestamp").isString)
                .andExpect(jsonPath("\$.data.uid").value(exampleLocation1.evses!![0].uid))
                .andExpect(jsonPath("\$.data.status").value(exampleLocation1.evses!![0].status.toString()))
    }


    @Test
    fun `When GET sender Locations return single connector`() {

        val locationID = "LOC1"
        val evseUID = "12345"
        val connectorID = "2"

        val sender = BasicRole("EMY", "DE")
        val receiver = BasicRole("IGY", "DE")

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.LOCATIONS,
                interfaceRole = InterfaceRole.SENDER,
                method = HttpMethod.GET,
                headers = OcpiRequestHeaders(
                        requestID = generateUUIDv4Token(),
                        correlationID = generateUUIDv4Token(),
                        sender = sender,
                        receiver = receiver),
                urlPathVariables = "/$locationID/$evseUID/$connectorID")

        val url = "https://ocpi.emsp.com/2.2/locations/$locationID/$evseUID/$connectorID"

        val forwardingHeaders = requestVariables.headers.copy(
                authorization = "Token token-b",
                requestID = generateUUIDv4Token())

        every { routingService.validateSender("Token token-c", sender) } just Runs
        every { routingService.validateReceiver(receiver) } returns Receiver.LOCAL
        every { routingService.prepareLocalPlatformRequest(requestVariables) } returns Pair(url, forwardingHeaders)

        every {
            httpService.makeOcpiRequest<Connector>(HttpMethod.GET, url, forwardingHeaders.toMap())
        } returns HttpResponse(
                statusCode = 200,
                headers = mapOf(),
                body = OcpiResponse(statusCode = 1000, data = exampleLocation1.evses!![0].connectors[0]))

        mockMvc.perform(get("/ocpi/sender/2.2/locations/$locationID/$evseUID/$connectorID")
                .header("Authorization", "Token token-c")
                .header("X-Request-ID", requestVariables.headers.requestID)
                .header("X-Correlation-ID", requestVariables.headers.correlationID)
                .header("OCPI-from-country-code", sender.country)
                .header("OCPI-from-party-id", sender.id)
                .header("OCPI-to-country-code", receiver.country)
                .header("OCPI-to-party-id", receiver.id))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("\$.status_code").value(OcpiStatus.SUCCESS.code))
                .andExpect(jsonPath("\$.status_message").doesNotExist())
                .andExpect(jsonPath("\$.timestamp").isString)
                .andExpect(jsonPath("\$.data.id").value(exampleLocation1.evses!![0].connectors[0].id))
                .andExpect(jsonPath("\$.data.standard").value(exampleLocation1.evses!![0].connectors[0].standard.toString()))
    }


    @Test
    fun `When GET receiver Locations return single location`() {

        val locationID = "LOC23"

        val sender = BasicRole("IGY", "DE")
        val receiver = BasicRole("EMY", "DE")

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.LOCATIONS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.GET,
                headers = OcpiRequestHeaders(
                        requestID = generateUUIDv4Token(),
                        correlationID = generateUUIDv4Token(),
                        sender = sender,
                        receiver = receiver),
                urlPathVariables = "/${sender.country}/${sender.id}/$locationID")

        val url = "https://ocpi.cpo.com/2.2/locations/${sender.country}/${sender.id}/$locationID"

        val forwardingHeaders = requestVariables.headers.copy(
                authorization = "Token token-b",
                requestID = generateUUIDv4Token())

        every { routingService.validateSender("Token token-c", sender) } just Runs
        every { routingService.validateReceiver(receiver) } returns Receiver.LOCAL
        every { routingService.prepareLocalPlatformRequest(requestVariables) } returns Pair(url, forwardingHeaders)

        every {
            httpService.makeOcpiRequest<Location>(HttpMethod.GET, url, forwardingHeaders.toMap())
        } returns HttpResponse(
                statusCode = 200,
                headers = mapOf(),
                body = OcpiResponse(statusCode = 1000, data = exampleLocation2))

        mockMvc.perform(get("/ocpi/receiver/2.2/locations/${sender.country}/${sender.id}/$locationID")
                .header("Authorization", "Token token-c")
                .header("X-Request-ID", requestVariables.headers.requestID)
                .header("X-Correlation-ID", requestVariables.headers.correlationID)
                .header("OCPI-from-country-code", sender.country)
                .header("OCPI-from-party-id", sender.id)
                .header("OCPI-to-country-code", receiver.country)
                .header("OCPI-to-party-id", receiver.id))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("\$.status_code").value(OcpiStatus.SUCCESS.code))
                .andExpect(jsonPath("\$.status_message").doesNotExist())
                .andExpect(jsonPath("\$.timestamp").isString)
                .andExpect(jsonPath("\$.data.id").value(exampleLocation2.id))
                .andExpect(jsonPath("\$.data.party_id").value((exampleLocation2.partyID)))
    }


    @Test
    fun `When GET receiver Locations return single evse`() {

        val locationID = "LOC23"
        val evseUID = "4444"

        val sender = BasicRole("IGY", "DE")
        val receiver = BasicRole("EMY", "DE")

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.LOCATIONS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.GET,
                headers = OcpiRequestHeaders(
                        requestID = generateUUIDv4Token(),
                        correlationID = generateUUIDv4Token(),
                        sender = sender,
                        receiver = receiver),
                urlPathVariables = "/${sender.country}/${sender.id}/$locationID/$evseUID")

        val url = "https://ocpi.cpo.com/2.2/locations/${sender.country}/${sender.id}/$locationID/$evseUID"

        val forwardingHeaders = requestVariables.headers.copy(
                authorization = "Token token-b",
                requestID = generateUUIDv4Token())

        every { routingService.validateSender("Token token-c", sender) } just Runs
        every { routingService.validateReceiver(receiver) } returns Receiver.LOCAL
        every { routingService.prepareLocalPlatformRequest(requestVariables) } returns Pair(url, forwardingHeaders)

        every {
            httpService.makeOcpiRequest<Evse>(HttpMethod.GET, url, forwardingHeaders.toMap())
        } returns HttpResponse(
                statusCode = 200,
                headers = mapOf(),
                body = OcpiResponse(statusCode = 1000, data = exampleLocation2.evses!![0]))

        mockMvc.perform(get("/ocpi/receiver/2.2/locations/${sender.country}/${sender.id}/$locationID/$evseUID")
                .header("Authorization", "Token token-c")
                .header("X-Request-ID", requestVariables.headers.requestID)
                .header("X-Correlation-ID", requestVariables.headers.correlationID)
                .header("OCPI-from-country-code", sender.country)
                .header("OCPI-from-party-id", sender.id)
                .header("OCPI-to-country-code", receiver.country)
                .header("OCPI-to-party-id", receiver.id))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("\$.status_code").value(OcpiStatus.SUCCESS.code))
                .andExpect(jsonPath("\$.status_message").doesNotExist())
                .andExpect(jsonPath("\$.timestamp").isString)
                .andExpect(jsonPath("\$.data.uid").value(exampleLocation2.evses!![0].uid))
                .andExpect(jsonPath("\$.data.status").value((exampleLocation2.evses!![0].status.toString())))
    }


    @Test
    fun `When GET receiver Locations return single connector`() {

        val locationID = "LOC23"
        val evseUID = "4444"
        val connectorID = "2"

        val sender = BasicRole("IGY", "DE")
        val receiver = BasicRole("EMY", "DE")

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.LOCATIONS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.GET,
                headers = OcpiRequestHeaders(
                        requestID = generateUUIDv4Token(),
                        correlationID = generateUUIDv4Token(),
                        sender = sender,
                        receiver = receiver),
                urlPathVariables = "/${sender.country}/${sender.id}/$locationID/$evseUID/$connectorID")

        val url = "https://ocpi.cpo.com/2.2/locations/${sender.country}/${sender.id}/$locationID/$evseUID/$connectorID"

        val forwardingHeaders = requestVariables.headers.copy(
                authorization = "Token token-b",
                requestID = generateUUIDv4Token())

        every { routingService.validateSender("Token token-c", sender) } just Runs
        every { routingService.validateReceiver(receiver) } returns Receiver.LOCAL
        every { routingService.prepareLocalPlatformRequest(requestVariables) } returns Pair(url, forwardingHeaders)

        every {
            httpService.makeOcpiRequest<Connector>(HttpMethod.GET, url, forwardingHeaders.toMap())
        } returns HttpResponse(
                statusCode = 200,
                headers = mapOf(),
                body = OcpiResponse(statusCode = 1000, data = exampleLocation2.evses!![0].connectors[0]))

        mockMvc.perform(get("/ocpi/receiver/2.2/locations/${sender.country}/${sender.id}/$locationID/$evseUID/$connectorID")
                .header("Authorization", "Token token-c")
                .header("X-Request-ID", requestVariables.headers.requestID)
                .header("X-Correlation-ID", requestVariables.headers.correlationID)
                .header("OCPI-from-country-code", sender.country)
                .header("OCPI-from-party-id", sender.id)
                .header("OCPI-to-country-code", receiver.country)
                .header("OCPI-to-party-id", receiver.id))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("\$.status_code").value(OcpiStatus.SUCCESS.code))
                .andExpect(jsonPath("\$.status_message").doesNotExist())
                .andExpect(jsonPath("\$.timestamp").isString)
                .andExpect(jsonPath("\$.data.id").value(exampleLocation2.evses!![0].connectors[0].id))
                .andExpect(jsonPath("\$.data.format").value((exampleLocation2.evses!![0].connectors[0].format.toString())))
    }


    @Test
    fun `When PUT receiver Locations with location body return OCPI success`() {

        val locationID = "LOC23"
        val body = exampleLocation2

        val sender = BasicRole("IGY", "DE")
        val receiver = BasicRole("EMY", "DE")

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.LOCATIONS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.PUT,
                headers = OcpiRequestHeaders(
                        requestID = generateUUIDv4Token(),
                        correlationID = generateUUIDv4Token(),
                        sender = sender,
                        receiver = receiver),
                urlPathVariables = "/${sender.country}/${sender.id}/$locationID",
                body = exampleLocation2)

        val url = "https://ocpi.cpo.com/2.2/locations/${sender.country}/${sender.id}/$locationID"

        val forwardingHeaders = requestVariables.headers.copy(
                authorization = "Token token-b",
                requestID = generateUUIDv4Token())

        every { routingService.validateSender("Token token-c", sender) } just Runs
        every { routingService.validateReceiver(receiver) } returns Receiver.LOCAL
        every { routingService.prepareLocalPlatformRequest(requestVariables) } returns Pair(url, forwardingHeaders)

        val bodyMap: Map<String, Any> = mapper.readValue(mapper.writeValueAsString(requestVariables.body))

        every {
            httpService.makeOcpiRequest<Unit>(HttpMethod.PUT, url, forwardingHeaders.toMap(), json = bodyMap)
        } returns HttpResponse(
                statusCode = 200,
                headers = mapOf(),
                body = OcpiResponse(statusCode = 1000))

        mockMvc.perform(put("/ocpi/receiver/2.2/locations/${sender.country}/${sender.id}/$locationID")
                .header("Authorization", "Token token-c")
                .header("X-Request-ID", requestVariables.headers.requestID)
                .header("X-Correlation-ID", requestVariables.headers.correlationID)
                .header("OCPI-from-country-code", sender.country)
                .header("OCPI-from-party-id", sender.id)
                .header("OCPI-to-country-code", receiver.country)
                .header("OCPI-to-party-id", receiver.id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jacksonObjectMapper().writeValueAsString(body)))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("\$.status_code").value(OcpiStatus.SUCCESS.code))
                .andExpect(jsonPath("\$.status_message").doesNotExist())
                .andExpect(jsonPath("\$.data").doesNotExist())
                .andExpect(jsonPath("\$.timestamp").isString)
    }


    @Test
    fun `When PUT receiver Locations with evse body return OCPI success`() {

        val locationID = "LOC23"
        val evseUID = "5555"
        val body = exampleLocation2.evses!![0]

        val sender = BasicRole("IGY", "DE")
        val receiver = BasicRole("EMY", "DE")

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.LOCATIONS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.PUT,
                headers = OcpiRequestHeaders(
                        requestID = generateUUIDv4Token(),
                        correlationID = generateUUIDv4Token(),
                        sender = sender,
                        receiver = receiver),
                urlPathVariables = "/${sender.country}/${sender.id}/$locationID/$evseUID",
                body = body)

        val url = "https://ocpi.cpo.com/2.2/locations/${sender.country}/${sender.id}/$locationID/$evseUID"

        val forwardingHeaders = requestVariables.headers.copy(
                authorization = "Token token-b",
                requestID = generateUUIDv4Token())

        every { routingService.validateSender("Token token-c", sender) } just Runs
        every { routingService.validateReceiver(receiver) } returns Receiver.LOCAL
        every { routingService.prepareLocalPlatformRequest(requestVariables) } returns Pair(url, forwardingHeaders)

        val bodyMap: Map<String, Any> = mapper.readValue(mapper.writeValueAsString(requestVariables.body))

        every {
            httpService.makeOcpiRequest<Unit>(HttpMethod.PUT, url, forwardingHeaders.toMap(), json = bodyMap)
        } returns HttpResponse(
                statusCode = 200,
                headers = mapOf(),
                body = OcpiResponse(statusCode = 1000))

        mockMvc.perform(put("/ocpi/receiver/2.2/locations/${sender.country}/${sender.id}/$locationID/$evseUID")
                .header("Authorization", "Token token-c")
                .header("X-Request-ID", requestVariables.headers.requestID)
                .header("X-Correlation-ID", requestVariables.headers.correlationID)
                .header("OCPI-from-country-code", sender.country)
                .header("OCPI-from-party-id", sender.id)
                .header("OCPI-to-country-code", receiver.country)
                .header("OCPI-to-party-id", receiver.id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jacksonObjectMapper().writeValueAsString(body)))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("\$.status_code").value(OcpiStatus.SUCCESS.code))
                .andExpect(jsonPath("\$.status_message").doesNotExist())
                .andExpect(jsonPath("\$.data").doesNotExist())
                .andExpect(jsonPath("\$.timestamp").isString)
    }


    @Test
    fun `When PUT receiver Locations with connector body return OCPI success`() {

        val locationID = "LOC23"
        val evseUID = "5555"
        val connectorID = "1"
        val body = exampleLocation2.evses!![0].connectors[0]

        val sender = BasicRole("IGY", "DE")
        val receiver = BasicRole("EMY", "DE")

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.LOCATIONS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.PUT,
                headers = OcpiRequestHeaders(
                        requestID = generateUUIDv4Token(),
                        correlationID = generateUUIDv4Token(),
                        sender = sender,
                        receiver = receiver),
                urlPathVariables = "/${sender.country}/${sender.id}/$locationID/$evseUID/$connectorID",
                body = body)

        val url = "https://ocpi.cpo.com/2.2/locations/${sender.country}/${sender.id}/$locationID/$evseUID/$connectorID"

        val forwardingHeaders = requestVariables.headers.copy(
                authorization = "Token token-b",
                requestID = generateUUIDv4Token())

        every { routingService.validateSender("Token token-c", sender) } just Runs
        every { routingService.validateReceiver(receiver) } returns Receiver.LOCAL
        every { routingService.prepareLocalPlatformRequest(requestVariables) } returns Pair(url, forwardingHeaders)

        val bodyMap: Map<String, Any> = mapper.readValue(mapper.writeValueAsString(requestVariables.body))

        every {
            httpService.makeOcpiRequest<Unit>(HttpMethod.PUT, url, forwardingHeaders.toMap(), json = bodyMap)
        } returns HttpResponse(
                statusCode = 200,
                headers = mapOf(),
                body = OcpiResponse(statusCode = 1000))

        mockMvc.perform(put("/ocpi/receiver/2.2/locations/${sender.country}/${sender.id}/$locationID/$evseUID/$connectorID")
                .header("Authorization", "Token token-c")
                .header("X-Request-ID", requestVariables.headers.requestID)
                .header("X-Correlation-ID", requestVariables.headers.correlationID)
                .header("OCPI-from-country-code", sender.country)
                .header("OCPI-from-party-id", sender.id)
                .header("OCPI-to-country-code", receiver.country)
                .header("OCPI-to-party-id", receiver.id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jacksonObjectMapper().writeValueAsString(body)))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("\$.status_code").value(OcpiStatus.SUCCESS.code))
                .andExpect(jsonPath("\$.status_message").doesNotExist())
                .andExpect(jsonPath("\$.data").doesNotExist())
                .andExpect(jsonPath("\$.timestamp").isString)
    }


    @Test
    fun `When PATCH receiver Locations on location object return OCPI success`() {

        val locationID = "LOC23"
        val body = mapOf("type" to LocationType.ON_STREET.toString())

        val sender = BasicRole("IGY", "DE")
        val receiver = BasicRole("EMY", "DE")

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.LOCATIONS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.PATCH,
                headers = OcpiRequestHeaders(
                        requestID = generateUUIDv4Token(),
                        correlationID = generateUUIDv4Token(),
                        sender = sender,
                        receiver = receiver),
                urlPathVariables = "/${sender.country}/${sender.id}/$locationID",
                body = body)

        val url = "https://ocpi.cpo.com/2.2/locations/${sender.country}/${sender.id}/$locationID"

        val forwardingHeaders = requestVariables.headers.copy(
                authorization = "Token token-b",
                requestID = generateUUIDv4Token())

        every { routingService.validateSender("Token token-c", sender) } just Runs
        every { routingService.validateReceiver(receiver) } returns Receiver.LOCAL
        every { routingService.prepareLocalPlatformRequest(requestVariables) } returns Pair(url, forwardingHeaders)

        val bodyMap: Map<String, Any> = mapper.readValue(mapper.writeValueAsString(requestVariables.body))

        every {
            httpService.makeOcpiRequest<Unit>(HttpMethod.PATCH, url, forwardingHeaders.toMap(), json = bodyMap)
        } returns HttpResponse(
                statusCode = 200,
                headers = mapOf(),
                body = OcpiResponse(statusCode = 1000))

        mockMvc.perform(patch("/ocpi/receiver/2.2/locations/${sender.country}/${sender.id}/$locationID")
                .header("Authorization", "Token token-c")
                .header("X-Request-ID", requestVariables.headers.requestID)
                .header("X-Correlation-ID", requestVariables.headers.correlationID)
                .header("OCPI-from-country-code", sender.country)
                .header("OCPI-from-party-id", sender.id)
                .header("OCPI-to-country-code", receiver.country)
                .header("OCPI-to-party-id", receiver.id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jacksonObjectMapper().writeValueAsString(body)))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("\$.status_code").value(OcpiStatus.SUCCESS.code))
                .andExpect(jsonPath("\$.status_message").doesNotExist())
                .andExpect(jsonPath("\$.data").doesNotExist())
                .andExpect(jsonPath("\$.timestamp").isString)
    }


    @Test
    fun `When PATCH receiver Locations on evse object return OCPI success`() {

        val locationID = "LOC23"
        val evseUID = "8888"

        val body = mapOf("status" to EvseStatus.CHARGING.toString())

        val sender = BasicRole("IGY", "DE")
        val receiver = BasicRole("EMY", "DE")

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.LOCATIONS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.PATCH,
                headers = OcpiRequestHeaders(
                        requestID = generateUUIDv4Token(),
                        correlationID = generateUUIDv4Token(),
                        sender = sender,
                        receiver = receiver),
                urlPathVariables = "/${sender.country}/${sender.id}/$locationID/$evseUID",
                body = body)

        val url = "https://ocpi.cpo.com/2.2/locations/${sender.country}/${sender.id}/$locationID/$evseUID"

        val forwardingHeaders = requestVariables.headers.copy(
                authorization = "Token token-b",
                requestID = generateUUIDv4Token())

        every { routingService.validateSender("Token token-c", sender) } just Runs
        every { routingService.validateReceiver(receiver) } returns Receiver.LOCAL
        every { routingService.prepareLocalPlatformRequest(requestVariables) } returns Pair(url, forwardingHeaders)

        val bodyMap: Map<String, Any> = mapper.readValue(mapper.writeValueAsString(requestVariables.body))

        every {
            httpService.makeOcpiRequest<Unit>(HttpMethod.PATCH, url, forwardingHeaders.toMap(), json = bodyMap)
        } returns HttpResponse(
                statusCode = 200,
                headers = mapOf(),
                body = OcpiResponse(statusCode = 1000))

        mockMvc.perform(patch("/ocpi/receiver/2.2/locations/${sender.country}/${sender.id}/$locationID/$evseUID")
                .header("Authorization", "Token token-c")
                .header("X-Request-ID", requestVariables.headers.requestID)
                .header("X-Correlation-ID", requestVariables.headers.correlationID)
                .header("OCPI-from-country-code", sender.country)
                .header("OCPI-from-party-id", sender.id)
                .header("OCPI-to-country-code", receiver.country)
                .header("OCPI-to-party-id", receiver.id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jacksonObjectMapper().writeValueAsString(body)))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("\$.status_code").value(OcpiStatus.SUCCESS.code))
                .andExpect(jsonPath("\$.status_message").doesNotExist())
                .andExpect(jsonPath("\$.data").doesNotExist())
                .andExpect(jsonPath("\$.timestamp").isString)
    }


    @Test
    fun `When PATCH receiver Locations on connector object return OCPI success`() {

        val locationID = "LOC23"
        val evseUID = "8888"
        val connectorID = "1"

        val body = mapOf("tariff_ids" to listOf("xxx-123", "xxx-456"))

        val sender = BasicRole("IGY", "DE")
        val receiver = BasicRole("EMY", "DE")

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.LOCATIONS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.PATCH,
                headers = OcpiRequestHeaders(
                        requestID = generateUUIDv4Token(),
                        correlationID = generateUUIDv4Token(),
                        sender = sender,
                        receiver = receiver),
                urlPathVariables = "/${sender.country}/${sender.id}/$locationID/$evseUID/$connectorID",
                body = body)

        val url = "https://ocpi.cpo.com/2.2/locations/${sender.country}/${sender.id}/$locationID/$evseUID/$connectorID"

        val forwardingHeaders = requestVariables.headers.copy(
                authorization = "Token token-b",
                requestID = generateUUIDv4Token())

        every { routingService.validateSender("Token token-c", sender) } just Runs
        every { routingService.validateReceiver(receiver) } returns Receiver.LOCAL
        every { routingService.prepareLocalPlatformRequest(requestVariables) } returns Pair(url, forwardingHeaders)

        val bodyMap: Map<String, Any> = mapper.readValue(mapper.writeValueAsString(requestVariables.body))

        every {
            httpService.makeOcpiRequest<Unit>(HttpMethod.PATCH, url, forwardingHeaders.toMap(), json = bodyMap)
        } returns HttpResponse(
                statusCode = 200,
                headers = mapOf(),
                body = OcpiResponse(statusCode = 1000))

        mockMvc.perform(patch("/ocpi/receiver/2.2/locations/${sender.country}/${sender.id}/$locationID/$evseUID/$connectorID")
                .header("Authorization", "Token token-c")
                .header("X-Request-ID", requestVariables.headers.requestID)
                .header("X-Correlation-ID", requestVariables.headers.correlationID)
                .header("OCPI-from-country-code", sender.country)
                .header("OCPI-from-party-id", sender.id)
                .header("OCPI-to-country-code", receiver.country)
                .header("OCPI-to-party-id", receiver.id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jacksonObjectMapper().writeValueAsString(body)))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("\$.status_code").value(OcpiStatus.SUCCESS.code))
                .andExpect(jsonPath("\$.status_message").doesNotExist())
                .andExpect(jsonPath("\$.data").doesNotExist())
                .andExpect(jsonPath("\$.timestamp").isString)
    }

}