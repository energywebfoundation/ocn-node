package snc.openchargingnetwork.node.services

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpMethod
import shareandcharge.openchargingnetwork.notary.Notary
import shareandcharge.openchargingnetwork.notary.OcpiRequest
import snc.openchargingnetwork.node.config.NodeProperties
import snc.openchargingnetwork.node.data.exampleLocation1
import snc.openchargingnetwork.node.models.HttpResponse
import snc.openchargingnetwork.node.models.OcnHeaders
import snc.openchargingnetwork.node.models.OcnMessageHeaders
import snc.openchargingnetwork.node.models.Receiver
import snc.openchargingnetwork.node.models.exceptions.OcpiClientInvalidParametersException
import snc.openchargingnetwork.node.models.ocpi.*
import snc.openchargingnetwork.node.tools.generatePrivateKey

class RequestHandlerTest {

    private val routingService: RoutingService = mockk()
    private val httpService: HttpService = mockk()
    private val walletService: WalletService = mockk()
    private val properties: NodeProperties = mockk()

    private val requestHandlerBuilder = RequestHandlerBuilder(routingService, httpService, walletService, properties)

    @Test
    fun validateSender_noSignature() {
        val variables = OcpiRequestVariables(
                module = ModuleID.LOCATIONS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.GET,
                headers = OcnHeaders(
                        authorization = "",
                        requestID = "123",
                        correlationID = "456",
                        sender = BasicRole("ABC", "DE"),
                        receiver = BasicRole("XYZ", "DE")))

        every { routingService.validateSender(variables.headers.authorization, variables.headers.sender) } just Runs
        every { properties.signatures } returns false

        val requestHandler = requestHandlerBuilder.build<Unit>(variables)
        assertDoesNotThrow { requestHandler.validateSender() }
    }

    @Test
    fun validateSender_missingSignature() {
        val variables = OcpiRequestVariables(
                module = ModuleID.LOCATIONS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.GET,
                headers = OcnHeaders(
                        authorization = "",
                        requestID = "123",
                        correlationID = "456",
                        sender = BasicRole("ABC", "DE"),
                        receiver = BasicRole("XYZ", "DE")))

        every { routingService.validateSender(variables.headers.authorization, variables.headers.sender) } just Runs
        every { properties.signatures } returns true

        val requestHandler = requestHandlerBuilder.build<Unit>(variables)
        assertThrows<OcpiClientInvalidParametersException>("Missing required header: 'OCN-Signature'") {
            requestHandler.validateSender()
        }
    }

    @Test
    fun validateSender_invalidSignature() {
        val variables = OcpiRequestVariables(
                module = ModuleID.LOCATIONS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.GET,
                headers = OcnHeaders(
                        authorization = "",
                        signature = "",
                        requestID = "123",
                        correlationID = "456",
                        sender = BasicRole("ABC", "DE"),
                        receiver = BasicRole("XYZ", "DE")))

        val privateKey = generatePrivateKey()
        val signature = Notary().sign(OcpiRequest(headers = variables.headers.toNotaryReadableHeaders(), body = "ACCEPTED"), privateKey)
        variables.headers.signature = signature.serialize()

        every { routingService.validateSender(variables.headers.authorization, variables.headers.sender) } just Runs
        every { properties.signatures } returns true


        val requestHandler = requestHandlerBuilder.build<Unit>(variables)
        assertThrows<OcpiClientInvalidParametersException>("Unable to verify signature: Request has been modified.") {
            requestHandler.validateSender()
        }
    }

    @Test
    fun validateSender_validSignature() {
        val variables = OcpiRequestVariables(
                module = ModuleID.LOCATIONS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.GET,
                headers = OcnHeaders(
                        authorization = "",
                        signature = "",
                        requestID = "123",
                        correlationID = "456",
                        sender = BasicRole("ABC", "DE"),
                        receiver = BasicRole("XYZ", "DE")))

        val privateKey = generatePrivateKey()
        val signature = Notary().sign(variables.toNotaryReadableVariables(), privateKey)
        variables.headers.signature = signature.serialize()

        every { routingService.validateSender(variables.headers.authorization, variables.headers.sender) } just Runs
        every { properties.signatures } returns true

        val requestHandler = requestHandlerBuilder.build<Unit>(variables)
        assertDoesNotThrow { requestHandler.validateSender() }
    }

    @Test
    fun validateOcnMessage() {
        val signature = "0x12345"

        val variables = OcpiRequestVariables(
                module = ModuleID.LOCATIONS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.GET,
                headers = OcnHeaders(
                        authorization = "",
                        requestID = "123",
                        correlationID = "456",
                        sender = BasicRole("ABC", "DE"),
                        receiver = BasicRole("XYZ", "DE")))

        val requestHandler = requestHandlerBuilder.build<Location>(variables)

        val variablesString = jacksonObjectMapper().writeValueAsString(variables)

        every { routingService.isRoleKnownOnNetwork(variables.headers.sender, false) } returns true
        every { routingService.isRoleKnown(variables.headers.receiver) } returns true
        every { httpService.mapper.writeValueAsString(variables) } returns variablesString
        every { walletService.verify(variablesString, signature, variables.headers.sender) } just Runs
        every { properties.signatures } returns false

        assertDoesNotThrow { requestHandler.validateOcnMessage(signature) }
    }

