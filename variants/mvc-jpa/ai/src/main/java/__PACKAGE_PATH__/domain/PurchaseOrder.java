package __PACKAGE_NAME__.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "purchase_order")
public class PurchaseOrder {

    @Id
    private Long id;

    @Column(name = "owner_id", nullable = false, length = 100)
    private String ownerId;

    @Column(nullable = false, length = 32)
    private String status;

    protected PurchaseOrder() {
    }

    public Long getId() {
        return id;
    }

    public String getStatus() {
        return status;
    }
}
