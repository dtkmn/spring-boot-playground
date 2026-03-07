package __PACKAGE_NAME__.web;

import __PACKAGE_NAME__.domain.CustomerResponse;
import __PACKAGE_NAME__.service.CustomerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebFluxTest(CustomerController.class)
class CustomerControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private CustomerService customerService;

    @Test
    void shouldReturnCustomers() {
        when(customerService.findAll()).thenReturn(Flux.just(new CustomerResponse(1L, "John", "Doe")));

        webTestClient.get()
            .uri("/api/v1/customers")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$[0].firstName").isEqualTo("John");
    }

    @Test
    void shouldCreateCustomer() {
        when(customerService.create(any())).thenReturn(Mono.just(new CustomerResponse(2L, "Jane", "Doe")));

        webTestClient.post()
            .uri("/api/v1/customers")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{\"firstName\":\"Jane\",\"lastName\":\"Doe\"}")
            .exchange()
            .expectStatus().isCreated()
            .expectBody()
            .jsonPath("$.id").isEqualTo(2);
    }
}
