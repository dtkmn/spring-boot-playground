package playground.tech.springbootplayground.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "binance")
public class BinanceProperties {

    private SpotProperties spot;
    private FuturesProperties futures;

    public SpotProperties getSpot() {
        return spot;
    }

    public void setSpot(SpotProperties spot) {
        this.spot = spot;
    }

    public FuturesProperties getFutures() {
        return futures;
    }

    public void setFutures(FuturesProperties futures) {
        this.futures = futures;
    }

    public static class SpotProperties {
        private WebSocketProperties websocket;

        public WebSocketProperties getWebsocket() {
            return websocket;
        }

        public void setWebsocket(WebSocketProperties websocket) {
            this.websocket = websocket;
        }

    }

    public static class FuturesProperties {
        private WebSocketProperties websocket;
        private ApiProperties openInterest;
        private ApiProperties fundingRate;

        public WebSocketProperties getWebsocket() {
            return websocket;
        }

        public void setWebsocket(WebSocketProperties websocket) {
            this.websocket = websocket;
        }

        public ApiProperties getOpenInterest() {
            return openInterest;
        }

        public void setOpenInterest(ApiProperties openInterest) {
            this.openInterest = openInterest;
        }

        public ApiProperties getFundingRate() {
            return fundingRate;
        }

        public void setFundingRate(ApiProperties fundingRate) {
            this.fundingRate = fundingRate;
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

    private static class ApiProperties {
        private String apiUrl;

        public String getApiUrl() {
            return apiUrl;
        }

        public void setApiUrl(String apiUrl) {
            this.apiUrl = apiUrl;
        }
    }

}
