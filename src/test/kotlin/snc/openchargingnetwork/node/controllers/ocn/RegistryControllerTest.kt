package snc.openchargingnetwork.node.controllers.ocn

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
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
import snc.openchargingnetwork.contracts.RegistryFacade
import snc.openchargingnetwork.node.config.NodeProperties
import snc.openchargingnetwork.node.services.WalletService

@WebMvcTest(RegistryController::class)
@ExtendWith(RestDocumentationExtension::class)
class RegistryControllerTest {

    lateinit var mockMvc: MockMvc

    @MockkBean
    lateinit var walletService: WalletService

    @MockkBean
    lateinit var properties: NodeProperties

    @MockkBean
    lateinit var registry: RegistryFacade

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
        val expectedAddress = "0x8e3eED126a8cc022b834DFECe0dc1D09cCe1e1F4"
        every { properties.url } returns expectedUrl
        every { walletService.credentials.address } returns expectedAddress
        mockMvc.perform(get("/ocn/registry/node-info"))
                .andExpect(jsonPath("\$.url").value(expectedUrl))
                .andExpect(jsonPath("\$.address").value(expectedAddress))
                .andDo(document("registry/node-info"))
    }

    @Test
    fun getNodeOf() {
        val country = "DE"
        val id = "ABC"
        val expectedUrl = "https://node.ocn.org"
        val expectedAddress = "0x22D44D286d219e1B55E6A5f1a3c82Af69716756A"
        every { registry.nodeURLOf(country.toByteArray(), id.toByteArray()).sendAsync().get() } returns expectedUrl
        every { registry.nodeAddressOf(country.toByteArray(), id.toByteArray()).sendAsync().get() } returns expectedAddress
        mockMvc.perform(get("/ocn/registry/node/$country/$id"))
                .andExpect(jsonPath("\$.url").value(expectedUrl))
                .andExpect(jsonPath("\$.address").value(expectedAddress))
                .andDo(document("registry/node-of"))
    }

}