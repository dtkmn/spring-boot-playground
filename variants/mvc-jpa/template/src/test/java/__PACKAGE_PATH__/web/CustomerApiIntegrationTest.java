package __PACKAGE_NAME__.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class CustomerApiIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnSeededCustomer() throws Exception {
        mockMvc.perform(get("/api/v1/customers"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].firstName").value("John"))
            .andExpect(jsonPath("$[0].lastName").value("Doe"));
    }

    @Test
    void shouldReturnNotFoundErrorEnvelope() throws Exception {
        mockMvc.perform(get("/api/v1/customers/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.type").value("about:blank"))
            .andExpect(jsonPath("$.title").value("Not Found"))
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.detail").value("Customer not found"))
            .andExpect(jsonPath("$.instance").value("/api/v1/customers/999"));
    }

    @Test
    void shouldReturnValidationProblemDetails() throws Exception {
        mockMvc.perform(post("/api/v1/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"firstName\":\"\",\"lastName\":\"Doe\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.type").value("about:blank"))
            .andExpect(jsonPath("$.title").value("Bad Request"))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.detail").value("Request validation failed"))
            .andExpect(jsonPath("$.instance").value("/api/v1/customers"))
            .andExpect(jsonPath("$.errors.firstName").value("must not be blank"));
    }
}
