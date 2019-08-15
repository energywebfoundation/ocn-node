package snc.openchargingnetwork.client.controllers.ocpi.v2_2

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import snc.openchargingnetwork.client.config.Properties
import snc.openchargingnetwork.client.data.exampleCDR
import snc.openchargingnetwork.client.services.RoutingService
import snc.openchargingnetwork.client.tools.generateUUIDv4Token
import org.hamcrest.Matchers.hasSize
import org.springframework.http.HttpHeaders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import snc.openchargingnetwork.client.models.*
import snc.openchargingnetwork.client.models.entities.ProxyResourceEntity
import snc.openchargingnetwork.client.models.ocpi.*
import snc.openchargingnetwork.client.repositories.ProxyResourceRepository
import snc.openchargingnetwork.client.services.HttpRequestService

@WebMvcTest(CdrsController::class)
class CdrsControllerTest(@Autowired val mockMvc: MockMvc) {

    @MockkBean
    lateinit var routingService: RoutingService

    @MockkBean
    lateinit var httpService: HttpRequestService

    @MockkBean
    lateinit var proxyResourceRepo: ProxyResourceRepository

    @MockkBean
    lateinit var properties: Properties

    @Test
    fun `When GET sender CDRs should return paginated response`() {

        val sender = BasicRole("EMY", "DE")
        val receiver = BasicRole("ZTP", "CH")

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.Cdrs,
                interfaceRole = InterfaceRole.SENDER,
                method = HttpMethod.GET,
                requestID = generateUUIDv4Token(),
                correlationID = generateUUIDv4Token(),
                sender = sender,
                receiver = receiver,
                urlEncodedParameters = OcpiRequestParameters(limit = 100),
                expectedResponseType = OcpiResponseDataType.CDR_ARRAY)

        val url = "https://some.emsp.com/ocpi/emsp/2.2/cdrs"

        val headers = OcpiRequestHeaders(
                authorization = "Token token-b",
                requestID = generateUUIDv4Token(),
                correlationID = requestVariables.correlationID,
                ocpiFromCountryCode = sender.country,
                ocpiFromPartyID = sender.id,
                ocpiToCountryCode = receiver.country,
                ocpiToPartyID = receiver.id)

        every { routingService.validateSender("Token token-b", sender) } just Runs
        every { routingService.validateReceiver(receiver) } returns OcpiRequestType.LOCAL
        every { routingService.prepareLocalPlatformRequest(requestVariables) } returns Pair(url, headers)

        val responseHeaders = mapOf(
                "Link" to "https://some.emsp.com/actual/cdr/location/1234; rel=\"next\"",
                "X-Limit" to "100")

        every {

            httpService.makeRequest(
                method = requestVariables.method,
                url = url,
                headers = headers,
                params = requestVariables.urlEncodedParameters,
                expectedDataType = requestVariables.expectedResponseType)

        } returns HttpResponse(
                statusCode = 200,
                headers = responseHeaders,
                body = OcpiResponse(statusCode = 1000, data = arrayOf(exampleCDR)))

        val httpHeaders = HttpHeaders()
        httpHeaders["Link"] = "<PROXY_RESPONSE_URL>; rel=\"next\""
        httpHeaders["X-Limit"] = responseHeaders["X-Limit"]

        every { routingService.proxyPaginationHeaders(responseHeaders) } returns httpHeaders

