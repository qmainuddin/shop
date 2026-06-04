package com.shop.productsvc;

import com.shop.productsvc.dto.ProductResponse;
import com.shop.productsvc.model.Product;
import com.shop.productsvc.repository.ProductRepository;
import com.shop.productsvc.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private Product sampleProduct;
    private UUID sampleId;

    @BeforeEach
    void setUp() {
        sampleId = UUID.randomUUID();
        sampleProduct = new Product();
        sampleProduct.setId(sampleId);
        sampleProduct.setName("Wireless Headphones");
        sampleProduct.setDescription("Great headphones");
        sampleProduct.setPrice(new BigDecimal("149.99"));
        sampleProduct.setStockQuantity(50);
        sampleProduct.setImageUrl("https://images.example.com/headphones.jpg");
        sampleProduct.setCreatedAt(Instant.now());
    }

    @Test
    void findAll_returnsAllProducts() {
        when(productRepository.findAll()).thenReturn(List.of(sampleProduct));

        List<ProductResponse> result = productService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Wireless Headphones");
        assertThat(result.get(0).getPrice()).isEqualByComparingTo(new BigDecimal("149.99"));
    }

    @Test
    void findAll_returnsEmptyList_whenNoProducts() {
        when(productRepository.findAll()).thenReturn(List.of());

        List<ProductResponse> result = productService.findAll();

        assertThat(result).isEmpty();
    }

    @Test
    void findById_returnsProduct_whenFound() {
        when(productRepository.findById(sampleId)).thenReturn(Optional.of(sampleProduct));

        Optional<ProductResponse> result = productService.findById(sampleId);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(sampleId);
        assertThat(result.get().getName()).isEqualTo("Wireless Headphones");
    }

    @Test
    void findById_returnsEmpty_whenNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(productRepository.findById(unknownId)).thenReturn(Optional.empty());

        Optional<ProductResponse> result = productService.findById(unknownId);

        assertThat(result).isEmpty();
    }

    @Test
    void productResponse_mapsAllFields() {
        when(productRepository.findById(sampleId)).thenReturn(Optional.of(sampleProduct));

        ProductResponse resp = productService.findById(sampleId).orElseThrow();

        assertThat(resp.getId()).isEqualTo(sampleId);
        assertThat(resp.getName()).isEqualTo(sampleProduct.getName());
        assertThat(resp.getDescription()).isEqualTo(sampleProduct.getDescription());
        assertThat(resp.getPrice()).isEqualByComparingTo(sampleProduct.getPrice());
        assertThat(resp.getStockQuantity()).isEqualTo(sampleProduct.getStockQuantity());
        assertThat(resp.getImageUrl()).isEqualTo(sampleProduct.getImageUrl());
        assertThat(resp.getCreatedAt()).isEqualTo(sampleProduct.getCreatedAt());
    }
}
