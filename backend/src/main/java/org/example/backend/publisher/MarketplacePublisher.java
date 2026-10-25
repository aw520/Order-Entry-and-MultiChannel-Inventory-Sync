package org.example.backend.publisher;

import java.util.ArrayList;
import java.util.List;

import org.example.backend.dto.PublicationResponse;
import org.example.backend.model.InventoryRow;
import org.example.backend.repository.InventoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarketplacePublisher {

    private static final Logger log = LoggerFactory.getLogger(MarketplacePublisher.class);

    private final InventoryRepository inventoryRepository;

    public MarketplacePublisher(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @Transactional(readOnly = true)
    public PublicationResponse publishCurrentInventory() {
        List<InventoryRow> inventory = inventoryRepository.findAll();
        List<PublicationResponse.Item> publications = new ArrayList<>();

        for (InventoryRow row : inventory) {
            publications.add(publish("AMAZON", row, row.amazonHoldQty()));
            publications.add(publish("WALMART", row, row.walmartHoldQty()));
        }

        return new PublicationResponse("LOG_ONLY", publications);
    }

    private PublicationResponse.Item publish(
            String channel,
            InventoryRow row,
            int quantity
    ) {
        log.info(
                "Simulated inventory publish: channel={}, sku={}, facility={}, quantity={}",
                channel,
                row.sku(),
                row.facility(),
                quantity
        );
        return new PublicationResponse.Item(
                channel,
                row.sku(),
                row.facility(),
                quantity
        );
    }
}
