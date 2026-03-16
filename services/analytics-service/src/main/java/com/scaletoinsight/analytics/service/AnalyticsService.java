package com.scaletoinsight.analytics.service;

import com.scaletoinsight.analytics.model.FactSales;
import com.scaletoinsight.analytics.repository.FactSalesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class AnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);

    private final FactSalesRepository factSalesRepository;

    public AnalyticsService(FactSalesRepository factSalesRepository) {
        this.factSalesRepository = factSalesRepository;
    }

    public List<FactSales> getAllSales() {
        return factSalesRepository.findAll();
    }

    public Map<String, Object> getSalesSummary() {
        List<FactSales> allSales = factSalesRepository.findAll();

        BigDecimal totalRevenue = allSales.stream()
                .map(fs -> fs.getUnitPrice().multiply(BigDecimal.valueOf(fs.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalDiscount = allSales.stream()
                .map(FactSales::getDiscountAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return Map.of(
                "totalOrders",   allSales.size(),
                "totalRevenue",  totalRevenue,
                "totalDiscount", totalDiscount,
                "netRevenue",    totalRevenue.subtract(totalDiscount),
                "asOf",          LocalDate.now().toString()
        );
    }
}
