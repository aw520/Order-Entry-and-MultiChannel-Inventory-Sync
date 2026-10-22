package org.example.backend.dto;

import java.util.List;

/**
 * Inventory response for the CSR dashboard.
 *
 * items: A flat list containing one entry per (SKU, facility) pair.
 * totals: One aggregated entry per SKU, summed across all facilities.
 */
public record InventoryResponse(
        List<Item> items,
        List<Total> totals
) {
    public record Item(
            String sku,
            String facility,
            int onHandQty,
            int reservedQty,
            int amazonHoldQty,
            int walmartHoldQty,
            int csrAvailableQty
    ) {
    }

    public record Total(
            String sku,
            int onHandQty,
            int reservedQty,
            int amazonHoldQty,
            int walmartHoldQty,
            int csrAvailableQty
    ) {
    }
}
