package com.tiki.product.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public record ProductDTO(String productId, String name, BigDecimal price,
                         String description, long version) implements Serializable {}
