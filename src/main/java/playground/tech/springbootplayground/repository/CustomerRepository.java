package playground.tech.springbootplayground.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.data.repository.query.Param;
import playground.tech.springbootplayground.entity.Customer;
import reactor.core.publisher.Flux;

public interface CustomerRepository extends ReactiveCrudRepository<Customer, Long> {

    @Query("SELECT * FROM customer WHERE last_name = :lastName")
    Flux<Customer> findByLastName(@Param("lastName") String lastName);

}
