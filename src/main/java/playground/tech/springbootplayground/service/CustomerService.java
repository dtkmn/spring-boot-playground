package playground.tech.springbootplayground.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import playground.tech.springbootplayground.entity.CustomerDTO;
import playground.tech.springbootplayground.repository.CustomerRepository;
import reactor.core.publisher.Mono;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    @Autowired
    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public Mono<CustomerDTO> getCustomerById(Long id) {
        return customerRepository.findById(id)
            .map(c -> new CustomerDTO(c.getId(), c.getFirstName(), c.getLastName()));
    }

}
