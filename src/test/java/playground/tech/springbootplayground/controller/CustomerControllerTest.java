package playground.tech.springbootplayground.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import playground.tech.springbootplayground.entity.CustomerDTO;
import playground.tech.springbootplayground.service.CustomerService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;


@WebFluxTest(CustomerController.class)
class CustomerControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private CustomerService customerService;

    private CustomerDTO customer1;
    private CustomerDTO customer2;

    @BeforeEach
    public void setUp() {
        customer1 = new CustomerDTO(1L, "John", "Doe");
        customer2 = new CustomerDTO(2L, "Jane", "Doe");
    }

    @Test
    void testGetCustomerById() {
        when(customerService.getCustomerById(anyLong())).thenReturn(Mono.just(customer1));
        webTestClient.get().uri("/customers/1")
            .exchange()
            .expectStatus().isOk()
            .expectBody(CustomerDTO.class)
            .isEqualTo(customer1);
    }

    @Test
    void testGetCustomersByLastName() {
        when(customerService.getCustomersByLastName("Doe")).thenReturn(Flux.just(customer1, customer2));
        webTestClient.get().uri("/customers/lastname/Doe")
            .exchange()
            .expectStatus().isOk()
            .expectBodyList(CustomerDTO.class)
            .hasSize(2)
            .contains(customer1, customer2);
    }

}