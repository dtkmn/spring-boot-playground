package __PACKAGE_NAME__.ai;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;
import __PACKAGE_NAME__.service.OrderService;

/** Request-owned state. Identity is supplied by the authenticated HTTP boundary. */
final class InvocationBudget {
    static final String CONTEXT_KEY = "orderInvocation";
    final String owner;
    final long orderId;
    private final long expiresAt;
    private final AtomicBoolean canceled = new AtomicBoolean();
    int rounds;
    int toolCalls;
    boolean orderLookedUp;
    boolean shipmentLookedUp;
    OrderService.OrderFacts order;
    OrderService.ShipmentFacts shipment;

    InvocationBudget(String owner, long orderId, Duration deadline) {
        this.owner = owner;
        this.orderId = orderId;
        this.expiresAt = System.nanoTime() + deadline.toNanos();
    }

    long remainingNanos() { return Math.max(0, expiresAt - System.nanoTime()); }
    void cancel() { canceled.set(true); }

    void check() {
        if (canceled.get() || Thread.currentThread().isInterrupted() || remainingNanos() == 0) {
            throw new ResponseStatusException(GATEWAY_TIMEOUT, "Assistant deadline exceeded");
        }
    }

    void beforeModel() {
        check();
        // An owner-filtered empty lookup is conclusive; no further inference is useful.
        if (orderLookedUp && order == null) {
            throw new ResponseStatusException(NOT_FOUND, "Order not accessible");
        }
        if (++rounds > 1) unavailable();
    }

    void beforeTool(String name, long requestedId) {
        check();
        if (requestedId != orderId || requestedId <= 0 || ++toolCalls > 2) unavailable();
        if (name.equals("getOrder")) {
            if (orderLookedUp) unavailable();
        } else if (name.equals("getShipment")) {
            if (!orderLookedUp || order == null || shipmentLookedUp) unavailable();
        } else unavailable();
    }

    static void unavailable() {
        throw new ResponseStatusException(SERVICE_UNAVAILABLE, "Assistant unavailable");
    }
}
