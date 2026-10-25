package org.example.backend.publisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.example.backend.dto.PublicationResponse;
import org.example.backend.model.InventoryRow;
import org.example.backend.repository.InventoryRepository;
import org.junit.jupiter.api.Test;

class MarketplacePublisherTests {

    @Test
    void publishesEachMarketplaceHoldWithoutChangingInventory() {
        InventoryRepository repository = mock(InventoryRepository.class);
        when(repository.findAll()).thenReturn(List.of(
                new InventoryRow("TShirt-White", "LA-DC", 10, 7, 1, 1, 1),
                new InventoryRow("TShirt-White", "NJ-DC", 3, 3, 0, 0, 0),
                new InventoryRow("TShirt-Black", "LA-DC", 0, 0, 0, 0, 0),
                new InventoryRow("TShirt-Black", "NJ-DC", 9, 3, 2, 2, 2)
        ));

        PublicationResponse response = new MarketplacePublisher(repository)
                .publishCurrentInventory();

        assertThat(response.mode()).isEqualTo("LOG_ONLY");
        assertThat(response.publications()).containsExactly(
                new PublicationResponse.Item("AMAZON", "TShirt-White", "LA-DC", 1),
                new PublicationResponse.Item("WALMART", "TShirt-White", "LA-DC", 1),
                new PublicationResponse.Item("AMAZON", "TShirt-White", "NJ-DC", 0),
                new PublicationResponse.Item("WALMART", "TShirt-White", "NJ-DC", 0),
                new PublicationResponse.Item("AMAZON", "TShirt-Black", "LA-DC", 0),
                new PublicationResponse.Item("WALMART", "TShirt-Black", "LA-DC", 0),
                new PublicationResponse.Item("AMAZON", "TShirt-Black", "NJ-DC", 2),
                new PublicationResponse.Item("WALMART", "TShirt-Black", "NJ-DC", 2)
        );
        verify(repository).findAll();
        verifyNoMoreInteractions(repository);
    }
}
