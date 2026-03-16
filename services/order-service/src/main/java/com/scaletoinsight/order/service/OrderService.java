package com.scaletoinsight.order.service;

import com.scaletoinsight.order.event.OrderCreatedEvent;
import com.scaletoinsight.order.model.Order;
import com.scaletoinsight.order.model.OrderItem;
import com.scaletoinsight.order.repository.OrderRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private static final String TOPIC_ORDER_CREATED = "order.created";

    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public OrderService(OrderRepository orderRepository,
                        KafkaTemplate<String, Object> kafkaTemplate) {
        this.orderRepository = orderRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    public List<Order> findAll() {
        return orderRepository.findAll();
    }

    public Optional<Order> findById(Long id) {
        return orderRepository.findById(id);
    }

    public Optional<Order> findByOrderCode(String orderCode) {
        return orderRepository.findByOrderCode(orderCode);
    }

    public List<Order> findByCustomer(String customerId) {
        return orderRepository.findByCustomerId(customerId);
    }

    @Transactional
    public Order createOrder(Order order) {
        if (order.getOrderCode() == null || order.getOrderCode().isBlank()) {
            order.setOrderCode("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }

        // Recalculate total from items
        BigDecimal total = order.getItems().stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalAmount(total);

        Order saved = orderRepository.save(order);

        // Publish event to Kafka for downstream processing
        publishOrderCreatedEvent(saved);

        log.info("Order created: {} for customer {}", saved.getOrderCode(), saved.getCustomerId());
        return saved;
    }

    @Transactional
    public Optional<Order> updateStatus(Long id, String newStatus) {
        return orderRepository.findById(id).map(order -> {
            order.setStatus(newStatus);
            Order updated = orderRepository.save(order);
            log.info("Order {} status updated to {}", updated.getOrderCode(), newStatus);
            return updated;
        });
    }

    private void publishOrderCreatedEvent(Order order) {
        try {
            var event = new OrderCreatedEvent(
                    order.getId(),
                    order.getOrderCode(),
                    order.getCustomerId(),
                    order.getChannel(),
                    order.getTotalAmount(),
                    order.getDiscountAmount(),
                    order.getCreatedAt()
            );
            kafkaTemplate.send(TOPIC_ORDER_CREATED, order.getOrderCode(), event);
        } catch (Exception e) {
            log.warn("Failed to publish order event for {}: {}", order.getOrderCode(), e.getMessage());
        }
    }
}
