package org.example.backend.integration;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;

final class TestInventoryFixture {

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    TestInventoryFixture(JdbcTemplate jdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
    }

    void restoreDemoSeed() throws Exception {
        requireTestDatabase();
        jdbcTemplate.update("DELETE FROM customer_order");

        setInventory("TShirt-White", "LA-DC", 10, 7, 1, 1);
        setInventory("TShirt-White", "NJ-DC", 3, 3, 0, 0);
        setInventory("TShirt-Black", "LA-DC", 0, 0, 0, 0);
        setInventory("TShirt-Black", "NJ-DC", 9, 3, 2, 2);

        insertHistoricalOrders("TShirt-White", "LA-DC", 7);
        insertHistoricalOrders("TShirt-White", "NJ-DC", 3);
        insertHistoricalOrders("TShirt-Black", "NJ-DC", 3);
    }

    void prepareTenUnitRaceFixture() throws Exception {
        requireTestDatabase();
        jdbcTemplate.update("DELETE FROM customer_order");
        setInventory("TShirt-White", "LA-DC", 10, 0, 0, 0);
    }

    InventorySnapshot inventory(String sku, String facility) {
        return jdbcTemplate.queryForObject("""
                SELECT on_hand_qty, reserved_qty, amazon_hold_qty,
                       walmart_hold_qty, csr_available_qty
                FROM inventory
                WHERE sku = ? AND facility = ?
                """, (resultSet, rowNumber) -> new InventorySnapshot(
                resultSet.getInt("on_hand_qty"),
                resultSet.getInt("reserved_qty"),
                resultSet.getInt("amazon_hold_qty"),
                resultSet.getInt("walmart_hold_qty"),
                resultSet.getInt("csr_available_qty")
        ), sku, facility);
    }

    int countOrders(String channel) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM customer_order WHERE channel = ?",
                Integer.class,
                channel
        );
    }

    int countHistoricalOrders(String sku) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM customer_order
                WHERE channel = 'HISTORICAL' AND sku = ?
                """, Integer.class, sku);
    }

    int sumOrderQuantity(String channel, String sku, String facility) {
        return jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(quantity), 0)
                FROM customer_order
                WHERE channel = ? AND sku = ? AND facility = ?
                """, Integer.class, channel, sku, facility);
    }

    private void requireTestDatabase() throws Exception {
        try (var connection = dataSource.getConnection()) {
            String url = connection.getMetaData().getURL();
            if (url == null || !url.contains("/oms_takehome_test")) {
                throw new IllegalStateException(
                        "Refusing to reset a database other than oms_takehome_test: " + url
                );
            }
        }
    }

    private void setInventory(
            String sku,
            String facility,
            int onHand,
            int reserved,
            int amazonHold,
            int walmartHold
    ) {
        jdbcTemplate.update("""
                UPDATE inventory
                SET on_hand_qty = ?, reserved_qty = ?,
                    amazon_hold_qty = ?, walmart_hold_qty = ?
                WHERE sku = ? AND facility = ?
                """, onHand, reserved, amazonHold, walmartHold, sku, facility);
    }

    private void insertHistoricalOrders(
            String sku,
            String facility,
            int count
    ) {
        for (int index = 0; index < count; index++) {
            jdbcTemplate.update("""
                    INSERT INTO customer_order
                        (sku, facility, channel, quantity, status, is_demo)
                    VALUES (?, ?, 'HISTORICAL', 1, 'ALLOCATED', TRUE)
                    """, sku, facility);
        }
    }

    record InventorySnapshot(
            int onHand,
            int reserved,
            int amazonHold,
            int walmartHold,
            int csrAvailable
    ) {
    }
}
