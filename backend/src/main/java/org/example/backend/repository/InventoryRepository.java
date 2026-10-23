package org.example.backend.repository;

import java.util.List;

import org.example.backend.model.InventoryRow;
import org.jdbi.v3.spring.JdbiRepository;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

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
}
