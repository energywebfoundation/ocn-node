package snc.openchargingnetwork.node.controllers.ocn

import com.ninjasquad.springmockk.MockkBean
import com.olisystems.ocnregistryv2_0.OcnRegistry
import io.mockk.every
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.restdocs.RestDocumentationContextProvider
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import org.web3j.tuples.generated.Tuple2
import snc.openchargingnetwork.node.config.NodeProperties


@WebMvcTest(RegistryController::class)
@ExtendWith(RestDocumentationExtension::class)
class RegistryControllerTest {

    lateinit var mockMvc: MockMvc

    @MockkBean
    lateinit var properties: NodeProperties

    @MockkBean
    lateinit var registry: OcnRegistry

    @BeforeEach
    fun setUp(webApplicationContext: WebApplicationContext,
              restDocumentation: RestDocumentationContextProvider) {

        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply<DefaultMockMvcBuilder>(MockMvcRestDocumentation.documentationConfiguration(restDocumentation))
                .build()
    }

    @Test
    fun getMyNodeInfo() {
        val expectedUrl = "https://node.ocn.org"
        val expectedAddress = "0x9bC1169Ca09555bf2721A5C9eC6D69c8073bfeB4"
        every { properties.url } returns expectedUrl
        every { properties.privateKey } returns "0x1c3e5453c0f9aa74a8eb0216310b2b013f017813a648fce364bf41dbc0b37647"
        mockMvc.perform(get("/ocn/registry/node-info"))
                .andExpect(jsonPath("\$.url").value(expectedUrl))
                .andExpect(jsonPath("\$.address").value(expectedAddress.toLowerCase()))
                .andDo(document("registry/node-info"))
    }

    @Test
    fun getNodeOf() {
        val country = "DE"
        val id = "ABC"
        val expectedUrl = "https://node.ocn.org"
        val expectedAddress = "0x22D44D286d219e1B55E6A5f1a3c82Af69716756A"
        every { registry.getOperatorByOcpi(country.toByteArray(), id.toByteArray()).sendAsync().get() } returns Tuple2(expectedAddress, expectedUrl)
        mockMvc.perform(get("/ocn/registry/node/$country/$id"))
                .andExpect(jsonPath("\$.url").value(expectedUrl))
                .andExpect(jsonPath("\$.address").value(expectedAddress))
                .andDo(document("registry/node-of"))
    }

}