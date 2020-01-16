package snc.openchargingnetwork.node.controllers.ocpi.v2_2

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.mockk
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import snc.openchargingnetwork.node.data.exampleLocation1
import snc.openchargingnetwork.node.data.exampleLocation2
import snc.openchargingnetwork.node.models.*
import snc.openchargingnetwork.node.models.ocpi.*
import snc.openchargingnetwork.node.services.RequestHandler
import snc.openchargingnetwork.node.services.RequestHandlerBuilder
import snc.openchargingnetwork.node.tools.generateUUIDv4Token
import snc.openchargingnetwork.node.tools.getTimestamp


@WebMvcTest(LocationsController::class)
class LocationsControllerTest(@Autowired val mockMvc: MockMvc) {

    @MockkBean
    lateinit var requestHandlerBuilder: RequestHandlerBuilder


    @Test
    fun `When GET sender Locations return paginated location list`() {

        val dateFrom = getTimestamp()

        val sender = BasicRole("EMY", "DE")
        val receiver = BasicRole("IGY", "DE")

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.LOCATIONS,
                interfaceRole = InterfaceRole.SENDER,
                method = HttpMethod.GET,
                headers = OcnHeaders(
                        authorization = "Token token-c",
                        requestID = generateUUIDv4Token(),
                        correlationID = generateUUIDv4Token(),
                        sender = sender,
                        receiver = receiver),
                urlEncodedParams = OcpiRequestParameters(dateFrom = dateFrom))

        val mockRequestHandler = mockk<RequestHandler<Array<Location>>>()

        val responseHeaders = HttpHeaders()
        responseHeaders["Link"] = "https://node.ocn.co/ocpi/sender/2.2/locations/page/189; rel=\"next\""
        responseHeaders["X-Limit"] = "25"
        responseHeaders["X-Total-Count"] = "500"

        every { requestHandlerBuilder.build<Array<Location>>(requestVariables) } returns mockRequestHandler

