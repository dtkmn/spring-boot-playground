package tech.dtkmn.examples.binancewebsocket.configuration;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import tech.dtkmn.examples.binancewebsocket.service.BinanceWebSocketClient;
import tech.dtkmn.examples.binancewebsocket.service.SpotMessageHandler;

@Configuration
public class WebSocketClientConfig {

    private final BinanceProperties binanceProperties;

    public WebSocketClientConfig(BinanceProperties binanceProperties) {
        this.binanceProperties = binanceProperties;
    }

    @Bean
    public BinanceWebSocketClient spotWebSocketClient(SpotMessageHandler spotMessageHandler) {
        List<String> symbols = binanceProperties.getSpot().getWebsocket().getSymbols();
        String streams = symbols.stream()
            .map(symbol -> symbol + "@trade")
            .collect(Collectors.joining("/"));
        String wsUrl = binanceProperties.getSpot().getWebsocket().getUrl() + "/stream?streams=" + streams;
        return new BinanceWebSocketClient(wsUrl, spotMessageHandler::handleMessage, new ReactorNettyWebSocketClient());
    }
}
