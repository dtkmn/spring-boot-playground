package playground.tech.springbootplayground.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import playground.tech.springbootplayground.service.BinanceWebSocketClient;
import playground.tech.springbootplayground.service.FuturesMessageHandler;
import playground.tech.springbootplayground.service.SpotMessageHandler;

import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class WebSocketClientConfig {

    private final BinanceProperties binanceProperties;

    public WebSocketClientConfig(BinanceProperties binanceProperties) {
        this.binanceProperties = binanceProperties;
    }

    @Bean
    public BinanceWebSocketClient spotWebSocketClient(SpotMessageHandler spotMessageHandler) {
        List<String> symbolsList = binanceProperties.getSpot().getWebsocket().getSymbols();
        String streams = symbolsList.stream()
                .map(symbol -> symbol + "@trade")
                .collect(Collectors.joining("/"));
        String wsUrl = binanceProperties.getSpot().getWebsocket().getUrl() + "/stream?streams=" + streams;
        return new BinanceWebSocketClient(wsUrl, spotMessageHandler::handleMessage, new ReactorNettyWebSocketClient());
    }

    @Bean
    public BinanceWebSocketClient futuresWebSocketClient(FuturesMessageHandler futuresMessageHandler) {
        List<String> symbolsList = binanceProperties.getFutures().getWebsocket().getSymbols();
        String streams = symbolsList.stream()
                .map(symbol -> symbol + "@markPrice")
                .collect(Collectors.joining("/"));
        String wsUrl = binanceProperties.getFutures().getWebsocket().getUrl() + "/stream?streams=" + streams;
        return new BinanceWebSocketClient(wsUrl, futuresMessageHandler::handleMessage, new ReactorNettyWebSocketClient());
    }

}
