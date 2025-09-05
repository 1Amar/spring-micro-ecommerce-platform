package com.amar.dto;

import java.math.BigDecimal;
import java.time.Instant;

public class CartItemDto {
    private Long productId;
    private String productName;
    private String imageUrl;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private Instant addedAt;
    
    // Inventory information
    private Integer availableStock;
    private Boolean inStock;
    private String stockStatus;

    public CartItemDto() {}

    public CartItemDto(Long productId, String productName, String imageUrl, 
                       Integer quantity, BigDecimal unitPrice, Instant addedAt) {
        this.productId = productId;
        this.productName = productName;
        this.imageUrl = imageUrl;
        this.quantity = quantity != null ? quantity : 0;
        this.unitPrice = unitPrice != null ? unitPrice : BigDecimal.ZERO;
        this.addedAt = addedAt;
        calculateTotalPrice();
    }

    // Getters
    public Long getProductId() { return productId; }
    public String getProductName() { return productName; }
    public String getImageUrl() { return imageUrl; }
    public Integer getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getTotalPrice() { return totalPrice; }
    public Instant getAddedAt() { return addedAt; }
    public Integer getAvailableStock() { return availableStock; }
    public Boolean getInStock() { return inStock; }
    public String getStockStatus() { return stockStatus; }

    // Setters
    public void setProductId(Long productId) { this.productId = productId; }
    public void setProductName(String productName) { this.productName = productName; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    
    public void setQuantity(Integer quantity) { 
        this.quantity = quantity != null ? quantity : 0;
        calculateTotalPrice();
    }
    
    public void setUnitPrice(BigDecimal unitPrice) { 
        this.unitPrice = unitPrice != null ? unitPrice : BigDecimal.ZERO;
        calculateTotalPrice();
    }
    
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }
    public void setAddedAt(Instant addedAt) { this.addedAt = addedAt; }
    public void setAvailableStock(Integer availableStock) { this.availableStock = availableStock; }
    public void setInStock(Boolean inStock) { this.inStock = inStock; }
    public void setStockStatus(String stockStatus) { this.stockStatus = stockStatus; }

    // Utility method
    public void calculateTotalPrice() {
        if (quantity != null && unitPrice != null) {
            this.totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
        } else {
            this.totalPrice = BigDecimal.ZERO;
        }
    }

    @Override
    public String toString() {
        return "CartItemDto{" +
                "productId=" + productId +
                ", productName='" + productName + '\'' +
                ", quantity=" + quantity +
                ", unitPrice=" + unitPrice +
                ", totalPrice=" + totalPrice +
                '}';
    }
}