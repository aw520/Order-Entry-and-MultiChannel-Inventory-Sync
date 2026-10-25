package org.example.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateOrderRequest(
        @NotBlank String sku,
        @NotBlank String facility,
        @NotNull @Min(1) @Max(100000) Integer quantity
) {
}
