package com.tiki.product;

import com.tiki.product.entity.Product;
import com.tiki.product.repository.ProductRepository;
import java.math.BigDecimal;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableCaching
public class ProductApplication {
    public static void main(String[] args) { SpringApplication.run(ProductApplication.class, args); }
    @Bean CommandLineRunner seed(ProductRepository repository) {
        return args -> repository.findById("P001").orElseGet(() -> repository.save(
                new Product("P001", "iPhone 15", new BigDecimal("19990000"), "Điện thoại Apple")));
    }
}
