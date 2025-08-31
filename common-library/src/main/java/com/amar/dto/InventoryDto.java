package com.amar.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

public class InventoryDto implements Serializable {
    
    @JsonProperty("id")
    private UUID id;
    
    @JsonProperty("productId")
    private Long productId;
    
    @JsonProperty("quantity")
    private Integer quantity;
    
    @JsonProperty("reservedQuantity")
    private Integer reservedQuantity;
    
    @JsonProperty("availableQuantity")
    private Integer availableQuantity;
    
    @JsonProperty("reorderLevel")
    private Integer reorderLevel;
    
    @JsonProperty("maxStockLevel")
    private Integer maxStockLevel;
    
    @JsonProperty("version")
    private Long version;
    
    @JsonProperty("createdAt")
    private LocalDateTime createdAt;
    
    @JsonProperty("updatedAt")
    private LocalDateTime updatedAt;
    
    // Additional fields for enhanced responses
    @JsonProperty("productName")
    private String productName;
    
    @JsonProperty("productAsin")
    private String productAsin;
    
    @JsonProperty("isLowStock")
    private Boolean isLowStock;
    
    @JsonProperty("isOutOfStock")
    private Boolean isOutOfStock;
    
    @JsonProperty("stockStatus")
    private String stockStatus; // IN_STOCK, LOW_STOCK, OUT_OF_STOCK

    // Constructors
    public InventoryDto() {}

    public InventoryDto(UUID id, Long productId, Integer quantity, Integer reservedQuantity, 
                       Integer availableQuantity, Integer reorderLevel, Integer maxStockLevel, 
                       Long version, LocalDateTime createdAt, LocalDateTime updatedAt,
                       String productName, String productAsin, Boolean isLowStock, 
                       Boolean isOutOfStock, String stockStatus) {
        this.id = id;
        this.productId = productId;
        this.quantity = quantity;
        this.reservedQuantity = reservedQuantity;
        this.availableQuantity = availableQuantity;
        this.reorderLevel = reorderLevel;
        this.maxStockLevel = maxStockLevel;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.productName = productName;
        this.productAsin = productAsin;
        this.isLowStock = isLowStock;
        this.isOutOfStock = isOutOfStock;
        this.stockStatus = stockStatus;
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

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getProductAsin() { return productAsin; }
    public void setProductAsin(String productAsin) { this.productAsin = productAsin; }

    public Boolean getIsLowStock() { return isLowStock; }
    public void setIsLowStock(Boolean isLowStock) { this.isLowStock = isLowStock; }

    public Boolean getIsOutOfStock() { return isOutOfStock; }
    public void setIsOutOfStock(Boolean isOutOfStock) { this.isOutOfStock = isOutOfStock; }

    public String getStockStatus() { return stockStatus; }
    public void setStockStatus(String stockStatus) { this.stockStatus = stockStatus; }
}