package com.scaletoinsight.analytics.controller;

import com.scaletoinsight.analytics.model.FactSales;
import com.scaletoinsight.analytics.service.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "analytics-service"));
    }

    @GetMapping("/sales")
    public ResponseEntity<List<FactSales>> getSales() {
        return ResponseEntity.ok(analyticsService.getAllSales());
    }

    @GetMapping("/sales/summary")
    public ResponseEntity<Map<String, Object>> getSalesSummary() {
        return ResponseEntity.ok(analyticsService.getSalesSummary());
    }
}
