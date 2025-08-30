package com.amar.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class CartDto {
    private String cartId;
    private String userId;
    private String sessionId;
    private List<CartItemDto> items;
    private BigDecimal totalAmount;
    private Instant createdAt;
    private Instant updatedAt;

    public CartDto() {
        this.items = new ArrayList<>();
        this.totalAmount = BigDecimal.ZERO;
    }

    public CartDto(String cartId, String userId, String sessionId, List<CartItemDto> items, 
                   BigDecimal totalAmount, Instant createdAt, Instant updatedAt) {
        this.cartId = cartId;
        this.userId = userId;
        this.sessionId = sessionId;
        this.items = items != null ? items : new ArrayList<>();
        this.totalAmount = totalAmount != null ? totalAmount : BigDecimal.ZERO;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters
    public String getCartId() { return cartId; }
    public String getUserId() { return userId; }
    public String getSessionId() { return sessionId; }
    public List<CartItemDto> getItems() { return items; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    // Setters
    public void setCartId(String cartId) { this.cartId = cartId; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public void setItems(List<CartItemDto> items) { this.items = items != null ? items : new ArrayList<>(); }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount != null ? totalAmount : BigDecimal.ZERO; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    // Utility methods
    public int getItemCount() {
        return items.stream().mapToInt(CartItemDto::getQuantity).sum();
    }

    public void calculateTotalAmount() {
        this.totalAmount = items.stream()
            .map(CartItemDto::getTotalPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void addItem(CartItemDto item) {
        if (items == null) {
            items = new ArrayList<>();
        }
        items.add(item);
        calculateTotalAmount();
    }

    @Override
    public String toString() {
        return "CartDto{" +
                "cartId='" + cartId + '\'' +
                ", userId='" + userId + '\'' +
                ", sessionId='" + sessionId + '\'' +
                ", itemCount=" + (items != null ? items.size() : 0) +
                ", totalAmount=" + totalAmount +
                '}';
    }
}