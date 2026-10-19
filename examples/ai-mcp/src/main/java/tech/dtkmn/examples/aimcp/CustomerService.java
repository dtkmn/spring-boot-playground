package tech.dtkmn.examples.aimcp;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

@Service
public class CustomerService {
    // Synthetic fixtures keep this integration example independent of a database.
    private final Map<Long, Customer> customers = Map.of(1L, new Customer(1, "John", "Doe"));

    public Optional<Customer> findById(long id) {
        if (id < 1) {
            throw new IllegalArgumentException("Customer ID must be positive");
        }
        return Optional.ofNullable(customers.get(id));
    }

    public record Customer(long id, String firstName, String lastName) {}
}
