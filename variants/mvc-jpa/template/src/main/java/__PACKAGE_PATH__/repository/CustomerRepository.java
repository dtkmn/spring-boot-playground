package __PACKAGE_NAME__.repository;

import __PACKAGE_NAME__.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
}