        every { mockRequestHandler.validateSender().forwardRequest().getResponseWithPaginationHeaders() } returns ResponseEntity
                .status(200)
                .headers(responseHeaders)
                .body(OcpiResponse(
                        statusCode = 1000,
                        data = arrayOf(exampleLocation1, exampleLocation2)))

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
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(header().string("Link", "https://node.ocn.co/ocpi/sender/2.2/locations/page/189; rel=\"next\""))
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
                headers = OcnHeaders(
                        authorization = "Token token-c",
                        requestID = generateUUIDv4Token(),
                        correlationID = generateUUIDv4Token(),
                        sender = sender,
                        receiver = receiver),
                urlPathVariables = "67")

        val mockRequestHandler = mockk<RequestHandler<Array<Location>>>()

        val responseHeaders = HttpHeaders()
        responseHeaders["Link"] = "https://node.ocn.co/ocpi/sender/2.2/locations/page/68; rel=\"next\""
        responseHeaders["X-Limit"] = "100"

        every { requestHandlerBuilder.build<Array<Location>>(requestVariables) } returns mockRequestHandler

        every { mockRequestHandler.validateSender().forwardRequest(true).getResponseWithPaginationHeaders() } returns ResponseEntity
                .status(200)
                .headers(responseHeaders)
                .body(OcpiResponse(
                        statusCode = 1000,
                        data = arrayOf(exampleLocation2)))

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
                .andExpect(header().string("Link", "https://node.ocn.co/ocpi/sender/2.2/locations/page/68; rel=\"next\""))
                .andExpect(header().string("X-Limit", "100"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
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
                headers = OcnHeaders(
                        authorization = "Token token-c",
                        requestID = generateUUIDv4Token(),
                        correlationID = generateUUIDv4Token(),
                        sender = sender,
                        receiver = receiver),
                urlPathVariables = locationID)

        val mockRequestHandler = mockk<RequestHandler<Location>>()

        every { requestHandlerBuilder.build<Location>(requestVariables) } returns mockRequestHandler

        every { mockRequestHandler.validateSender().forwardRequest().getResponse() } returns ResponseEntity
                .status(200)
                .body(OcpiResponse(
                        statusCode = 1000,
                        data = exampleLocation1))

        mockMvc.perform(get("/ocpi/sender/2.2/locations/$locationID")
                .header("Authorization", "Token token-c")
                .header("X-Request-ID", requestVariables.headers.requestID)
                .header("X-Correlation-ID", requestVariables.headers.correlationID)
                .header("OCPI-from-country-code", sender.country)
                .header("OCPI-from-party-id", sender.id)
                .header("OCPI-to-country-code", receiver.country)
                .header("OCPI-to-party-id", receiver.id))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
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
                headers = OcnHeaders(
                        authorization = "Token token-c",
                        requestID = generateUUIDv4Token(),
                        correlationID = generateUUIDv4Token(),
                        sender = sender,
                        receiver = receiver),
                urlPathVariables = "/$locationID/$evseUID")

        val mockRequestHandler = mockk<RequestHandler<Evse>>()

        every { requestHandlerBuilder.build<Evse>(requestVariables) } returns mockRequestHandler

        every { mockRequestHandler.validateSender().forwardRequest().getResponse() } returns ResponseEntity
                .status(200)
                .body(OcpiResponse(
                        statusCode = 1000,
                        data = exampleLocation1.evses!![0]))

        mockMvc.perform(get("/ocpi/sender/2.2/locations/$locationID/$evseUID")
                .header("Authorization", "Token token-c")
                .header("X-Request-ID", requestVariables.headers.requestID)
                .header("X-Correlation-ID", requestVariables.headers.correlationID)
                .header("OCPI-from-country-code", sender.country)
                .header("OCPI-from-party-id", sender.id)
                .header("OCPI-to-country-code", receiver.country)
                .header("OCPI-to-party-id", receiver.id))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
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
                headers = OcnHeaders(
                        authorization = "Token token-c",
                        requestID = generateUUIDv4Token(),
                        correlationID = generateUUIDv4Token(),
                        sender = sender,
                        receiver = receiver),
                urlPathVariables = "/$locationID/$evseUID/$connectorID")

        val mockRequestHandler = mockk<RequestHandler<Connector>>()

        every { requestHandlerBuilder.build<Connector>(requestVariables) } returns mockRequestHandler

        every { mockRequestHandler.validateSender().forwardRequest().getResponse() } returns ResponseEntity
                .status(200)
                .body(OcpiResponse(
                        statusCode = 1000,
                        data = exampleLocation1.evses!![0].connectors[0]))

        mockMvc.perform(get("/ocpi/sender/2.2/locations/$locationID/$evseUID/$connectorID")
                .header("Authorization", "Token token-c")
                .header("X-Request-ID", requestVariables.headers.requestID)
                .header("X-Correlation-ID", requestVariables.headers.correlationID)
                .header("OCPI-from-country-code", sender.country)
                .header("OCPI-from-party-id", sender.id)
                .header("OCPI-to-country-code", receiver.country)
                .header("OCPI-to-party-id", receiver.id))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
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
                headers = OcnHeaders(
                        authorization = "Token token-c",
                        requestID = generateUUIDv4Token(),
                        correlationID = generateUUIDv4Token(),
                        sender = sender,
                        receiver = receiver),
                urlPathVariables = "/${sender.country}/${sender.id}/$locationID")

        val mockRequestHandler = mockk<RequestHandler<Location>>()

        every { requestHandlerBuilder.build<Location>(requestVariables) } returns mockRequestHandler

        every { mockRequestHandler.validateSender().forwardRequest().getResponse() } returns ResponseEntity
                .status(200)
                .body(OcpiResponse(
                        statusCode = 1000,
                        data = exampleLocation2))

        mockMvc.perform(get("/ocpi/receiver/2.2/locations/${sender.country}/${sender.id}/$locationID")
                .header("Authorization", "Token token-c")
                .header("X-Request-ID", requestVariables.headers.requestID)
                .header("X-Correlation-ID", requestVariables.headers.correlationID)
                .header("OCPI-from-country-code", sender.country)
                .header("OCPI-from-party-id", sender.id)
                .header("OCPI-to-country-code", receiver.country)
                .header("OCPI-to-party-id", receiver.id))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
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
                headers = OcnHeaders(
                        authorization = "Token token-c",
                        requestID = generateUUIDv4Token(),
                        correlationID = generateUUIDv4Token(),
                        sender = sender,
                        receiver = receiver),
                urlPathVariables = "/${sender.country}/${sender.id}/$locationID/$evseUID")

        val mockRequestHandler = mockk<RequestHandler<Evse>>()

        every { requestHandlerBuilder.build<Evse>(requestVariables) } returns mockRequestHandler

        every { mockRequestHandler.validateSender().forwardRequest().getResponse() } returns ResponseEntity
                .status(200)
                .body(OcpiResponse(
                        statusCode = 1000,
                        data = exampleLocation2.evses!![0]))

        mockMvc.perform(get("/ocpi/receiver/2.2/locations/${sender.country}/${sender.id}/$locationID/$evseUID")
                .header("Authorization", "Token token-c")
                .header("X-Request-ID", requestVariables.headers.requestID)
                .header("X-Correlation-ID", requestVariables.headers.correlationID)
                .header("OCPI-from-country-code", sender.country)
                .header("OCPI-from-party-id", sender.id)
                .header("OCPI-to-country-code", receiver.country)
                .header("OCPI-to-party-id", receiver.id))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
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
                headers = OcnHeaders(
                        authorization = "Token token-c",
                        requestID = generateUUIDv4Token(),
                        correlationID = generateUUIDv4Token(),
                        sender = sender,
                        receiver = receiver),
                urlPathVariables = "/${sender.country}/${sender.id}/$locationID/$evseUID/$connectorID")

        val mockRequestHandler = mockk<RequestHandler<Connector>>()

        every { requestHandlerBuilder.build<Connector>(requestVariables) } returns mockRequestHandler

        every { mockRequestHandler.validateSender().forwardRequest().getResponse() } returns ResponseEntity
                .status(200)
                .body(OcpiResponse(
                        statusCode = 1000,
                        data = exampleLocation2.evses!![0].connectors[0]))

        mockMvc.perform(get("/ocpi/receiver/2.2/locations/${sender.country}/${sender.id}/$locationID/$evseUID/$connectorID")
                .header("Authorization", "Token token-c")
                .header("X-Request-ID", requestVariables.headers.requestID)
                .header("X-Correlation-ID", requestVariables.headers.correlationID)
                .header("OCPI-from-country-code", sender.country)
                .header("OCPI-from-party-id", sender.id)
                .header("OCPI-to-country-code", receiver.country)
                .header("OCPI-to-party-id", receiver.id))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
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
                headers = OcnHeaders(
                        authorization = "Token token-c",
                        requestID = generateUUIDv4Token(),
                        correlationID = generateUUIDv4Token(),
                        sender = sender,
                        receiver = receiver),
                urlPathVariables = "/${sender.country}/${sender.id}/$locationID",
                body = exampleLocation2)

        val mockRequestHandler = mockk<RequestHandler<Unit>>()

        every { requestHandlerBuilder.build<Unit>(requestVariables) } returns mockRequestHandler

        every { mockRequestHandler.validateSender().forwardRequest().getResponse() } returns ResponseEntity
                .status(200)
                .body(OcpiResponse(statusCode = 1000))

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
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
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
                headers = OcnHeaders(
                        authorization = "Token token-c",
                        requestID = generateUUIDv4Token(),
                        correlationID = generateUUIDv4Token(),
                        sender = sender,
                        receiver = receiver),
                urlPathVariables = "/${sender.country}/${sender.id}/$locationID/$evseUID",
                body = body)

        val mockRequestHandler = mockk<RequestHandler<Unit>>()

        every { requestHandlerBuilder.build<Unit>(requestVariables) } returns mockRequestHandler

        every { mockRequestHandler.validateSender().forwardRequest().getResponse() } returns ResponseEntity
                .status(200)
                .body(OcpiResponse(statusCode = 1000))

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
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
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
                headers = OcnHeaders(
                        authorization = "Token token-c",
                        requestID = generateUUIDv4Token(),
                        correlationID = generateUUIDv4Token(),
                        sender = sender,
                        receiver = receiver),
                urlPathVariables = "/${sender.country}/${sender.id}/$locationID/$evseUID/$connectorID",
                body = body)

        val mockRequestHandler = mockk<RequestHandler<Unit>>()

        every { requestHandlerBuilder.build<Unit>(requestVariables) } returns mockRequestHandler

        every { mockRequestHandler.validateSender().forwardRequest().getResponse() } returns ResponseEntity
                .status(200)
                .body(OcpiResponse(statusCode = 1000))

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
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
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
                headers = OcnHeaders(
                        authorization = "Token token-c",
                        requestID = generateUUIDv4Token(),
                        correlationID = generateUUIDv4Token(),
                        sender = sender,
                        receiver = receiver),
                urlPathVariables = "/${sender.country}/${sender.id}/$locationID",
                body = body)

        val mockRequestHandler = mockk<RequestHandler<Unit>>()

        every { requestHandlerBuilder.build<Unit>(requestVariables) } returns mockRequestHandler

        every { mockRequestHandler.validateSender().forwardRequest().getResponse() } returns ResponseEntity
                .status(200)
                .body(OcpiResponse(statusCode = 1000))

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
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("\$.status_code").value(OcpiStatus.SUCCESS.code))
                .andExpect(jsonPath("\$.status_message").doesNotExist())
                .andExpect(jsonPath("\$.data").doesNotExist())
                .andExpect(jsonPath("\$.timestamp").isString)
    }
