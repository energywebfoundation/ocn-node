// TODO fix this test: after implementing the apiPrefix it started to not pass in the test providing this error: CustomModulesControllerTest > When POST custom module endpoint then return success() FAILED
package snc.openchargingnetwork.node.controllers.ocpi.v2_2

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.core.env.Environment
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import snc.openchargingnetwork.node.components.OcpiRequestHandler
import snc.openchargingnetwork.node.components.OcpiRequestHandlerBuilder
import snc.openchargingnetwork.node.models.OcnHeaders
import snc.openchargingnetwork.node.models.ocpi.*

@WebMvcTest(CustomModulesController::class)
class CustomModulesControllerTest(@Autowired val mockMvc: MockMvc) {

    @MockkBean
    lateinit var requestHandlerBuilder: OcpiRequestHandlerBuilder

    @Autowired
    lateinit var env: Environment

    lateinit var apiPrefix: String

    @BeforeEach
    fun setUp() {
        apiPrefix = env.getProperty("ocn.node.apiPrefix") ?: ""
    }

    @Test
    fun `When POST custom module endpoint then return success`() {
        val request = OcpiRequestVariables(
            module = ModuleID.CUSTOM,
            customModuleId = "my-module",
            interfaceRole = InterfaceRole.SENDER,
            method = HttpMethod.POST,
            headers = OcnHeaders("Token token", null, "1", "1",
                BasicRole("abc", "de"), BasicRole("lon", "gb")),
            urlPath = "/ocn-v2/path/1/2/3/4/5/6/7",
            queryParams = mapOf("hello" to "world", "foo" to "bar"),
            body = "{\"foo\": \"bar\"}"
        )

        val requestHandler: OcpiRequestHandler<OcpiResponse<*>> = mockk()

        every { requestHandlerBuilder.build<OcpiResponse<*>>(request) } returns requestHandler
        every { requestHandler.forwardDefault().getResponseWithAllHeaders() } returns ResponseEntity
            .ok(OcpiResponse(statusCode = 1000))

        mockMvc.perform(post("/$apiPrefix/ocpi/custom/${request.interfaceRole.id}/${request.customModuleId}/${request.urlPath?.substringAfter("/ocn-v2")}")
            .queryParam("hello", "world")
            .queryParam("foo", "bar")
            .header("authorization", request.headers.authorization)
            .header("x-request-id", request.headers.requestID)
            .header("x-correlation-id", request.headers.correlationID)
            .header("ocpi-from-country-code", request.headers.sender.country)
            .header("ocpi-from-party-id", request.headers.sender.id)
            .header("ocpi-to-country-code", request.headers.receiver.country)
            .header("ocpi-to-party-id", request.headers.receiver.id)
            .contentType(MediaType.APPLICATION_JSON)
            .content(request.body.toString()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.status_code").value(1000))
    }
}