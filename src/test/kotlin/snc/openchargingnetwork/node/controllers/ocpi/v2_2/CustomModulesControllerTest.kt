package snc.openchargingnetwork.node.controllers.ocpi.v2_2

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(CustomModulesController::class)
class CustomModulesControllerTest(@Autowired val mockMvc: MockMvc) {

    @Test
    fun providesCustomModuleEndpoint() {
        mockMvc.perform(get("/ocpi/custom/my-module/sender/path/1/2/3/5/6/7?hello=world&foo=bar")
                .header("authorization", "Token abc123")
                .header("x-request-id", "1")
                .header("x-correlation-id", "1")
                .header("ocpi-from-country-code", "de")
                .header("ocpi-from-party-id", "abc")
                .header("ocpi-to-country-code", "gb")
                .header("ocpi-to-party-id", "lon")
                .contentType("application/json")
                .content("{\"foo\": \"bar\"}"))
                .andExpect(status().isOk)
    }

}