//
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
                headers = OcnHeaders(
                        authorization = "Token token-c",
                        requestID = generateUUIDv4Token(),
                        correlationID = generateUUIDv4Token(),
                        sender = sender,
                        receiver = receiver),
                urlPathVariables = "/${sender.country}/${sender.id}/$locationID/$evseUID",
                body = body)

        val mockRequestHandler = mockk<RequestHandler<Unit>>()

        every { requestHandlerBuilder.build<Unit>(requestVariables) } returns mockRequestHandler

        every { mockRequestHandler.validateSender().forwardRequest().getResponse() } returns ResponseEntity
                .status(200)
                .body(OcpiResponse(statusCode = 1000))

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
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
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
                headers = OcnHeaders(
                        authorization = "Token token-c",
                        requestID = generateUUIDv4Token(),
                        correlationID = generateUUIDv4Token(),
                        sender = sender,
                        receiver = receiver),
                urlPathVariables = "/${sender.country}/${sender.id}/$locationID/$evseUID/$connectorID",
                body = body)

        val mockRequestHandler = mockk<RequestHandler<Unit>>()

        every { requestHandlerBuilder.build<Unit>(requestVariables) } returns mockRequestHandler

        every { mockRequestHandler.validateSender().forwardRequest().getResponse() } returns ResponseEntity
                .status(200)
                .body(OcpiResponse(statusCode = 1000))

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
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("\$.status_code").value(OcpiStatus.SUCCESS.code))
                .andExpect(jsonPath("\$.status_message").doesNotExist())
                .andExpect(jsonPath("\$.data").doesNotExist())
                .andExpect(jsonPath("\$.timestamp").isString)
    }

}