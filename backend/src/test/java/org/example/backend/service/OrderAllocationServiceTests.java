package org.example.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

import org.example.backend.dto.CreateOrderRequest;
import org.example.backend.dto.OrderResponse;
import org.example.backend.error.InsufficientInventoryException;
import org.example.backend.error.NotFoundException;
import org.example.backend.model.InventoryRow;
import org.example.backend.model.OrderRow;
import org.example.backend.repository.InventoryRepository;
import org.example.backend.repository.OrderRepository;
import org.junit.jupiter.api.Test;

class OrderAllocationServiceTests {

    private final InventoryRepository inventoryRepository = mock(InventoryRepository.class);
    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final OrderAllocationService service =
            new OrderAllocationService(inventoryRepository, orderRepository);

    @Test
    void reservesInventoryAndCreatesCsrOrder() {
        CreateOrderRequest request = new CreateOrderRequest("TShirt-White", "LA-DC", 1);
        Instant now = Instant.parse("2026-10-22T19:00:00Z");
        OrderRow saved = new OrderRow(
                14L,
                request.sku(),
                request.facility(),
                "CSR",
                null,
                request.quantity(),
                "ALLOCATED",
                Timestamp.from(now),
                Timestamp.from(now)
        );
        when(inventoryRepository.reserveForCsr(request.sku(), request.facility(), 1)).thenReturn(1);
        when(orderRepository.insertCsrAllocated(request.sku(), request.facility(), 1)).thenReturn(14L);
        when(orderRepository.findById(14L)).thenReturn(Optional.of(saved));

        OrderResponse response = service.allocate(request);

        assertThat(response.id()).isEqualTo(14L);
        assertThat(response.channel()).isEqualTo("CSR");
        assertThat(response.status()).isEqualTo("ALLOCATED");
        assertThat(response.createdAt()).isEqualTo(now);
        verify(inventoryRepository).reserveForCsr(request.sku(), request.facility(), 1);
        verify(orderRepository).insertCsrAllocated(request.sku(), request.facility(), 1);
    }

    @Test
    void rejectsRequestWhenCsrInventoryIsInsufficient() {
        CreateOrderRequest request = new CreateOrderRequest("TShirt-White", "LA-DC", 2);
        when(inventoryRepository.reserveForCsr(request.sku(), request.facility(), 2)).thenReturn(0);
        when(inventoryRepository.skuExists(request.sku())).thenReturn(true);
        when(inventoryRepository.facilityExists(request.facility())).thenReturn(true);
        when(inventoryRepository.findBySkuAndFacility(request.sku(), request.facility()))
                .thenReturn(Optional.of(new InventoryRow(
                        request.sku(), request.facility(), 10, 7, 1, 1, 1
                )));

        assertThatThrownBy(() -> service.allocate(request))
                .isInstanceOf(InsufficientInventoryException.class)
                .hasMessageContaining("Only 1 CSR unit is available")
                .hasMessageContaining("requested 2");

        verify(orderRepository, never()).insertCsrAllocated(
                request.sku(), request.facility(), request.quantity()
        );
    }

    @Test
    void rejectsUnknownSkuBeforeCreatingOrder() {
        CreateOrderRequest request = new CreateOrderRequest("Missing", "LA-DC", 1);
        when(inventoryRepository.reserveForCsr(request.sku(), request.facility(), 1)).thenReturn(0);
        when(inventoryRepository.skuExists(request.sku())).thenReturn(false);

        assertThatThrownBy(() -> service.allocate(request))
                .isInstanceOfSatisfying(NotFoundException.class,
                        exception -> assertThat(exception.code()).isEqualTo("SKU_NOT_FOUND"));

        verifyNoInteractions(orderRepository);
    }
}
