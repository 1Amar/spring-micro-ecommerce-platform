package com.amar.dto;

import java.math.BigDecimal;

/**
 * Internal data structure for storing cart items in Redis
 */
public class CartItemData {
    private Integer quantity;
    private BigDecimal price;
    private String addedAt;
    private String productName;
    private String imageUrl;

    public CartItemData() {}

    public CartItemData(Integer quantity, BigDecimal price, String addedAt, String productName, String imageUrl) {
        this.quantity = quantity;
        this.price = price;
        this.addedAt = addedAt;
        this.productName = productName;
        this.imageUrl = imageUrl;
    }

    // Getters
    public Integer getQuantity() { return quantity; }
    public BigDecimal getPrice() { return price; }
    public String getAddedAt() { return addedAt; }
    public String getProductName() { return productName; }
    public String getImageUrl() { return imageUrl; }

    // Setters
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public void setAddedAt(String addedAt) { this.addedAt = addedAt; }
    public void setProductName(String productName) { this.productName = productName; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    @Override
    public String toString() {
        return "CartItemData{" +
                "quantity=" + quantity +
                ", price=" + price +
                ", productName='" + productName + '\'' +
                '}';
    }
}