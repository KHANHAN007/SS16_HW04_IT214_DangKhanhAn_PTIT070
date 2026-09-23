package com.tiki.product.controller;

import com.tiki.product.dto.*;
import com.tiki.product.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService service;
    public ProductController(ProductService service) { this.service = service; }
    @GetMapping("/{productId}") ProductDTO get(@PathVariable String productId) {
        return service.getProductById(productId);
    }
    @PutMapping("/{productId}") ProductDTO update(@PathVariable String productId,
            @Valid @RequestBody UpdateProductRequest request) {
        return service.updateProduct(productId, request);
    }
}
