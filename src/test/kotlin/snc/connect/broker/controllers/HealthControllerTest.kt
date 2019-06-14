package snc.connect.broker.controllers

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(HealthController::class)
class HealthControllerTest(@Autowired val mockMvc: MockMvc) {

    @Test
    fun `When GET health should return OK`() {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk)
                .andExpect(content().string("OK"))
    }
}