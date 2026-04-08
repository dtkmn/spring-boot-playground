package __PACKAGE_NAME__.web;

import __PACKAGE_NAME__.domain.CustomerResponse;
import __PACKAGE_NAME__.service.CustomerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.springframework.http.HttpStatus.NOT_FOUND;
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

    @Test
    void shouldReturnValidationProblemDetails() {
        webTestClient.post()
            .uri("/api/v1/customers")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{\"firstName\":\"\",\"lastName\":\"Doe\"}")
            .exchange()
            .expectStatus().isBadRequest()
            .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody()
            .jsonPath("$.type").isEqualTo("about:blank")
            .jsonPath("$.title").isEqualTo("Bad Request")
            .jsonPath("$.status").isEqualTo(400)
            .jsonPath("$.detail").isEqualTo("Request validation failed")
            .jsonPath("$.instance").isEqualTo("/api/v1/customers")
            .jsonPath("$.errors.firstName").isEqualTo("must not be blank");
    }

    @Test
    void shouldReturnNotFoundProblemDetails() {
        when(customerService.findById(99L))
            .thenReturn(Mono.error(new ResponseStatusException(NOT_FOUND, "Customer not found")));

        webTestClient.get()
            .uri("/api/v1/customers/99")
            .exchange()
            .expectStatus().isNotFound()
            .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody()
            .jsonPath("$.type").isEqualTo("about:blank")
            .jsonPath("$.title").isEqualTo("Not Found")
            .jsonPath("$.status").isEqualTo(404)
            .jsonPath("$.detail").isEqualTo("Customer not found")
            .jsonPath("$.instance").isEqualTo("/api/v1/customers/99")
            .jsonPath("$.errors").doesNotExist();
    }

    @Test
    void shouldReturnUnexpectedProblemDetails() {
        when(customerService.findAll()).thenReturn(Flux.error(new RuntimeException("boom")));

        webTestClient.get()
            .uri("/api/v1/customers")
            .exchange()
            .expectStatus().is5xxServerError()
            .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody()
            .jsonPath("$.type").isEqualTo("about:blank")
            .jsonPath("$.title").isEqualTo("Internal Server Error")
            .jsonPath("$.status").isEqualTo(500)
            .jsonPath("$.detail").isEqualTo("Unexpected error")
            .jsonPath("$.instance").isEqualTo("/api/v1/customers")
            .jsonPath("$.errors").doesNotExist();
    }
}
