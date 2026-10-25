package org.example.backend.service;

import java.util.List;

import org.example.backend.dto.OrderListResponse;
import org.example.backend.dto.OrderResponse;
import org.example.backend.error.NotFoundException;
import org.example.backend.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderQueryService {

    private final OrderRepository orderRepository;

    public OrderQueryService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public OrderResponse findById(long id) {
        return orderRepository.findById(id)
                .map(OrderResponse::from)
                .orElseThrow(() -> new NotFoundException(
                        "ORDER_NOT_FOUND",
                        "Order %d was not found.".formatted(id)
                ));
    }

    @Transactional(readOnly = true)
    public OrderListResponse findRecent(int limit) {
        List<OrderResponse> orders = orderRepository.findRecent(limit).stream()
                .map(OrderResponse::from)
                .toList();
        return new OrderListResponse(orders);
    }
}
