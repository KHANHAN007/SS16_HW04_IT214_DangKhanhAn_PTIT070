package com.tiki.product.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.tiki.product.dto.UpdateProductRequest;
import com.tiki.product.entity.Product;
import com.tiki.product.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {
    @Mock ProductRepository repository;
    @InjectMocks ProductService service;

    @Test void updateUsesEvictAndReadUsesCacheable() throws Exception {
        assertThat(ProductService.class.getMethod("getProductById", String.class)
                .getAnnotation(Cacheable.class)).isNotNull();
        CacheEvict evict = ProductService.class
                .getMethod("updateProduct", String.class, UpdateProductRequest.class)
                .getAnnotation(CacheEvict.class);
        assertThat(evict).isNotNull();
        assertThat(evict.beforeInvocation()).isFalse();
    }

    @Test void rejectsNegativePriceBeforeDatabaseWrite() {
        var request = new UpdateProductRequest("iPhone", new BigDecimal("-1"), "Mô tả");
        assertThatThrownBy(() -> service.updateProduct("P001", request))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("price");
        verifyNoInteractions(repository);
    }

    @Test void updatesDatabaseAndReturnsNewValue() {
        Product product = new Product("P001", "Cũ", BigDecimal.TEN, "Cũ");
        when(repository.findById("P001")).thenReturn(Optional.of(product));
        when(repository.saveAndFlush(product)).thenReturn(product);
        var request = new UpdateProductRequest("iPhone 15 Pro", new BigDecimal("24990000"), "Mới");

        var result = service.updateProduct("P001", request);

        assertThat(result.name()).isEqualTo("iPhone 15 Pro");
        assertThat(result.price()).isEqualByComparingTo("24990000");
        verify(repository).saveAndFlush(product);
    }
}
