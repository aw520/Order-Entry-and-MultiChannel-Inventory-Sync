package org.example.backend.model;

public record InventoryRow(
        String sku,
        String facility,
        int onHandQty,
        int reservedQty,
        int amazonHoldQty,
        int walmartHoldQty,
        int csrAvailableQty
) {
}
