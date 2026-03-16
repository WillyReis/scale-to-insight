package com.scaletoinsight.analytics;

import com.scaletoinsight.analytics.model.FactSales;
import com.scaletoinsight.analytics.repository.FactSalesRepository;
import com.scaletoinsight.analytics.service.AnalyticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class AnalyticsServiceTest {

    private FactSalesRepository factSalesRepository;
    private AnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        factSalesRepository = Mockito.mock(FactSalesRepository.class);
        analyticsService = new AnalyticsService(factSalesRepository);
    }

    @Test
    void getSalesSummary_shouldAggregateTotals() {
        FactSales sale1 = buildSale(BigDecimal.valueOf(100.00), BigDecimal.valueOf(10.00), 2);
        FactSales sale2 = buildSale(BigDecimal.valueOf(50.00), BigDecimal.ZERO, 1);
        when(factSalesRepository.findAll()).thenReturn(List.of(sale1, sale2));

        Map<String, Object> summary = analyticsService.getSalesSummary();

        assertThat(summary.get("totalOrders")).isEqualTo(2);
        // sale1: 100 * 2 = 200, sale2: 50 * 1 = 50 => total = 250
        assertThat((BigDecimal) summary.get("totalRevenue"))
                .isEqualByComparingTo(BigDecimal.valueOf(250.00));
        // discount: 10 + 0 = 10
        assertThat((BigDecimal) summary.get("totalDiscount"))
                .isEqualByComparingTo(BigDecimal.valueOf(10.00));
        // net = 250 - 10 = 240
        assertThat((BigDecimal) summary.get("netRevenue"))
                .isEqualByComparingTo(BigDecimal.valueOf(240.00));
    }

    @Test
    void getSalesSummary_shouldReturnZerosWhenNoSales() {
        when(factSalesRepository.findAll()).thenReturn(List.of());
        Map<String, Object> summary = analyticsService.getSalesSummary();
        assertThat(summary.get("totalOrders")).isEqualTo(0);
        assertThat((BigDecimal) summary.get("totalRevenue")).isEqualByComparingTo(BigDecimal.ZERO);
    }

    private FactSales buildSale(BigDecimal unitPrice, BigDecimal discount, int qty) {
        FactSales fs = new FactSales();
        fs.setUnitPrice(unitPrice);
        fs.setDiscountAmount(discount);
        fs.setQuantity(qty);
        fs.setOrderId("ORD-TEST");
        fs.setDateKey(1);
        fs.setProductKey(1);
        fs.setCustomerKey(1);
        fs.setChannelKey(1);
        return fs;
    }
}
