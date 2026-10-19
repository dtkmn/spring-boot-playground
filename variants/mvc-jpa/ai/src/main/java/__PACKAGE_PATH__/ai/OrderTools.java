package __PACKAGE_NAME__.ai;

import io.micrometer.core.instrument.MeterRegistry;
import __PACKAGE_NAME__.service.OrderService;

/** Typed, read-only business lookups. Only Java supplies the authenticated owner and selected ID. */
final class OrderTools {
    private final OrderService orders;
    private final MeterRegistry meters;

    OrderTools(OrderService orders, MeterRegistry meters) { this.orders = orders; this.meters = meters; }

    void getOrder(InvocationBudget budget) { lookup("getOrder", budget); }
    void getShipment(InvocationBudget budget) { lookup("getShipment", budget); }

    private void lookup(String name, InvocationBudget budget) {
        String outcome = "unavailable";
        try {
            budget.beforeTool(name, budget.orderId);
            if (name.equals("getOrder")) {
                var facts = orders.findOrder(budget.owner, budget.orderId);
                budget.check();
                budget.orderLookedUp = true;
                budget.order = facts.orElse(null);
                outcome = facts.isPresent() ? "found" : "missing";
            } else {
                var facts = orders.findShipment(budget.owner, budget.orderId);
                budget.check();
                budget.shipmentLookedUp = true;
                budget.shipment = facts.orElse(null);
                outcome = facts.isPresent() ? "found" : "missing";
            }
        } finally {
            meters.counter("pilot.ai.tool.calls", "tool", name, "outcome", outcome).increment();
        }
    }
}
