package org.example.backend.dto;

import java.util.List;

public record OrderListResponse(List<OrderResponse> orders) {
}
