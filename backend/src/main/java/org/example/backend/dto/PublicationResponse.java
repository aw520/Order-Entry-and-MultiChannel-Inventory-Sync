package org.example.backend.dto;

import java.util.List;

public record PublicationResponse(
        String mode,
        List<Item> publications
) {
    public record Item(
            String channel,
            String sku,
            String facility,
            int quantity
    ) {
    }
}
