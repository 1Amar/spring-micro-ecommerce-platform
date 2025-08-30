package com.amar.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class UpdateCartItemRequest {
    
    @NotNull(message = "Product ID is required")
    private Long productId;
    
    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Max(value = 99, message = "Quantity cannot exceed 99")
    private Integer quantity;

    public UpdateCartItemRequest() {}

    public UpdateCartItemRequest(Long productId, Integer quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }

    // Getters
    public Long getProductId() { return productId; }
    public Integer getQuantity() { return quantity; }

    // Setters
    public void setProductId(Long productId) { this.productId = productId; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    @Override
    public String toString() {
        return "UpdateCartItemRequest{" +
                "productId=" + productId +
                ", quantity=" + quantity +
                '}';
    }
}