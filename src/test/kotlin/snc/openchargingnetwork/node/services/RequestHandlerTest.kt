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
import snc.openchargingnetwork.node.config.NodeProperties
import snc.openchargingnetwork.node.data.exampleLocation1
import snc.openchargingnetwork.node.components.OcpiRequestHandlerBuilder
import snc.openchargingnetwork.node.components.OcpiResponseHandlerBuilder
import snc.openchargingnetwork.node.models.*
import snc.openchargingnetwork.node.models.entities.OcnRules
import snc.openchargingnetwork.node.models.ocpi.*
import snc.openchargingnetwork.node.tools.generatePrivateKey

class RequestHandlerTest {

    private val routingService: RoutingService = mockk()
    private val registryService: RegistryService = mockk()
    private val httpService: HttpService = mockk()
    private val walletService: WalletService = mockk()
    private val hubClientInfoService: HubClientInfoService = mockk()
    private val asyncTaskService: AsyncTaskService = mockk()
    private val properties: NodeProperties = mockk()
    private val responseHandlerBuilder: OcpiResponseHandlerBuilder = mockk()

    private val requestHandlerBuilder = OcpiRequestHandlerBuilder(routingService, registryService, httpService, walletService,
            hubClientInfoService, asyncTaskService, responseHandlerBuilder, properties)

    @Test
    fun validateSender() {
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

        every { routingService.checkSenderKnown(variables.headers.authorization, variables.headers.sender) } just Runs
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

        every { registryService.isRoleKnown(variables.headers.sender, false) } returns true
        every { routingService.isRoleKnown(variables.headers.receiver) } returns true
        every { httpService.mapper.writeValueAsString(variables) } returns variablesString
        every { walletService.verify(variablesString, signature, variables.headers.sender) } just Runs
        every { properties.signatures } returns false

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

        every { routingService.getReceiverType(variables.headers.receiver) } returns Receiver.LOCAL
        every { routingService.checkSenderWhitelisted(variables.headers.sender, variables.headers.receiver, variables.module) } just Runs
        every { properties.signatures } returns false
        every { routingService.getPlatformRules(any()) } returns OcnRules(signatures = false)
        every { routingService.prepareLocalPlatformRequest(variables, false) } returns Pair(recipientUrl, outgoingHeaders)
        every { httpService.makeOcpiRequest<Unit>(recipientUrl, outgoingHeaders, variables) } returns expectedResponse
        every { routingService.isRoleKnown(variables.headers.receiver) } returns true
        every { hubClientInfoService.renewClientConnection(variables.headers.sender) } just Runs
        every { hubClientInfoService.renewClientConnection(variables.headers.receiver) } just Runs
        every { asyncTaskService.forwardToLinkedApps(variables) } just Runs
        every { registryService.getAgreementsByInterface(variables.headers.sender, variables.module, variables.interfaceRole) } returns sequenceOf()

        val response = requestHandler.forwardRequest().getResponse()
        assertEquals(expectedResponse.statusCode, response.statusCodeValue)
    }

