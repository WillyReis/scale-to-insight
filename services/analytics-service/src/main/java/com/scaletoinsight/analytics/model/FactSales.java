package com.scaletoinsight.analytics.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "fact_sales")
public class FactSales {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sales_key")
    private Long salesKey;

    @Column(name = "order_id", nullable = false)
    private String orderId;

    @Column(name = "date_key", nullable = false)
    private Integer dateKey;

    @Column(name = "product_key", nullable = false)
    private Integer productKey;

    @Column(name = "customer_key", nullable = false)
    private Integer customerKey;

    @Column(name = "channel_key", nullable = false)
    private Integer channelKey;

    @Column(name = "quantity", nullable = false)
    private Integer quantity = 1;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "discount_amount", nullable = false)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "cost_of_goods")
    private BigDecimal costOfGoods;

    @Column(name = "loaded_at", nullable = false)
    private LocalDateTime loadedAt = LocalDateTime.now();

    public FactSales() {}

    // Getters & Setters
    public Long getSalesKey() { return salesKey; }
    public void setSalesKey(Long salesKey) { this.salesKey = salesKey; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public Integer getDateKey() { return dateKey; }
    public void setDateKey(Integer dateKey) { this.dateKey = dateKey; }

    public Integer getProductKey() { return productKey; }
    public void setProductKey(Integer productKey) { this.productKey = productKey; }

    public Integer getCustomerKey() { return customerKey; }
    public void setCustomerKey(Integer customerKey) { this.customerKey = customerKey; }

    public Integer getChannelKey() { return channelKey; }
    public void setChannelKey(Integer channelKey) { this.channelKey = channelKey; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }

    public BigDecimal getCostOfGoods() { return costOfGoods; }
    public void setCostOfGoods(BigDecimal costOfGoods) { this.costOfGoods = costOfGoods; }

    public LocalDateTime getLoadedAt() { return loadedAt; }
    public void setLoadedAt(LocalDateTime loadedAt) { this.loadedAt = loadedAt; }
}