        mockMvc.perform(get("/ocpi/sender/2.2/cdrs")
                .header("Authorization", headers.authorization)
                .header("X-Request-ID", requestVariables.requestID)
                .header("X-Correlation-ID", requestVariables.correlationID)
                .header("OCPI-from-country-code", requestVariables.sender.country)
                .header("OCPI-from-party-id", requestVariables.sender.id)
                .header("OCPI-to-country-code", requestVariables.receiver.country)
                .header("OCPI-to-party-id", requestVariables.receiver.id)
                .param("limit", "100"))
                .andExpect(status().isOk)
                .andExpect(header().string("Link", "<PROXY_RESPONSE_URL>; rel=\"next\""))
                .andExpect(header().string("X-Limit", "100"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("\$.status_code").value(1000))
                .andExpect(jsonPath("\$.status_message").doesNotExist())
                .andExpect(jsonPath("\$.data").isArray)
                .andExpect(jsonPath("\$.data", hasSize<Array<CDR>>(1)))
                .andExpect(jsonPath("\$.data[0].id").value(exampleCDR.id))
                .andExpect(jsonPath("\$.data[0].party_id").value(exampleCDR.partyID))
                .andExpect(jsonPath("\$.timestamp").isString)
    }

//    @Test
//    fun `When GET receiver CDRs should return single CDR`() {
//
//        val cdrID = generateUUIDv4Token()
//
//        val headers = OcpiRequestHeaders(
//                authorization = "Token ${generateUUIDv4Token()}",
//                requestID = generateUUIDv4Token(),
//                correlationID = generateUUIDv4Token(),
//                ocpiFromPartyID = "ZUB",
//                ocpiFromCountryCode = "UK",
//                ocpiToPartyID = "ABC",
//                ocpiToCountryCode = "DE")
//
//        every {
//
//            routingService.forwardRequest(
//                    proxy = true,
//                    module = ModuleID.Cdrs,
//                    interfaceRole = InterfaceRole.RECEIVER,
//                    method = HttpMethod.GET,
//                    headers = headers,
//                    urlPathVariables = cdrID,
//                    responseBodyType = OcpiResponseDataType.CDR)
//
//        } returns HttpResponse(
//                statusCode = 200,
//                headers = mapOf(),
//                body = OcpiResponse(
//                        statusCode = 1000,
//                        data = exampleCDR))
//
//        mockMvc.perform(get("/ocpi/receiver/2.2/cdrs/$cdrID")
//                .header("Authorization", headers.authorization)
//                .header("X-Request-ID", headers.requestID)
//                .header("X-Correlation-ID", headers.correlationID)
//                .header("OCPI-from-country-code", headers.ocpiFromCountryCode)
//                .header("OCPI-from-party-id", headers.ocpiFromPartyID)
//                .header("OCPI-to-country-code", headers.ocpiToCountryCode)
//                .header("OCPI-to-party-id", headers.ocpiToPartyID))
//                .andExpect(status().isOk)
//                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
//                .andExpect(jsonPath("\$.status_code").value(1000))
//                .andExpect(jsonPath("\$.status_message").doesNotExist())
//                .andExpect(jsonPath("\$.data.id").value(exampleCDR.id))
//                .andExpect(jsonPath("\$.data.party_id").value(exampleCDR.partyID))
//                .andExpect(jsonPath("\$.timestamp").isString)
//    }
//
//    @Test
//    fun `when POST receiver cdrs should return Location header`() {
//
//        val location = "https://cool.emsp.net/ocpi/receiver/2.2/cdrs/1234567890"
//
//        val headers = OcpiRequestHeaders(
//                authorization = "Token ${generateUUIDv4Token()}",
//                requestID = generateUUIDv4Token(),
//                correlationID = generateUUIDv4Token(),
//                ocpiFromPartyID = "DDD",
//                ocpiFromCountryCode = "BE",
//                ocpiToPartyID = "EEE",
//                ocpiToCountryCode = "NL")
//
//        every {
//
//            routingService.forwardRequest(
//                    module = ModuleID.Cdrs,
//                    interfaceRole = InterfaceRole.RECEIVER,
//                    method = HttpMethod.POST,
//                    headers = headers,
//                    body = exampleCDR,
//                    responseBodyType = OcpiResponseDataType.NOTHING)
//
//        } returns HttpResponse(
//                statusCode = 200,
//                headers = mapOf("Location" to location),
//                body = OcpiResponse(
//                        statusCode = 1000,
//                        data = null))
//
//        every { proxyResourceRepo.save(any<ProxyResourceEntity>()) } returns ProxyResourceEntity(
//                id = 5L,
//                resource = location,
//                sender = BasicRole(headers.ocpiFromPartyID, headers.ocpiFromCountryCode),
//                receiver = BasicRole(headers.ocpiToPartyID, headers.ocpiToCountryCode))
//
//        every { properties.url } returns "https://super.hub.net"
//
//        mockMvc.perform(post("/ocpi/receiver/2.2/cdrs")
//                .header("Authorization", headers.authorization)
//                .header("X-Request-ID", headers.requestID)
//                .header("X-Correlation-ID", headers.correlationID)
//                .header("OCPI-from-country-code", headers.ocpiFromCountryCode)
//                .header("OCPI-from-party-id", headers.ocpiFromPartyID)
//                .header("OCPI-to-country-code", headers.ocpiToCountryCode)
//                .header("OCPI-to-party-id", headers.ocpiToPartyID)
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(jacksonObjectMapper().writeValueAsString(exampleCDR)))
//                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
//                .andExpect(jsonPath("\$.status_code").value(1000))
//                .andExpect(jsonPath("\$.status_message").doesNotExist())
//                .andExpect(jsonPath("\$.data").doesNotExist())
//                .andExpect(jsonPath("\$.timestamp").isString)
//                .andExpect(header().string("Location", "https://super.hub.net/ocpi/receiver/2.2/cdrs/5"))
//    }


