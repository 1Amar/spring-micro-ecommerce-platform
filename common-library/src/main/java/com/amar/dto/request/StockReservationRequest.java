package com.amar.dto.request;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class StockReservationRequest {
    
    @JsonProperty("orderId")
    @NotNull(message = "Order ID cannot be null")
    private UUID orderId;
    
    @JsonProperty("items")
    @NotNull(message = "Items list cannot be null")
    @Size(min = 1, message = "At least one item must be specified")
    private List<ReservationItem> items;
    
    @JsonProperty("userId")
    private String userId;
    
    @JsonProperty("sessionId")
    private String sessionId;
    
    @JsonProperty("reservationType")
    @NotBlank(message = "Reservation type cannot be blank")
    @Pattern(regexp = "CHECKOUT|HOLD|ADMIN", message = "Invalid reservation type")
    private String reservationType = "CHECKOUT";
    
    @JsonProperty("expirationMinutes")
    @Min(value = 1, message = "Expiration must be at least 1 minute")
    @Max(value = 60, message = "Expiration cannot exceed 60 minutes")
    private Integer expirationMinutes = 15; // Default 15 minutes

    // Default constructor
    public StockReservationRequest() {
    }

    // Full constructor
    public StockReservationRequest(UUID orderId, List<ReservationItem> items, String userId, String sessionId,
                                 String reservationType, Integer expirationMinutes) {
        this.orderId = orderId;
        this.items = items;
        this.userId = userId;
        this.sessionId = sessionId;
        this.reservationType = reservationType;
        this.expirationMinutes = expirationMinutes;
    }

    // Getters and setters
    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public List<ReservationItem> getItems() {
        return items;
    }

    public void setItems(List<ReservationItem> items) {
        this.items = items;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getReservationType() {
        return reservationType;
    }

    public void setReservationType(String reservationType) {
        this.reservationType = reservationType;
    }

    public Integer getExpirationMinutes() {
        return expirationMinutes;
    }

    public void setExpirationMinutes(Integer expirationMinutes) {
        this.expirationMinutes = expirationMinutes;
    }
    
    public static class ReservationItem {
        
        @JsonProperty("productId")
        @NotNull(message = "Product ID cannot be null")
        private Long productId;
        
        @JsonProperty("quantity")
        @NotNull(message = "Quantity cannot be null")
        @Min(value = 1, message = "Quantity must be at least 1")
        private Integer quantity;

        // Default constructor
        public ReservationItem() {
        }

        // Full constructor
        public ReservationItem(Long productId, Integer quantity) {
            this.productId = productId;
            this.quantity = quantity;
        }

        // Getters and setters
        public Long getProductId() {
            return productId;
        }

        public void setProductId(Long productId) {
            this.productId = productId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
    }
}