package org.example.backend.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.example.backend.dto.CreateOrderRequest;
import org.example.backend.dto.OrderResponse;
import org.example.backend.error.InsufficientInventoryException;
import org.example.backend.service.OrderAllocationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderAllocationIntegrationTests {

    private static final String WHITE = "TShirt-White";
    private static final String BLACK = "TShirt-Black";
    private static final String LA = "LA-DC";
    private static final String NJ = "NJ-DC";

    @Autowired
    private OrderAllocationService allocationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private MockMvc mockMvc;

    private TestInventoryFixture fixture;
    private ExecutorService executor;

    @BeforeEach
    void restoreDemoSeed() throws Exception {
        fixture = new TestInventoryFixture(jdbcTemplate, dataSource);
        fixture.restoreDemoSeed();
    }

    @AfterEach
    void stopExecutor() throws InterruptedException {
        if (executor != null) {
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void demoSeedMatchesTheDocumentedInventoryAndOrders() {
        assertThat(fixture.inventory(WHITE, LA))
                .isEqualTo(new TestInventoryFixture.InventorySnapshot(10, 7, 1, 1, 1));
        assertThat(fixture.inventory(WHITE, NJ))
                .isEqualTo(new TestInventoryFixture.InventorySnapshot(3, 3, 0, 0, 0));
        assertThat(fixture.inventory(BLACK, LA))
                .isEqualTo(new TestInventoryFixture.InventorySnapshot(0, 0, 0, 0, 0));
        assertThat(fixture.inventory(BLACK, NJ))
                .isEqualTo(new TestInventoryFixture.InventorySnapshot(9, 3, 2, 2, 2));
        assertThat(fixture.countOrders("HISTORICAL")).isEqualTo(13);
        assertThat(fixture.countHistoricalOrders(WHITE)).isEqualTo(10);
        assertThat(fixture.countHistoricalOrders(BLACK)).isEqualTo(3);
    }

    @Test
    void validAllocationCommitsInventoryAndOrderTogether() {
        OrderResponse response = allocationService.allocate(
                new CreateOrderRequest(WHITE, LA, 1)
        );

        assertThat(response.channel()).isEqualTo("CSR");
        assertThat(response.status()).isEqualTo("ALLOCATED");
        assertThat(fixture.inventory(WHITE, LA))
                .isEqualTo(new TestInventoryFixture.InventorySnapshot(10, 8, 1, 1, 0));
        assertThat(fixture.countOrders("CSR")).isEqualTo(1);
        assertThat(fixture.sumOrderQuantity("CSR", WHITE, LA)).isEqualTo(1);
    }

    @Test
    void insufficientAllocationDoesNotChangeInventoryOrCreateOrder() {
        assertThatThrownBy(() -> allocationService.allocate(
                new CreateOrderRequest(WHITE, LA, 2)
        )).isInstanceOf(InsufficientInventoryException.class);

        assertThat(fixture.inventory(WHITE, LA))
                .isEqualTo(new TestInventoryFixture.InventorySnapshot(10, 7, 1, 1, 1));
        assertThat(fixture.countOrders("CSR")).isZero();
    }

    @Test
    void zeroStockAllocationDoesNotChangeInventoryOrCreateOrder() {
        assertThatThrownBy(() -> allocationService.allocate(
                new CreateOrderRequest(BLACK, LA, 1)
        )).isInstanceOf(InsufficientInventoryException.class);

        assertThat(fixture.inventory(BLACK, LA))
                .isEqualTo(new TestInventoryFixture.InventorySnapshot(0, 0, 0, 0, 0));
        assertThat(fixture.countOrders("CSR")).isZero();
    }

    @Test
    void invalidPayloadsReturnBadRequestWithoutChangingTheDatabase() throws Exception {
        List<String> invalidBodies = List.of(
                "{\"sku\":\"TShirt-White\",\"facility\":\"LA-DC\",\"quantity\":0}",
                "{\"sku\":\"TShirt-White\",\"facility\":\"LA-DC\"}",
                "{\"sku\":\"\",\"facility\":\"LA-DC\",\"quantity\":1}"
        );

        for (String body : invalidBodies) {
            mockMvc.perform(post("/api/orders")
                            .contentType("application/json")
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        assertThat(fixture.inventory(WHITE, LA))
                .isEqualTo(new TestInventoryFixture.InventorySnapshot(10, 7, 1, 1, 1));
        assertThat(fixture.countOrders("CSR")).isZero();
    }

    @Test
    void twentyCallersCannotReserveMoreThanTenUnits() throws Exception {
        fixture.prepareTenUnitRaceFixture();
        executor = Executors.newFixedThreadPool(20);
        CountDownLatch ready = new CountDownLatch(20);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();

        for (int index = 0; index < 20; index++) {
            results.add(executor.submit(() -> {
                ready.countDown();
                start.await();
                try {
                    allocationService.allocate(new CreateOrderRequest(WHITE, LA, 1));
                    return true;
                } catch (InsufficientInventoryException expected) {
                    return false;
                }
            }));
        }

        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();

        int successes = 0;
        for (Future<Boolean> result : results) {
            if (result.get(20, TimeUnit.SECONDS)) {
                successes++;
            }
        }

        assertThat(successes).isEqualTo(10);
        assertThat(fixture.inventory(WHITE, LA))
                .isEqualTo(new TestInventoryFixture.InventorySnapshot(10, 10, 0, 0, 0));
        assertThat(fixture.countOrders("CSR")).isEqualTo(10);
        assertThat(fixture.sumOrderQuantity("CSR", WHITE, LA)).isEqualTo(10);
    }

    @Test
    void twoLargeConcurrentRequestsCannotOverAllocate() throws Exception {
        fixture.prepareTenUnitRaceFixture();
        executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Future<Integer> sevenUnits = submitAllocation(7, ready, start);
        Future<Integer> sixUnits = submitAllocation(6, ready, start);

        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();

        List<Integer> results = List.of(
                sevenUnits.get(20, TimeUnit.SECONDS),
                sixUnits.get(20, TimeUnit.SECONDS)
        );
        int reserved = results.stream().mapToInt(Integer::intValue).sum();

        assertThat(results).contains(0);
        assertThat(results.stream().filter(quantity -> quantity > 0).count()).isEqualTo(1);
        assertThat(reserved).isIn(6, 7);
        assertThat(fixture.inventory(WHITE, LA).reserved()).isEqualTo(reserved);
        assertThat(fixture.inventory(WHITE, LA).csrAvailable()).isEqualTo(10 - reserved);
        assertThat(fixture.countOrders("CSR")).isEqualTo(1);
        assertThat(fixture.sumOrderQuantity("CSR", WHITE, LA)).isEqualTo(reserved);
    }

    private Future<Integer> submitAllocation(
            int quantity,
            CountDownLatch ready,
            CountDownLatch start
    ) {
        return executor.submit(() -> {
            ready.countDown();
            start.await();
            try {
                allocationService.allocate(new CreateOrderRequest(WHITE, LA, quantity));
                return quantity;
            } catch (InsufficientInventoryException expected) {
                return 0;
            }
        });
    }
}
