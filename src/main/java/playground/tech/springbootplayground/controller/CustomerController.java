package playground.tech.springbootplayground.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import playground.tech.springbootplayground.entity.CustomerDTO;
import playground.tech.springbootplayground.service.CustomerService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/customers")
public class CustomerController {

    private final CustomerService customerService;

    @Autowired
    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping("/{id}")
    public Mono<CustomerDTO> getCustomerById(@PathVariable Long id) {
        return customerService.getCustomerById(id);
    }

    @GetMapping("/mono")
    public Mono<String> getMono() {
        return Mono.just("Hello, Mono!")
                .delayElement(Duration.ofSeconds(1));
    }

    public Flux<String> getFlux() {
        List<String> list = Arrays.asList("Hello", "Reactive", "World");
        return Flux.fromIterable(list)
                .delayElements(Duration.ofSeconds(1));
    }

}