    @Test
    fun validateOcnMessage_signature() {
        val variables = OcpiRequestVariables(
                module = ModuleID.LOCATIONS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.GET,
                headers = OcnHeaders(
                        authorization = "",
                        signature = "xxx",
                        requestID = "123",
                        correlationID = "456",
                        sender = BasicRole("ABC", "DE"),
                        receiver = BasicRole("XYZ", "DE")))

        val requestHandler = requestHandlerBuilder.build<Location>(variables)

        val privateKey = generatePrivateKey()
        val signature = Notary().sign(variables.toNotaryReadableVariables(), privateKey).serialize()
        variables.headers.signature = signature

        val variablesString = jacksonObjectMapper().writeValueAsString(variables)

        every { routingService.isRoleKnownOnNetwork(variables.headers.sender, false) } returns true
        every { routingService.isRoleKnown(variables.headers.receiver) } returns true
        every { httpService.mapper.writeValueAsString(variables) } returns variablesString
        every { walletService.verify(variablesString, signature, variables.headers.sender) } just Runs
        every { properties.signatures } returns true

        assertDoesNotThrow { requestHandler.validateOcnMessage(signature) }
    }

    @Test
    fun forwardRequest_local() {
        val variables = OcpiRequestVariables(
                module = ModuleID.LOCATIONS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.PUT,
                headers = OcnHeaders(
                        authorization = "",
                        requestID = "123",
                        correlationID = "456",
                        sender = BasicRole("ABC", "DE"),
                        receiver = BasicRole("XYZ", "DE")),
                body = exampleLocation1)

        val requestHandler = requestHandlerBuilder.build<Unit>(variables)

        val recipientUrl = "https://msp.com/ocpi/locations"
        val outgoingHeaders = OcnHeaders(
                authorization = "Token token-b",
                requestID = "666",
                correlationID = variables.headers.correlationID,
                sender = variables.headers.sender,
                receiver = variables.headers.receiver)

        val expectedResponse = HttpResponse<Unit>(
                statusCode = 200,
                headers = mapOf(),
                body = OcpiResponse(1000))

        every { routingService.validateReceiver(variables.headers.receiver) } returns Receiver.LOCAL
        every { routingService.prepareLocalPlatformRequest(variables, false) } returns Pair(recipientUrl, outgoingHeaders)
        every { httpService.makeOcpiRequest<Unit>(recipientUrl, outgoingHeaders, variables) } returns expectedResponse

        val response = requestHandler.forwardRequest().getResponse()
        assertEquals(expectedResponse.statusCode, response.statusCodeValue)
    }

    @Test
    fun forwardRequest_remote() {
        val variables = OcpiRequestVariables(
                module = ModuleID.LOCATIONS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.PUT,
                headers = OcnHeaders(
                        authorization = "",
                        requestID = "123",
                        correlationID = "456",
                        sender = BasicRole("ABC", "DE"),
                        receiver = BasicRole("XYZ", "DE")),
                body = exampleLocation1)

        val requestHandler = requestHandlerBuilder.build<Unit>(variables)

        val recipientUrl = "https://node.ocn.com/ocpi/locations"
        val outgoingHeaders = OcnMessageHeaders(
                signature = "0x12345",
                requestID = "666")
        val outgoingBody = jacksonObjectMapper().writeValueAsString(variables)

        val expectedResponse = HttpResponse<Unit>(
                statusCode = 200,
                headers = mapOf(),
                body = OcpiResponse(1000))

        every { routingService.validateReceiver(variables.headers.receiver) } returns Receiver.REMOTE

        every { routingService.prepareRemotePlatformRequest(variables, false) } returns Triple(recipientUrl, outgoingHeaders, outgoingBody)
        every { httpService.postOcnMessage<Unit>(recipientUrl, outgoingHeaders, outgoingBody) } returns expectedResponse

        val response = requestHandler.forwardRequest().getResponse()
        assertEquals(expectedResponse.statusCode, response.statusCodeValue)
    }

    @Test
    fun validateResponse() {
        val variables = OcpiRequestVariables(
                module = ModuleID.LOCATIONS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.GET,
                headers = OcnHeaders(
                        authorization = "",
                        requestID = "123",
                        correlationID = "456",
                        sender = BasicRole("ABC", "DE"),
                        receiver = BasicRole("XYZ", "DE")))

        val requestHandler = requestHandlerBuilder.build<Unit>(variables)

        assertThrows<UnsupportedOperationException> { requestHandler.getResponse() }
        assertThrows<UnsupportedOperationException> { requestHandler.getResponseWithPaginationHeaders() }
        assertThrows<UnsupportedOperationException> { requestHandler.getResponseWithLocationHeader("/proxy") }
        assertThrows<UnsupportedOperationException> { requestHandler.getResponseWithAllHeaders() }
    }

}