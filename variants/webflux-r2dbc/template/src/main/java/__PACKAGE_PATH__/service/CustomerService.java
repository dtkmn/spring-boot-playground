package __PACKAGE_NAME__.service;

import __PACKAGE_NAME__.domain.Customer;
import __PACKAGE_NAME__.domain.CustomerRequest;
import __PACKAGE_NAME__.domain.CustomerResponse;
import __PACKAGE_NAME__.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public Flux<CustomerResponse> findAll() {
        return customerRepository.findAll().map(this::toResponse);
    }

    public Mono<CustomerResponse> findById(Long id) {
        return customerRepository.findById(id)
            .map(this::toResponse)
            .switchIfEmpty(Mono.error(new ResponseStatusException(NOT_FOUND, "Customer not found")));
    }

    public Mono<CustomerResponse> create(CustomerRequest request) {
        Customer customer = new Customer(null, request.firstName(), request.lastName());
        return customerRepository.save(customer).map(this::toResponse);
    }

    private CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(customer.getId(), customer.getFirstName(), customer.getLastName());
    }
}
