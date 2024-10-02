package playground.tech.springbootplayground;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import playground.tech.springbootplayground.configuration.BinanceProperties;

@SpringBootApplication
@EnableConfigurationProperties(BinanceProperties.class)
public class SpringBootPlaygroundApplication {

	private static final Logger log = LoggerFactory.getLogger(SpringBootPlaygroundApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(SpringBootPlaygroundApplication.class, args);
	}

}
