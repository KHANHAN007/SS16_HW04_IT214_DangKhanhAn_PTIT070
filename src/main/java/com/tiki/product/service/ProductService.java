package com.tiki.product.service;

import com.tiki.product.dto.*;
import com.tiki.product.entity.Product;
import com.tiki.product.exception.ProductNotFoundException;
import com.tiki.product.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {
    private static final Logger log = LoggerFactory.getLogger(ProductService.class);
    private final ProductRepository repository;
    public ProductService(ProductRepository repository) { this.repository = repository; }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "products", key = "#productId", sync = true)
    public ProductDTO getProductById(String productId) {
        String id = validateId(productId);
        log.info("Cache miss hoặc Redis lỗi - đọc productId={} từ DB", id);
        return repository.findById(id).map(this::toDto)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    @Transactional
    @CacheEvict(cacheNames = "products", key = "#productId", beforeInvocation = false)
    public ProductDTO updateProduct(String productId, UpdateProductRequest request) {
        String id = validateId(productId);
        validateRequest(request);
        Product product = repository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
        product.update(request.name().trim(), request.price(), request.description());
        Product saved = repository.saveAndFlush(product);
        log.info("DB đã cập nhật productId={}, version={}; evict cache sau commit method", id, saved.getVersion());
        return toDto(saved);
    }

    private String validateId(String id) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("productId không được rỗng");
        return id.trim();
    }
    private void validateRequest(UpdateProductRequest request) {
        if (request == null) throw new IllegalArgumentException("UpdateProductRequest không được null");
        if (request.name() == null || request.name().isBlank()) throw new IllegalArgumentException("name không được rỗng");
        if (request.price() == null || request.price().signum() < 0) throw new IllegalArgumentException("price phải >= 0");
    }
    private ProductDTO toDto(Product p) {
        return new ProductDTO(p.getProductId(), p.getName(), p.getPrice(), p.getDescription(), p.getVersion());
    }
}
