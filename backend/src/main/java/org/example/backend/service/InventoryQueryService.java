package org.example.backend.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.example.backend.dto.InventoryResponse;
import org.example.backend.model.InventoryRow;
import org.example.backend.repository.InventoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryQueryService {

    private final InventoryRepository inventoryRepository;

    public InventoryQueryService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @Transactional(readOnly = true)
    public InventoryResponse getInventory() {
        List<InventoryRow> rows = inventoryRepository.findAll();

        List<InventoryResponse.Item> items = rows.stream()
                .map(InventoryQueryService::toItem)
                .toList();

        Map<String, MutableTotal> totalsBySku = new LinkedHashMap<>();
        for (InventoryRow row : rows) {
            totalsBySku.computeIfAbsent(row.sku(), ignored -> new MutableTotal())
                    .add(row);
        }

        List<InventoryResponse.Total> totals = totalsBySku.entrySet().stream()
                .map(entry -> entry.getValue().toResponse(entry.getKey()))
                .toList();

        return new InventoryResponse(items, totals);
    }

    private static InventoryResponse.Item toItem(InventoryRow row) {
        return new InventoryResponse.Item(
                row.sku(),
                row.facility(),
                row.onHandQty(),
                row.reservedQty(),
                row.amazonHoldQty(),
                row.walmartHoldQty(),
                row.csrAvailableQty()
        );
    }

    private static final class MutableTotal {
        private int onHandQty;
        private int reservedQty;
        private int amazonHoldQty;
        private int walmartHoldQty;
        private int csrAvailableQty;

        private MutableTotal add(InventoryRow row) {
            onHandQty += row.onHandQty();
            reservedQty += row.reservedQty();
            amazonHoldQty += row.amazonHoldQty();
            walmartHoldQty += row.walmartHoldQty();
            csrAvailableQty += row.csrAvailableQty();
            return this;
        }

        private InventoryResponse.Total toResponse(String sku) {
            return new InventoryResponse.Total(
                    sku,
                    onHandQty,
                    reservedQty,
                    amazonHoldQty,
                    walmartHoldQty,
                    csrAvailableQty
            );
        }
    }
}
