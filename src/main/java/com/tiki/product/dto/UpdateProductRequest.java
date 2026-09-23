package com.tiki.product.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record UpdateProductRequest(
        @NotBlank(message = "name không được rỗng")
        @Size(max = 150, message = "name không được quá 150 ký tự") String name,
        @NotNull(message = "price không được null")
        @PositiveOrZero(message = "price phải lớn hơn hoặc bằng 0") BigDecimal price,
        @Size(max = 500, message = "description không được quá 500 ký tự") String description) {}
