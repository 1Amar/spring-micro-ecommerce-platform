package com.amar.cart.service;

public class InsufficientStockException extends RuntimeException {
    
    private final Long productId;
    private final int requestedQuantity;
    private final int availableQuantity;
    
    public InsufficientStockException(String message, Long productId, int requestedQuantity, int availableQuantity) {
        super(message);
        this.productId = productId;
        this.requestedQuantity = requestedQuantity;
        this.availableQuantity = availableQuantity;
    }
    
    public InsufficientStockException(String message, Long productId, int requestedQuantity) {
        this(message, productId, requestedQuantity, 0);
    }
    
    public Long getProductId() {
        return productId;
    }
    
    public int getRequestedQuantity() {
        return requestedQuantity;
    }
    
    public int getAvailableQuantity() {
        return availableQuantity;
    }
}