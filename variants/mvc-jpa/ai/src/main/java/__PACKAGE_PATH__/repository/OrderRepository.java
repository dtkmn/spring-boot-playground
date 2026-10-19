package __PACKAGE_NAME__.repository;

import jakarta.persistence.QueryHint;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import __PACKAGE_NAME__.domain.PurchaseOrder;

public interface OrderRepository extends Repository<PurchaseOrder, Long> {

    @Query("select o from PurchaseOrder o where o.id = :id and o.ownerId = :owner")
    @QueryHints(@QueryHint(name = "jakarta.persistence.query.timeout", value = "5000"))
    Optional<PurchaseOrder> findAccessible(@Param("owner") String owner, @Param("id") long id);
}
