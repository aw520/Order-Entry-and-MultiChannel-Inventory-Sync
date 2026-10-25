package org.example.backend.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import javax.sql.DataSource;

import org.example.backend.dto.CreateOrderRequest;
import org.example.backend.repository.OrderRepository;
import org.example.backend.service.OrderAllocationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class OrderAllocationRollbackIntegrationTests {

    @Autowired
    private OrderAllocationService allocationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    @MockitoBean
    private OrderRepository orderRepository;

    private TestInventoryFixture fixture;

    @BeforeEach
    void restoreDemoSeed() throws Exception {
        fixture = new TestInventoryFixture(jdbcTemplate, dataSource);
        fixture.restoreDemoSeed();
    }

    @Test
    void orderInsertFailureRollsBackTheInventoryReservation() {
        when(orderRepository.insertCsrAllocated("TShirt-White", "LA-DC", 1))
                .thenThrow(new IllegalStateException("Controlled order insert failure"));

        assertThatThrownBy(() -> allocationService.allocate(
                new CreateOrderRequest("TShirt-White", "LA-DC", 1)
        )).isInstanceOf(IllegalStateException.class)
                .hasMessage("Controlled order insert failure");

        assertThat(fixture.inventory("TShirt-White", "LA-DC"))
                .isEqualTo(new TestInventoryFixture.InventorySnapshot(10, 7, 1, 1, 1));
        assertThat(fixture.countOrders("HISTORICAL")).isEqualTo(13);
        assertThat(fixture.countOrders("CSR")).isZero();
    }
}
