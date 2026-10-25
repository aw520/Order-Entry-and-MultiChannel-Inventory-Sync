package org.example.backend.service;

import org.example.backend.dto.CreateOrderRequest;
import org.example.backend.dto.OrderResponse;
import org.example.backend.error.InsufficientInventoryException;
import org.example.backend.error.NotFoundException;
import org.example.backend.model.InventoryRow;
import org.example.backend.model.OrderRow;
import org.example.backend.repository.InventoryRepository;
import org.example.backend.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderAllocationService {

    private final InventoryRepository inventoryRepository;
    private final OrderRepository orderRepository;

    public OrderAllocationService(
            InventoryRepository inventoryRepository,
            OrderRepository orderRepository
    ) {
        this.inventoryRepository = inventoryRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public OrderResponse allocate(CreateOrderRequest request) {
        /*
        allocate for csr
        */
        int affectedRows = inventoryRepository.reserveForCsr(
                request.sku(),
                request.facility(),
                request.quantity()
        );

        if (affectedRows != 1) {
            throw allocationFailure(request);
        }

        long orderId = orderRepository.insertCsrAllocated(
                request.sku(),
                request.facility(),
                request.quantity()
        );

        OrderRow order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalStateException("Created order could not be read."));
        return OrderResponse.from(order);
    }

    private RuntimeException allocationFailure(CreateOrderRequest request) {
        if (!inventoryRepository.skuExists(request.sku())) {
            return new NotFoundException(
                    "SKU_NOT_FOUND",
                    "SKU %s was not found.".formatted(request.sku())
            );
        }
        if (!inventoryRepository.facilityExists(request.facility())) {
            return new NotFoundException(
                    "FACILITY_NOT_FOUND",
                    "Facility %s was not found.".formatted(request.facility())
            );
        }

        InventoryRow inventory = inventoryRepository
                .findBySkuAndFacility(request.sku(), request.facility())
                .orElseThrow(() -> new NotFoundException(
                        "INVENTORY_NOT_FOUND",
                        "Inventory for %s at %s was not found."
                                .formatted(request.sku(), request.facility())
                ));

        return new InsufficientInventoryException(
                request.sku(),
                request.facility(),
                request.quantity(),
                inventory.csrAvailableQty()
        );
    }
}
