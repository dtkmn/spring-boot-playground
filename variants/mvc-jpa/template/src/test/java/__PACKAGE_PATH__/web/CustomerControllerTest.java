package __PACKAGE_NAME__.web;

import __PACKAGE_NAME__.domain.CustomerResponse;
import __PACKAGE_NAME__.service.CustomerService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerController.class)
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomerService customerService;

    @Test
    void shouldReturnCustomers() throws Exception {
        when(customerService.findAll()).thenReturn(List.of(new CustomerResponse(1L, "John", "Doe")));

        mockMvc.perform(get("/api/v1/customers"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].firstName").value("John"));
    }

    @Test
    void shouldCreateCustomer() throws Exception {
        when(customerService.create(new __PACKAGE_NAME__.domain.CustomerRequest("Jane", "Doe")))
            .thenReturn(new CustomerResponse(2L, "Jane", "Doe"));

        mockMvc.perform(post("/api/v1/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"firstName\":\"Jane\",\"lastName\":\"Doe\"}"))
            .andExpect(status().isCreated())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(2));
    }

    @Test
    void shouldReturnValidationErrorEnvelope() throws Exception {
        mockMvc.perform(post("/api/v1/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"firstName\":\"\",\"lastName\":\"Doe\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("Request validation failed"))
            .andExpect(jsonPath("$.path").value("/api/v1/customers"))
            .andExpect(jsonPath("$.validationErrors.firstName").value("must not be blank"));
    }

    @Test
    void shouldReturnNotFoundErrorEnvelope() throws Exception {
        when(customerService.findById(99L))
            .thenThrow(new ResponseStatusException(NOT_FOUND, "Customer not found"));

        mockMvc.perform(get("/api/v1/customers/99"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message").value("Customer not found"))
            .andExpect(jsonPath("$.path").value("/api/v1/customers/99"))
            .andExpect(jsonPath("$.validationErrors").isEmpty());
    }
}
