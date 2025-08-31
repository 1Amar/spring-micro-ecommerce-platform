package com.amar.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.UUID;

public class InventoryReservationDto {

    @JsonProperty("id")
    private UUID id;

    @JsonProperty("productId")
    private Long productId;

    @JsonProperty("orderId")
    private UUID orderId;

    @JsonProperty("userId")
    private String userId;

    @JsonProperty("sessionId")
    private String sessionId;

    @JsonProperty("quantityRequested")
    private Integer quantityRequested;

    @JsonProperty("quantityReserved")
    private Integer quantityReserved;

    @JsonProperty("reservedBy")
    private String reservedBy;

    @JsonProperty("reservationType")
    private String reservationType;

    @JsonProperty("expiresAt")
    private LocalDateTime expiresAt;

    @JsonProperty("isExpired")
    private Boolean isExpired;

    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    // Constructors
    public InventoryReservationDto() {}

    public InventoryReservationDto(UUID id, Long productId, UUID orderId, String userId, String sessionId,
                                  Integer quantityRequested, Integer quantityReserved, String reservedBy,
                                  String reservationType, LocalDateTime expiresAt, Boolean isExpired,
                                  LocalDateTime createdAt) {
        this.id = id;
        this.productId = productId;
        this.orderId = orderId;
        this.userId = userId;
        this.sessionId = sessionId;
        this.quantityRequested = quantityRequested;
        this.quantityReserved = quantityReserved;
        this.reservedBy = reservedBy;
        this.reservationType = reservationType;
        this.expiresAt = expiresAt;
        this.isExpired = isExpired;
        this.createdAt = createdAt;
    }

    // Getters and setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
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

    public Integer getQuantityRequested() {
        return quantityRequested;
    }

    public void setQuantityRequested(Integer quantityRequested) {
        this.quantityRequested = quantityRequested;
    }

    public Integer getQuantityReserved() {
        return quantityReserved;
    }

    public void setQuantityReserved(Integer quantityReserved) {
        this.quantityReserved = quantityReserved;
    }

    public String getReservedBy() {
        return reservedBy;
    }

    public void setReservedBy(String reservedBy) {
        this.reservedBy = reservedBy;
    }

    public String getReservationType() {
        return reservationType;
    }

    public void setReservationType(String reservationType) {
        this.reservationType = reservationType;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Boolean getIsExpired() {
        return isExpired;
    }

    public void setIsExpired(Boolean isExpired) {
        this.isExpired = isExpired;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}