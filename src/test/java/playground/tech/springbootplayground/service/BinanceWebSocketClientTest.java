package playground.tech.springbootplayground.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

public class BinanceWebSocketClientTest {

    @Mock
    private ReactorNettyWebSocketClient mockClient;

    @Mock
    private Consumer<String> mockMessageHandler;

    private BinanceWebSocketClient binanceWebSocketClient;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        // Stub the client.execute method before instantiation
        when(mockClient.execute(any(URI.class), any())).thenAnswer(invocation -> {
            URI uri = invocation.getArgument(0);
            WebSocketHandler handler = invocation.getArgument(1);

            // Mock the WebSocketSession and WebSocketMessage
            WebSocketSession mockSession = mock(WebSocketSession.class);
            WebSocketMessage mockMessage = mock(WebSocketMessage.class);
            when(mockMessage.getPayloadAsText()).thenReturn("test message");
            when(mockSession.receive()).thenReturn(Flux.just(mockMessage));

            // Apply the handler to the mock session
            return handler.handle(mockSession);
        });
        binanceWebSocketClient = new BinanceWebSocketClient("wsUrl", mockMessageHandler, mockClient);
    }

    @Test
    void testStartWebSocketConnection() throws InterruptedException {
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockMessageHandler, times(1)).accept(messageCaptor.capture());
        assertEquals("test message", messageCaptor.getValue());
    }
}
