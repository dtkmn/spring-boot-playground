package __PACKAGE_NAME__.repository;

import jakarta.persistence.QueryHint;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import __PACKAGE_NAME__.domain.Shipment;

public interface ShipmentRepository extends Repository<Shipment, Long> {

    @Query("select s from Shipment s, PurchaseOrder o "
        + "where s.orderId = o.id and o.id = :id and o.ownerId = :owner")
    @QueryHints(@QueryHint(name = "jakarta.persistence.query.timeout", value = "5000"))
    Optional<Shipment> findAccessible(@Param("owner") String owner, @Param("id") long id);
}
