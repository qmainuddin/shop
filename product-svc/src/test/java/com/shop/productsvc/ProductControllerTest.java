package com.shop.productsvc;

import com.shop.productsvc.model.Product;
import com.shop.productsvc.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    private Product savedProduct;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();

        Product p = new Product();
        p.setId(UUID.randomUUID());
        p.setName("Wireless Headphones");
        p.setDescription("Premium noise-cancelling headphones");
        p.setPrice(new BigDecimal("149.99"));
        p.setStockQuantity(42);
        p.setImageUrl("https://images.example.com/headphones.jpg");
        p.setCreatedAt(Instant.now());
        savedProduct = productRepository.save(p);

        Product p2 = new Product();
        p2.setId(UUID.randomUUID());
        p2.setName("Laptop Stand");
        p2.setDescription("Adjustable aluminium stand");
        p2.setPrice(new BigDecimal("39.95"));
        p2.setStockQuantity(120);
        p2.setCreatedAt(Instant.now());
        productRepository.save(p2);
    }

    @Test
    void listProducts_returns200_withNonEmptyArray() throws Exception {
        mockMvc.perform(get("/api/products").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$[*].name", hasItem("Wireless Headphones")));
    }

    @Test
    void getProduct_returns200_whenProductExists() throws Exception {
        mockMvc.perform(get("/api/products/{id}", savedProduct.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(savedProduct.getId().toString()))
                .andExpect(jsonPath("$.name").value("Wireless Headphones"))
                .andExpect(jsonPath("$.price").value(149.99));
    }

    @Test
    void getProduct_returns404_whenProductNotFound() throws Exception {
        UUID unknownId = UUID.randomUUID();
        mockMvc.perform(get("/api/products/{id}", unknownId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void health_returns200_withStatusUp() throws Exception {
        mockMvc.perform(get("/health").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
