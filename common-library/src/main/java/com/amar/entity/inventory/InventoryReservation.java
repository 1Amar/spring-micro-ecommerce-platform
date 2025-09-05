package com.amar.entity.inventory;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "inventory_reservations",
    schema = "inventory_service_schema",
    indexes = {
        @Index(name = "idx_reservations_product_id", columnList = "product_id"),
        @Index(name = "idx_reservations_order_id", columnList = "order_id"),
        @Index(name = "idx_reservations_expires_at", columnList = "expires_at"),
        @Index(name = "idx_reservations_user_id", columnList = "user_id"),
        @Index(name = "idx_reservations_session_id", columnList = "session_id"),
        @Index(name = "idx_reservations_expired", columnList = "is_expired, expires_at")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_reservations_order_id", columnNames = "order_id")
    }
)
public class InventoryReservation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;
    
    @Column(name = "product_id", nullable = false)
    private Long productId;
    
    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;
    
    @Column(name = "session_id")
    private String sessionId;
    
    @Column(name = "user_id")
    private String userId;
    
    @Column(name = "quantity_reserved", nullable = false)
    private Integer quantityReserved;
    
    @Column(name = "reserved_by", nullable = false)
    private String reservedBy;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "reservation_type", nullable = false)
    private ReservationType reservationType = ReservationType.CHECKOUT;
    
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    
    @Column(name = "is_expired", nullable = false)
    private Boolean isExpired = false;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum ReservationType {
        CHECKOUT,
        HOLD,
        ADMIN
    }
    
    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        
        // Set reserved_by based on user or session
        if (this.reservedBy == null) {
            this.reservedBy = this.userId != null ? this.userId : this.sessionId;
        }
        
        // Set default expiration if not set
        if (this.expiresAt == null) {
            this.expiresAt = now.plusMinutes(15); // Default 15 minutes
        }
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
        
        // Update expiration status
        if (!this.isExpired && LocalDateTime.now().isAfter(this.expiresAt)) {
            this.isExpired = true;
        }
    }
    
    // Business methods
    public boolean isExpired() {
        if (this.isExpired) {
            return true;
        }
        return LocalDateTime.now().isAfter(this.expiresAt);
    }
    
    public long getRemainingTimeMinutes() {
        if (isExpired()) {
            return 0;
        }
        return ChronoUnit.MINUTES.between(LocalDateTime.now(), this.expiresAt);
    }
    
    public boolean canExtend() {
        return !isExpired() && getRemainingTimeMinutes() < 60; // Can extend if less than 1 hour remaining
    }
    
    public void extendExpiration(int additionalMinutes) {
        if (canExtend()) {
            this.expiresAt = this.expiresAt.plusMinutes(additionalMinutes);
            this.isExpired = false;
        }
    }
    
    public void markAsExpired() {
        this.isExpired = true;
    }
    
    public boolean belongsToUser(String userId, String sessionId) {
        if (this.userId != null) {
            return this.userId.equals(userId);
        }
        return this.sessionId != null && this.sessionId.equals(sessionId);
    }
    
    // Constructors
    public InventoryReservation() {}
    
    public InventoryReservation(UUID id, Long productId, UUID orderId, String sessionId,
                               String userId, Integer quantityReserved, String reservedBy,
                               ReservationType reservationType, LocalDateTime expiresAt,
                               Boolean isExpired, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.productId = productId;
        this.orderId = orderId;
        this.sessionId = sessionId;
        this.userId = userId;
        this.quantityReserved = quantityReserved;
        this.reservedBy = reservedBy;
        this.reservationType = reservationType != null ? reservationType : ReservationType.CHECKOUT;
        this.expiresAt = expiresAt;
        this.isExpired = isExpired != null ? isExpired : false;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
    
    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    
    public UUID getOrderId() { return orderId; }
    public void setOrderId(UUID orderId) { this.orderId = orderId; }
    
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public Integer getQuantityReserved() { return quantityReserved; }
    public void setQuantityReserved(Integer quantityReserved) { this.quantityReserved = quantityReserved; }
    
    public String getReservedBy() { return reservedBy; }
    public void setReservedBy(String reservedBy) { this.reservedBy = reservedBy; }
    
    public ReservationType getReservationType() { return reservationType; }
    public void setReservationType(ReservationType reservationType) { this.reservationType = reservationType; }
    
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    
    public Boolean getIsExpired() { return isExpired; }
    public void setIsExpired(Boolean isExpired) { this.isExpired = isExpired; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}