package org.example.backend.model;

import org.jdbi.v3.core.mapper.reflect.ColumnName;

public record InventoryRow(
        String sku,
        String facility,
        @ColumnName("on_hand_qty") int onHandQty,
        @ColumnName("reserved_qty") int reservedQty,
        @ColumnName("amazon_hold_qty") int amazonHoldQty,
        @ColumnName("walmart_hold_qty") int walmartHoldQty,
        @ColumnName("csr_available_qty") int csrAvailableQty
) {
}
