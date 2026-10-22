CREATE TABLE sku (
                     sku  VARCHAR(64) NOT NULL PRIMARY KEY,
                     name VARCHAR(128) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE facility (
                          code VARCHAR(16) NOT NULL PRIMARY KEY,
                          name VARCHAR(128) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE inventory (
                           sku               VARCHAR(64) NOT NULL,
                           facility          VARCHAR(16) NOT NULL,
                           on_hand_qty       INT NOT NULL,
                           reserved_qty      INT NOT NULL DEFAULT 0,
                           amazon_hold_qty   INT NOT NULL DEFAULT 0,
                           walmart_hold_qty  INT NOT NULL DEFAULT 0,
                           csr_available_qty INT GENERATED ALWAYS AS (
                               on_hand_qty - reserved_qty - amazon_hold_qty - walmart_hold_qty
                               ) STORED,
                           updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                               ON UPDATE CURRENT_TIMESTAMP(3),
                           PRIMARY KEY (sku, facility),
                           CONSTRAINT fk_inventory_sku FOREIGN KEY (sku) REFERENCES sku(sku),
                           CONSTRAINT fk_inventory_facility FOREIGN KEY (facility)
                               REFERENCES facility(code),
                           CONSTRAINT chk_inventory_nonnegative CHECK (
                               on_hand_qty >= 0 AND reserved_qty >= 0
                                   AND amazon_hold_qty >= 0 AND walmart_hold_qty >= 0
                               ),
                           CONSTRAINT chk_inventory_commitments CHECK (
                               reserved_qty + amazon_hold_qty + walmart_hold_qty <= on_hand_qty
                               )
) ENGINE=InnoDB;

CREATE TABLE customer_order (
                                id                BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                                sku               VARCHAR(64) NOT NULL,
                                facility          VARCHAR(16) NOT NULL,
                                channel           VARCHAR(16) NOT NULL,
                                external_order_id VARCHAR(128) NULL,
                                quantity          INT NOT NULL,
                                status            VARCHAR(16) NOT NULL,
                                is_demo           BOOLEAN NOT NULL DEFAULT FALSE,
                                created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
                                updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                                    ON UPDATE CURRENT_TIMESTAMP(3),
                                CONSTRAINT fk_order_inventory FOREIGN KEY (sku, facility)
                                    REFERENCES inventory(sku, facility),
                                CONSTRAINT uq_order_external UNIQUE (channel, external_order_id),
                                CONSTRAINT chk_order_channel CHECK (
                                    channel IN ('CSR', 'AMAZON', 'WALMART', 'HISTORICAL')
                                    ),
                                CONSTRAINT chk_order_quantity CHECK (quantity > 0),
                                CONSTRAINT chk_order_status CHECK (
                                    status IN ('ALLOCATED', 'PICKING', 'PACKED', 'FULFILLED', 'CANCELLED')
                                    ),
                                CONSTRAINT chk_external_id CHECK (
                                    channel IN ('CSR', 'HISTORICAL') OR
                                    (external_order_id IS NOT NULL AND CHAR_LENGTH(external_order_id) > 0)
                                    ),
                                KEY idx_order_inventory_status (sku, facility, status),
                                KEY idx_order_created (created_at, id)
) ENGINE=InnoDB;