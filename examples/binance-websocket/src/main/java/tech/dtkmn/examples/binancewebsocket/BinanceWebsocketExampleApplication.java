package tech.dtkmn.examples.binancewebsocket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import tech.dtkmn.examples.binancewebsocket.configuration.BinanceProperties;

@SpringBootApplication
@EnableConfigurationProperties(BinanceProperties.class)
public class BinanceWebsocketExampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(BinanceWebsocketExampleApplication.class, args);
    }
}
