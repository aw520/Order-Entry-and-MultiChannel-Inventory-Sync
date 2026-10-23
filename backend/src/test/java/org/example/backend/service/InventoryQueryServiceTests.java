package org.example.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.example.backend.dto.InventoryResponse;
import org.example.backend.model.InventoryRow;
import org.example.backend.repository.InventoryRepository;
import org.junit.jupiter.api.Test;

class InventoryQueryServiceTests {

    @Test
    void returnsRowsAndComputesTotalsFromTheSameRepositoryResult() {
        InventoryRepository repository = mock(InventoryRepository.class);
        List<InventoryRow> rows = List.of(
                new InventoryRow("TShirt-Black", "LA-DC", 0, 0, 0, 0, 0),
                new InventoryRow("TShirt-Black", "NJ-DC", 9, 3, 2, 2, 2),
                new InventoryRow("TShirt-White", "LA-DC", 10, 7, 1, 1, 1),
                new InventoryRow("TShirt-White", "NJ-DC", 3, 3, 0, 0, 0)
        );
        when(repository.findAll()).thenReturn(rows);

        InventoryResponse response = new InventoryQueryService(repository).getInventory();

        assertThat(response.items()).hasSize(4);
        assertThat(response.totals()).containsExactly(
                new InventoryResponse.Total("TShirt-Black", 9, 3, 2, 2, 2),
                new InventoryResponse.Total("TShirt-White", 13, 10, 1, 1, 1)
        );
        verify(repository).findAll();
    }
}
