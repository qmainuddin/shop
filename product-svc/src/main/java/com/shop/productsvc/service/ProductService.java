package com.shop.productsvc.service;

import com.shop.productsvc.dto.ProductResponse;
import com.shop.productsvc.model.Product;
import com.shop.productsvc.repository.ProductRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Cacheable(cacheNames = "products", key = "'all'")
    public List<ProductResponse> findAll() {
        return productRepository.findAll()
                .stream()
                .map(ProductResponse::from)
                .collect(Collectors.toList());
    }

    public Optional<ProductResponse> findById(UUID id) {
        return productRepository.findById(id)
                .map(ProductResponse::from);
    }
}