//        val sender = BasicRole("ZUG", "CH")
//        val receiver = BasicRole("TRE", "DE")
//        val receiverEndpoint = EndpointEntity(9L, "cdrs", InterfaceRole.RECEIVER, "http://platform.com/cdrs")
//        val headers = mapOf(
//                "Authorization" to "Token 9342",
//                "X-Request-ID" to "53245324",
//                "X-Correlation-ID" to "4567878",
//                "OCPI-from-country-code" to sender.country,
//                "OCPI-from-party-id" to sender.id,
//                "OCPI-to-country-code" to receiver.country,
//                "OCPI-to-party-id" to receiver.id)
//        every { routingService.validateSender("Token 5195923", sender, BasicRole(exampleCDR.partyID, exampleCDR.countryCode)) } returns mockk()
//        every { routingService.isRoleKnown(receiver) } returns true
//        every { routingService.getPlatformID(receiver) } returns 9L
//        every { routingService.getPlatformEndpoint(9L, "cdrs", InterfaceRole.RECEIVER) } returns receiverEndpoint
//        every { routingService.makeHeaders(9L, "4567878", sender, receiver) } returns headers
//        every { routingService.forwardRequest("POST", receiverEndpoint.url, headers, null, exampleCDR, Nothing::class) } returns HttpResponse(
//                statusCode = 200,
//                headers = mapOf("Location" to "http://platform.com/cdrs/42"),
//                body = OcpiResponse(statusCode = 1000)
//        )
//        every { routingService.saveCDR(exampleCDR.id, "http://platform.com/cdrs/42", sender, receiver) } returns mockk()
//        every { properties.url } returns "http://hub.net/"
//
//        mockMvc.perform(post("/ocpi/receiver/2.2/cdrs")
//                .header("Authorization", "Token 5195923")
//                .header("X-Request-ID", "12345")
//                .header("X-Correlation-ID", "4567878")
//                .header("OCPI-from-country-code", sender.country)
//                .header("OCPI-from-party-id", sender.id)
//                .header("OCPI-to-country-code", receiver.country)
//                .header("OCPI-to-party-id", receiver.id)
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(jacksonObjectMapper().writeValueAsString(exampleCDR)))
//                .andExpect(status().isOk)
//                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
//                .andExpect(jsonPath("\$.status_code").value(1000))
//                .andExpect(jsonPath("\$.status_message").doesNotExist())
//                .andExpect(jsonPath("\$.data").doesNotExist())
//                .andExpect(header().string("Location", "http://hub.net/ocpi/receiver/2.2/cdrs/${exampleCDR.id}"))
//    }

}