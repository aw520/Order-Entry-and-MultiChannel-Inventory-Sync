package org.example.backend.error;

public class InsufficientInventoryException extends RuntimeException {

    public InsufficientInventoryException(
            String sku,
            String facility,
            int requestedQuantity,
            int availableQuantity
    ) {
        super(message(sku, facility, requestedQuantity, availableQuantity));
    }

    private static String message(
            String sku,
            String facility,
            int requestedQuantity,
            int availableQuantity
    ) {
        String availability = availableQuantity == 1
                ? "Only 1 CSR unit is available"
                : "Only %d CSR units are available".formatted(availableQuantity);
        return "%s for %s at %s; requested %d."
                .formatted(availability, sku, facility, requestedQuantity);
    }
}
