package com.scaletoinsight.product;

import com.scaletoinsight.product.model.Product;
import com.scaletoinsight.product.repository.ProductRepository;
import com.scaletoinsight.product.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProductServiceTest {

    private ProductRepository productRepository;
    private KafkaTemplate<String, Object> kafkaTemplate;
    private ProductService productService;

    @BeforeEach
    void setUp() {
        productRepository = Mockito.mock(ProductRepository.class);
        kafkaTemplate = Mockito.mock(KafkaTemplate.class);
        productService = new ProductService(productRepository, kafkaTemplate);
    }

    @Test
    void createProduct_shouldGenerateProductCodeWhenNotProvided() {
        Product product = buildSampleProduct(null);
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        Product created = productService.createProduct(product);

        assertThat(created.getProductCode()).startsWith("PROD-");
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    void updateStock_shouldNotGoBelowZero() {
        Product product = buildSampleProduct("PROD-001");
        product.setId(1L);
        product.setStockQuantity(5);

        when(productRepository.findById(1L)).thenReturn(java.util.Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        // Subtract more than available stock
        productService.updateStock(1L, -10);

        assertThat(product.getStockQuantity()).isZero();
    }

    @Test
    void findAll_shouldDelegateToRepository() {
        when(productRepository.findAll()).thenReturn(List.of(new Product()));
        List<Product> result = productService.findAll();
        assertThat(result).hasSize(1);
    }

    private Product buildSampleProduct(String productCode) {
        Product product = new Product();
        product.setProductCode(productCode);
        product.setName("Test Product");
        product.setCategory("Electronics");
        product.setUnitPrice(BigDecimal.valueOf(199.99));
        product.setStockQuantity(100);
        return product;
    }
}
