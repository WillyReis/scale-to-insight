package com.scaletoinsight.order;

import com.scaletoinsight.order.model.Order;
import com.scaletoinsight.order.model.OrderItem;
import com.scaletoinsight.order.repository.OrderRepository;
import com.scaletoinsight.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrderServiceTest {

    private OrderRepository orderRepository;
    private KafkaTemplate<String, Object> kafkaTemplate;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderRepository = Mockito.mock(OrderRepository.class);
        kafkaTemplate = Mockito.mock(KafkaTemplate.class);
        orderService = new OrderService(orderRepository, kafkaTemplate);
    }

    @Test
    void createOrder_shouldGenerateOrderCodeWhenNotProvided() {
        Order order = buildSampleOrder(null);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(1L);
            return o;
        });

        Order created = orderService.createOrder(order);

        assertThat(created.getOrderCode()).startsWith("ORD-");
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void createOrder_shouldCalculateTotalFromItems() {
        Order order = buildSampleOrder("ORD-TEST-001");
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(1L);
            return o;
        });

        Order created = orderService.createOrder(order);

        // 2 items x 50.00 = 100.00
        assertThat(created.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(100.00));
    }

    @Test
    void findAll_shouldDelegateToRepository() {
        when(orderRepository.findAll()).thenReturn(List.of(new Order()));
        List<Order> result = orderService.findAll();
        assertThat(result).hasSize(1);
    }

    private Order buildSampleOrder(String orderCode) {
        Order order = new Order();
        order.setOrderCode(orderCode);
        order.setCustomerId("CUST-001");
        order.setChannel("WEB");

        OrderItem item = new OrderItem();
        item.setProductId("PROD-001");
        item.setProductName("Test Product");
        item.setQuantity(2);
        item.setUnitPrice(BigDecimal.valueOf(50.00));

        order.addItem(item);
        return order;
    }
}
