package org.example.backend.dto;

import java.time.Instant;

import org.example.backend.model.OrderRow;

public record OrderResponse(
        long id,
        String sku,
        String facility,
        String channel,
        String externalOrderId,
        int quantity,
        String status,
        boolean isDemo,
        Instant createdAt,
        Instant updatedAt
) {
    public static OrderResponse from(OrderRow row) {
        return new OrderResponse(
                row.id(),
                row.sku(),
                row.facility(),
                row.channel(),
                row.externalOrderId(),
                row.quantity(),
                row.status(),
                row.isDemo(),
                row.createdAt().toInstant(),
                row.updatedAt().toInstant()
        );
    }
}
