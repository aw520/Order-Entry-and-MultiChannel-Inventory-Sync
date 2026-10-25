package org.example.backend.repository;

import java.util.List;
import java.util.Optional;

import org.example.backend.model.InventoryRow;
import org.jdbi.v3.spring.JdbiRepository;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

@JdbiRepository
@RegisterConstructorMapper(InventoryRow.class)
public interface InventoryRepository {

    @SqlQuery("""
            SELECT sku, facility, on_hand_qty, reserved_qty,
                   amazon_hold_qty, walmart_hold_qty, csr_available_qty
            FROM inventory
            ORDER BY sku, facility
            """)
    List<InventoryRow> findAll();

    @SqlQuery("""
            SELECT sku, facility, on_hand_qty, reserved_qty,
                   amazon_hold_qty, walmart_hold_qty, csr_available_qty
            FROM inventory
            WHERE sku = :sku AND facility = :facility
            """)
    Optional<InventoryRow> findBySkuAndFacility(
            @Bind("sku") String sku,
            @Bind("facility") String facility
    );

    @SqlQuery("SELECT EXISTS(SELECT 1 FROM sku WHERE sku = :sku)")
    boolean skuExists(@Bind("sku") String sku);

    @SqlQuery("SELECT EXISTS(SELECT 1 FROM facility WHERE code = :facility)")
    boolean facilityExists(@Bind("facility") String facility);

    @SqlUpdate("""
            UPDATE inventory
            SET reserved_qty = reserved_qty + :qty
            WHERE sku = :sku
              AND facility = :facility
              AND csr_available_qty >= :qty
            """)
    int reserveForCsr(
            @Bind("sku") String sku,
            @Bind("facility") String facility,
            @Bind("qty") int quantity
    );
}
