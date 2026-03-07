package __PACKAGE_NAME__.repository;

import __PACKAGE_NAME__.domain.Customer;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface CustomerRepository extends ReactiveCrudRepository<Customer, Long> {
}
