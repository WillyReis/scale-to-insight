package com.scaletoinsight.analytics.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.scaletoinsight.analytics.model.DimDate;
import com.scaletoinsight.analytics.model.FactSales;
import com.scaletoinsight.analytics.repository.DimDateRepository;
import com.scaletoinsight.analytics.repository.FactSalesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * Consumes Kafka events from the order-service and loads them into the
 * fact_sales table (ETL – Extract, Transform, Load).
 */
@Service
public class SalesEtlService {

    private static final Logger log = LoggerFactory.getLogger(SalesEtlService.class);

    private final FactSalesRepository factSalesRepository;
    private final DimDateRepository dimDateRepository;
    private final DataLakeService dataLakeService;

    public SalesEtlService(FactSalesRepository factSalesRepository,
                            DimDateRepository dimDateRepository,
                            DataLakeService dataLakeService) {
        this.factSalesRepository = factSalesRepository;
        this.dimDateRepository = dimDateRepository;
        this.dataLakeService = dataLakeService;
    }

    @KafkaListener(topics = "order.created", groupId = "analytics-service-group")
    @Transactional
    public void onOrderCreated(JsonNode payload) {
        try {
            log.info("Received order.created event: {}", payload);

            String orderId   = payload.path("orderCode").asText();
            String channel   = payload.path("channel").asText("WEB");
            BigDecimal total = new BigDecimal(payload.path("totalAmount").asText("0"));
            BigDecimal disc  = new BigDecimal(payload.path("discountAmount").asText("0"));

            LocalDate eventDate = LocalDate.now();
            DimDate dimDate = findOrCreateDimDate(eventDate);

            // Map channel to channel_key (seeded defaults: WEB=1, MOBILE=2, MARKETPLACE=3, STORE=4)
            int channelKey = mapChannelKey(channel);

            FactSales fact = new FactSales();
            fact.setOrderId(orderId);
            fact.setDateKey(dimDate.getDateKey());
            fact.setProductKey(1);    // placeholder – enrich with real product dimension
            fact.setCustomerKey(1);   // placeholder – enrich with real customer dimension
            fact.setChannelKey(channelKey);
            fact.setQuantity(1);
            fact.setUnitPrice(total);
            fact.setDiscountAmount(disc);
            fact.setLoadedAt(LocalDateTime.now());

            factSalesRepository.save(fact);

            // Archive raw event to the Data Lake (S3)
            dataLakeService.archiveRawEvent("raw/sales-events/", orderId, payload.toString());

            log.info("Loaded order {} into fact_sales", orderId);
        } catch (Exception e) {
            log.error("Error processing order.created event: {}", e.getMessage(), e);
        }
    }

    private DimDate findOrCreateDimDate(LocalDate date) {
        return dimDateRepository.findByFullDate(date).orElseGet(() -> {
            DimDate d = new DimDate();
            d.setFullDate(date);
            d.setDayOfWeek((short) date.getDayOfWeek().getValue());
            d.setDayName(date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH));
            d.setMonthNumber((short) date.getMonthValue());
            d.setMonthName(date.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH));
            d.setQuarter((short) ((date.getMonthValue() - 1) / 3 + 1));
            d.setYear((short) date.getYear());
            DayOfWeek dow = date.getDayOfWeek();
            d.setWeekend(dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY);
            return dimDateRepository.save(d);
        });
    }

    private int mapChannelKey(String channel) {
        return switch (channel.toUpperCase()) {
            case "MOBILE"      -> 2;
            case "MARKETPLACE" -> 3;
            case "STORE"       -> 4;
            default            -> 1; // WEB
        };
    }
}
