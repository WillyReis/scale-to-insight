package com.scaletoinsight.product.service;

import com.scaletoinsight.product.model.Product;
import com.scaletoinsight.product.repository.ProductRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);
    private static final String TOPIC_PRODUCT_CREATED = "product.created";

    private final ProductRepository productRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public ProductService(ProductRepository productRepository,
                          KafkaTemplate<String, Object> kafkaTemplate) {
        this.productRepository = productRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    public List<Product> findAll() {
        return productRepository.findAll();
    }

    public Optional<Product> findById(Long id) {
        return productRepository.findById(id);
    }

    public List<Product> findByCategory(String category) {
        return productRepository.findByCategory(category);
    }

    @Transactional
    public Product createProduct(Product product) {
        if (product.getProductCode() == null || product.getProductCode().isBlank()) {
            product.setProductCode("PROD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }
        Product saved = productRepository.save(product);

        try {
            kafkaTemplate.send(TOPIC_PRODUCT_CREATED, saved.getProductCode(), saved);
        } catch (Exception e) {
            log.warn("Failed to publish product event for {}: {}", saved.getProductCode(), e.getMessage());
        }

        log.info("Product created: {} – {}", saved.getProductCode(), saved.getName());
        return saved;
    }

    @Transactional
    public Optional<Product> updateStock(Long id, int delta) {
        return productRepository.findById(id).map(product -> {
            int newStock = product.getStockQuantity() + delta;
            product.setStockQuantity(Math.max(0, newStock));
            Product updated = productRepository.save(product);
            log.info("Product {} stock updated to {}", updated.getProductCode(), updated.getStockQuantity());
            return updated;
        });
    }
}
