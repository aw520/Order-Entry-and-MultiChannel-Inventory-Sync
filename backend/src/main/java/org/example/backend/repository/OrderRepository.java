package org.example.backend.repository;

import java.util.List;
import java.util.Optional;

import org.example.backend.model.OrderRow;
import org.jdbi.v3.spring.JdbiRepository;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

@JdbiRepository
@RegisterConstructorMapper(OrderRow.class)
public interface OrderRepository {

    @SqlUpdate("""
            INSERT INTO customer_order
                (sku, facility, channel, quantity, status)
            VALUES (:sku, :facility, 'CSR', :qty, 'ALLOCATED')
            """)
    @GetGeneratedKeys("id")
    long insertCsrAllocated(
            @Bind("sku") String sku,
            @Bind("facility") String facility,
            @Bind("qty") int quantity
    );

    @SqlQuery("""
            SELECT id, sku, facility, channel, external_order_id, quantity,
                   status, created_at, updated_at
            FROM customer_order
            WHERE id = :id
            """)
    Optional<OrderRow> findById(@Bind("id") long id);

    @SqlQuery("""
            SELECT id, sku, facility, channel, external_order_id, quantity,
                   status, created_at, updated_at
            FROM customer_order
            ORDER BY created_at DESC, id DESC
            LIMIT :limit
            """)
    List<OrderRow> findRecent(@Bind("limit") int limit);
}
