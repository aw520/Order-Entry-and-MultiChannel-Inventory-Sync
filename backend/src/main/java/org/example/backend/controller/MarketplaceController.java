package org.example.backend.controller;

import org.example.backend.dto.PublicationResponse;
import org.example.backend.publisher.MarketplacePublisher;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/marketplaces")
public class MarketplaceController {

    private final MarketplacePublisher marketplacePublisher;

    public MarketplaceController(MarketplacePublisher marketplacePublisher) {
        this.marketplacePublisher = marketplacePublisher;
    }

    @PostMapping("/publish")
    public PublicationResponse publish() {
        return marketplacePublisher.publishCurrentInventory();
    }
}
