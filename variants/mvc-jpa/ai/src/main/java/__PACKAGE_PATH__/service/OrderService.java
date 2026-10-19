package __PACKAGE_NAME__.service;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import __PACKAGE_NAME__.repository.OrderRepository;
import __PACKAGE_NAME__.repository.ShipmentRepository;

@Service
public class OrderService {

    private final OrderRepository orders;
    private final ShipmentRepository shipments;

    public OrderService(OrderRepository orders, ShipmentRepository shipments) {
        this.orders = orders;
        this.shipments = shipments;
    }

    @Transactional(readOnly = true, timeout = 5)
    public Optional<OrderFacts> findOrder(String owner, long id) {
        requireValidLookup(owner, id);
        return orders.findAccessible(owner, id)
            .map(order -> new OrderFacts(order.getId(), order.getStatus()));
    }

    @Transactional(readOnly = true, timeout = 5)
    public Optional<ShipmentFacts> findShipment(String owner, long id) {
        requireValidLookup(owner, id);
        return shipments.findAccessible(owner, id)
            .map(shipment -> new ShipmentFacts(shipment.getOrderId(), shipment.getStatus(),
                shipment.getDelayReason(), shipment.getEstimatedDelivery()));
    }

    private static void requireValidLookup(String owner, long id) {
        if (owner == null || owner.isBlank() || id <= 0) {
            throw new IllegalArgumentException("Invalid order lookup");
        }
    }

    public record OrderFacts(long id, String status) {
    }

    public record ShipmentFacts(long orderId, String status, String delayReason,
                                LocalDate estimatedDelivery) {
    }
}
