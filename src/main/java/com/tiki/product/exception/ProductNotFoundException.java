package com.tiki.product.exception;

public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(String id) { super("Không tìm thấy sản phẩm " + id); }
}
