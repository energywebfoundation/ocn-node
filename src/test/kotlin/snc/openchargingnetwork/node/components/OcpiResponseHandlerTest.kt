package snc.openchargingnetwork.node.components

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import snc.openchargingnetwork.node.config.NodeProperties
import snc.openchargingnetwork.node.models.HttpResponse
import snc.openchargingnetwork.node.models.OcnHeaders
import snc.openchargingnetwork.node.models.ocpi.*
import snc.openchargingnetwork.node.services.*

class OcpiResponseHandlerTest {

    private val routingService: RoutingService = mockk()
    private val registryService: RegistryService = mockk()
    private val hubClientInfoService: HubClientInfoService = mockk()
    private val properties: NodeProperties = mockk()

    private val responseHandlerBuilder = OcpiResponseHandlerBuilder(routingService, registryService,
            hubClientInfoService, properties)

    @Test
    fun getResponse() {
        val request = OcpiRequestVariables(
                module = ModuleID.LOCATIONS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.GET,
                headers = OcnHeaders(
                        authorization = "",
                        requestID = "123",
                        correlationID = "456",
                        sender = BasicRole("ABC", "DE"),
                        receiver = BasicRole("XYZ", "DE")))

        val response = HttpResponse<Unit>(
                statusCode = 200,
                headers = mapOf(),
                body = OcpiResponse(statusCode = 1000))

        every { properties.signatures } returns false
        every { routingService.isRoleKnown(request.headers.receiver) } returns false

        val responseHandler = responseHandlerBuilder.build(request, response)
        val actual = responseHandler.getResponse()
        assertEquals(actual.statusCodeValue, response.statusCode)
        assertEquals(actual.headers.count(), response.headers.count())
        assertEquals(actual.body, response.body)

    }

}