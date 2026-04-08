package __PACKAGE_NAME__.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Testcontainers
class CustomerApiIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void shouldReturnSeededCustomer() {
        webTestClient.get()
            .uri("/api/v1/customers")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$[0].firstName").isEqualTo("John")
            .jsonPath("$[0].lastName").isEqualTo("Doe");
    }

    @Test
    void shouldReturnNotFoundProblemDetails() {
        webTestClient.get()
            .uri("/api/v1/customers/999")
            .exchange()
            .expectStatus().isNotFound()
            .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody()
            .jsonPath("$.type").isEqualTo("about:blank")
            .jsonPath("$.title").isEqualTo("Not Found")
            .jsonPath("$.status").isEqualTo(404)
            .jsonPath("$.detail").isEqualTo("Customer not found")
            .jsonPath("$.instance").isEqualTo("/api/v1/customers/999");
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
}
