package org.example.backend.model;

import java.sql.Timestamp;

import org.jdbi.v3.core.mapper.reflect.ColumnName;

public record OrderRow(
        long id,
        String sku,
        String facility,
        String channel,
        @ColumnName("external_order_id") String externalOrderId,
        int quantity,
        String status,
        @ColumnName("is_demo") boolean isDemo,
        @ColumnName("created_at") Timestamp createdAt,
        @ColumnName("updated_at") Timestamp updatedAt
) {
}
