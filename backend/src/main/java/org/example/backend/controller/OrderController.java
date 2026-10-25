package org.example.backend.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.example.backend.dto.CreateOrderRequest;
import org.example.backend.dto.OrderListResponse;
import org.example.backend.dto.OrderResponse;
import org.example.backend.service.OrderAllocationService;
import org.example.backend.service.OrderQueryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderAllocationService orderAllocationService;
    private final OrderQueryService orderQueryService;

    public OrderController(
            OrderAllocationService orderAllocationService,
            OrderQueryService orderQueryService
    ) {
        this.orderAllocationService = orderAllocationService;
        this.orderQueryService = orderQueryService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderAllocationService.allocate(request));
    }

    @GetMapping("/{id}")
    public OrderResponse getOrder(@PathVariable long id) {
        return orderQueryService.findById(id);
    }

    @GetMapping
    public OrderListResponse getRecentOrders(
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit
    ) {
        return orderQueryService.findRecent(limit);
    }
}
