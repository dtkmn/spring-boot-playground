package __PACKAGE_NAME__.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "shipment")
public class Shipment {

    @Id
    @Column(name = "order_id")
    private Long orderId;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "delay_reason", length = 1000)
    private String delayReason;

    @Column(name = "estimated_delivery")
    private LocalDate estimatedDelivery;

    protected Shipment() {
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getStatus() {
        return status;
    }

    public String getDelayReason() {
        return delayReason;
    }

    public LocalDate getEstimatedDelivery() {
        return estimatedDelivery;
    }
}