    @Test
    fun forwardRequest_local_signatureRequired() {
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

        val senderKey = generatePrivateKey()
        val signature = Notary().sign(variables.toSignedValues(), senderKey)
        variables.headers.signature = signature.serialize()

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

        val receiverKey = generatePrivateKey()
        val receiverSig = Notary().sign(expectedResponse.toSignedValues(), receiverKey)
        expectedResponse.body.signature = receiverSig.serialize()

        every { routingService.getReceiverType(variables.headers.receiver) } returns Receiver.LOCAL
        every { routingService.checkSenderWhitelisted(variables.headers.sender, variables.headers.receiver, variables.module) } just Runs
        every { properties.signatures } returns false
        every { routingService.getPlatformRules(variables.headers.receiver) } returns OcnRules(signatures = true)
        every { registryService.getPartyDetails(variables.headers.sender) } returns RegistryPartyDetailsBasic(
                signature.signatory, "0x9bC1169Ca09555bf2721A5C9eC6D69c8073bfeB4")
        every { registryService.getPartyDetails(variables.headers.receiver) } returns RegistryPartyDetailsBasic(
                receiverSig.signatory, "0x9bC1169Ca09555bf2721A5C9eC6D69c8073bfeB4")
        every { routingService.prepareLocalPlatformRequest(variables, false) } returns Pair(recipientUrl, outgoingHeaders)
        every { httpService.makeOcpiRequest<Unit>(recipientUrl, outgoingHeaders, variables) } returns expectedResponse
        every { asyncTaskService.forwardToLinkedApps(variables) } just Runs

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

        every { routingService.getReceiverType(variables.headers.receiver) } returns Receiver.REMOTE
        every { properties.signatures } returns false
        every { routingService.prepareRemotePlatformRequest(variables, false) } returns Triple(
                recipientUrl, outgoingHeaders, outgoingBody)
        every { httpService.postOcnMessage<Unit>(recipientUrl, outgoingHeaders, outgoingBody) } returns expectedResponse
        every { hubClientInfoService.renewClientConnection(variables.headers.sender) } just Runs
        every { routingService.isRoleKnown(variables.headers.receiver) } returns false
        every { asyncTaskService.forwardToLinkedApps(variables) } just Runs

        val response = requestHandler.forwardRequest().getResponse()
        assertEquals(expectedResponse.statusCode, response.statusCodeValue)
    }

    @Test
    fun forwardRequest_remote_signatureRequired() {
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

        val privateKey = generatePrivateKey()
        val signature = Notary().sign(variables.toSignedValues(), privateKey)
        variables.headers.signature = signature.serialize()

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

        val receiverKey = generatePrivateKey()
        val receiverSig = Notary().sign(expectedResponse.toSignedValues(), receiverKey)
        expectedResponse.body.signature = receiverSig.serialize()

        every { routingService.getReceiverType(variables.headers.receiver) } returns Receiver.REMOTE
        every { properties.signatures } returns true
        every { registryService.getPartyDetails(variables.headers.sender) } returns RegistryPartyDetailsBasic(
                signature.signatory, "0x7c514d15709fb091243a4dffb649361354a9b038")
        every { registryService.getPartyDetails(variables.headers.receiver) } returns RegistryPartyDetailsBasic(
                receiverSig.signatory, "0xd49ead20b0ae060161c9ddea9b1bc46bb29b3c58")
        every { routingService.prepareRemotePlatformRequest(variables, false) } returns Triple(
                recipientUrl, outgoingHeaders, outgoingBody)
        every { httpService.postOcnMessage<Unit>(recipientUrl, outgoingHeaders, outgoingBody) } returns expectedResponse
        every { asyncTaskService.forwardToLinkedApps(variables) } just Runs


        val response = requestHandler.forwardRequest().getResponse()
        assertEquals(expectedResponse.statusCode, response.statusCodeValue)
    }

//    @Test
//    fun validateResponse() {
//        val variables = OcpiRequestVariables(
//                module = ModuleID.LOCATIONS,
//                interfaceRole = InterfaceRole.RECEIVER,
//                method = HttpMethod.GET,
//                headers = OcnHeaders(
//                        authorization = "",
//                        requestID = "123",
//                        correlationID = "456",
//                        sender = BasicRole("ABC", "DE"),
//                        receiver = BasicRole("XYZ", "DE")))
//
//        val requestHandler = requestHandlerBuilder.build<Unit>(variables)
//
//        assertThrows<UnsupportedOperationException> { requestHandler.getResponse() }
//        assertThrows<UnsupportedOperationException> { requestHandler.getResponseWithPaginationHeaders() }
//        assertThrows<UnsupportedOperationException> { requestHandler.getResponseWithLocationHeader("/proxy") }
//        assertThrows<UnsupportedOperationException> { requestHandler.getResponseWithAllHeaders() }
//    }

}