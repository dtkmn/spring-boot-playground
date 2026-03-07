package __PACKAGE_NAME__.service;

import __PACKAGE_NAME__.domain.Customer;
import __PACKAGE_NAME__.domain.CustomerRequest;
import __PACKAGE_NAME__.domain.CustomerResponse;
import __PACKAGE_NAME__.repository.CustomerRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public List<CustomerResponse> findAll() {
        return customerRepository.findAll().stream().map(this::toResponse).toList();
    }

    public CustomerResponse findById(Long id) {
        return customerRepository.findById(id)
            .map(this::toResponse)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Customer not found"));
    }

    public CustomerResponse create(CustomerRequest request) {
        Customer customer = new Customer(null, request.firstName(), request.lastName());
        return toResponse(customerRepository.save(customer));
    }

    private CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(customer.getId(), customer.getFirstName(), customer.getLastName());
    }
}
