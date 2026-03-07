package tech.dtkmn.examples.binancewebsocket.configuration;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "binance")
public class BinanceProperties {

    private SpotProperties spot = new SpotProperties();

    public SpotProperties getSpot() {
        return spot;
    }

    public void setSpot(SpotProperties spot) {
        this.spot = spot;
    }

    public static class SpotProperties {
        private WebSocketProperties websocket = new WebSocketProperties();

        public WebSocketProperties getWebsocket() {
            return websocket;
        }

        public void setWebsocket(WebSocketProperties websocket) {
            this.websocket = websocket;
        }
    }

    public static class WebSocketProperties {
        private String url;
        private List<String> symbols;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public List<String> getSymbols() {
            return symbols;
        }

        public void setSymbols(List<String> symbols) {
            this.symbols = symbols;
        }
    }
}
