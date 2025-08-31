package com.amar.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.annotations.OptimisticLocking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

@Entity
@Table(
    name = "inventory",
    schema = "inventory_service_schema",
    indexes = {
        @Index(name = "idx_inventory_product_id", columnList = "product_id"),
        @Index(name = "idx_inventory_available_quantity", columnList = "available_quantity"),
        @Index(name = "idx_inventory_updated_at", columnList = "updated_at")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_inventory_product_id", columnNames = "product_id")
    }
)
@DynamicUpdate
@OptimisticLocking(type = OptimisticLockType.VERSION)
public class Inventory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;
    
    @Column(name = "product_id", nullable = false, unique = true)
    private Long productId;
    
    @Column(name = "quantity", nullable = false)
    private Integer quantity = 0;
    
    @Column(name = "reserved_quantity", nullable = false)
    private Integer reservedQuantity = 0;
    
    @Column(name = "available_quantity", insertable = false, updatable = false)
    private Integer availableQuantity; // Computed column: quantity - reserved_quantity
    
    @Column(name = "reorder_level")
    private Integer reorderLevel = 10;
    
    @Column(name = "max_stock_level")
    private Integer maxStockLevel = 1000;
    
    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    // Business methods
    public boolean isLowStock() {
        return this.availableQuantity != null && this.availableQuantity <= this.reorderLevel;
    }
    
    public boolean isOutOfStock() {
        return this.availableQuantity != null && this.availableQuantity <= 0;
    }
    
    public boolean canReserve(Integer requestedQuantity) {
        return this.availableQuantity != null && 
               requestedQuantity != null && 
               this.availableQuantity >= requestedQuantity;
    }
    
    public String getStockStatus() {
        if (isOutOfStock()) {
            return "OUT_OF_STOCK";
        } else if (isLowStock()) {
            return "LOW_STOCK";
        } else {
            return "IN_STOCK";
        }
    }
    
    public Integer getStockDeficit() {
        if (isLowStock()) {
            return this.reorderLevel - this.availableQuantity;
        }
        return 0;
    }
    
    // Synchronized methods for thread-safe operations
    public synchronized boolean reserveStock(Integer quantityToReserve) {
        if (!canReserve(quantityToReserve)) {
            return false;
        }
        this.reservedQuantity += quantityToReserve;
        return true;
    }
    
    public synchronized void releaseReservedStock(Integer quantityToRelease) {
        this.reservedQuantity = Math.max(0, this.reservedQuantity - quantityToRelease);
    }
    
    public synchronized void addStock(Integer quantityToAdd) {
        this.quantity += quantityToAdd;
    }
    
    public synchronized boolean removeStock(Integer quantityToRemove) {
        if (this.quantity < quantityToRemove) {
            return false;
        }
        this.quantity -= quantityToRemove;
        // Also reduce reserved quantity if necessary to maintain consistency
        if (this.reservedQuantity > this.quantity) {
            this.reservedQuantity = this.quantity;
        }
        return true;
    }
    
    // Constructors
    public Inventory() {}
    
    public Inventory(UUID id, Long productId, Integer quantity, Integer reservedQuantity,
                    Integer availableQuantity, Integer reorderLevel, Integer maxStockLevel,
                    Long version, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.productId = productId;
        this.quantity = quantity != null ? quantity : 0;
        this.reservedQuantity = reservedQuantity != null ? reservedQuantity : 0;
        this.availableQuantity = availableQuantity;
        this.reorderLevel = reorderLevel != null ? reorderLevel : 10;
        this.maxStockLevel = maxStockLevel != null ? maxStockLevel : 1000;
        this.version = version != null ? version : 0L;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
    
    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    
    public Integer getReservedQuantity() { return reservedQuantity; }
    public void setReservedQuantity(Integer reservedQuantity) { this.reservedQuantity = reservedQuantity; }
    
    public Integer getAvailableQuantity() { return availableQuantity; }
    public void setAvailableQuantity(Integer availableQuantity) { this.availableQuantity = availableQuantity; }
    
    public Integer getReorderLevel() { return reorderLevel; }
    public void setReorderLevel(Integer reorderLevel) { this.reorderLevel = reorderLevel; }
    
    public Integer getMaxStockLevel() { return maxStockLevel; }
    public void setMaxStockLevel(Integer maxStockLevel) { this.maxStockLevel = maxStockLevel; }
    
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}