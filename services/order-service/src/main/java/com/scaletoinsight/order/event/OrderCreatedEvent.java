package com.scaletoinsight.order.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderCreatedEvent(
        Long orderId,
        String orderCode,
        String customerId,
        String channel,
        BigDecimal totalAmount,
        BigDecimal discountAmount,
        LocalDateTime createdAt
) {}
