package playground.tech.springbootplayground.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import playground.tech.springbootplayground.entity.Customer;
import playground.tech.springbootplayground.entity.CustomerDTO;
import playground.tech.springbootplayground.repository.CustomerRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.when;

class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerService customerService;

    private Customer customer1;
    private Customer customer2;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        customer1 = new Customer("John", "Doe");
        customer2 = new Customer("Jane", "Doe");
        customer1.setId(1L);
        customer2.setId(2L);
    }

    @Test
    public void testGetCustomerById() {
        when(customerRepository.findById(1L)).thenReturn(Mono.just(customer1));
        Mono<CustomerDTO> result = customerService.getCustomerById(1L);
        StepVerifier.create(result)
            .expectNextMatches(c -> c.firstName().equals("John") && c.lastName().equals("Doe"))
            .verifyComplete();
    }

    @Test
    public void testGetCustomersByLastName() {
        when(customerRepository.findByLastName("Doe")).thenReturn(Flux.just(customer1, customer2));
        Flux<CustomerDTO> result = customerService.getCustomersByLastName("Doe");
        StepVerifier.create(result)
            .expectNextMatches(c -> c.firstName().equals("John") && c.lastName().equals("Doe"))
            .expectNextMatches(c -> c.firstName().equals("Jane") && c.lastName().equals("Doe"))
            .verifyComplete();
    }

}