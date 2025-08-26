package com.amar.dto;

import com.amar.entity.ProductAttribute.AttributeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class ProductAttributeDto {
    
    private Long id;
    private Long productId;
    
    @NotBlank(message = "Attribute name is required")
    @Size(max = 100, message = "Attribute name must not exceed 100 characters")
    private String name;
    
    @NotBlank(message = "Attribute value is required")
    @Size(max = 500, message = "Attribute value must not exceed 500 characters")
    private String value;
    
    private AttributeType type;
    private Boolean isFilterable;
    private Boolean isRequired;
    private Integer sortOrder;
    
    @Size(max = 100, message = "Unit must not exceed 100 characters")
    private String unit;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Constructors
    public ProductAttributeDto() {}
    
    public ProductAttributeDto(String name, String value) {
        this.name = name;
        this.value = value;
    }
    
    public ProductAttributeDto(String name, String value, AttributeType type) {
        this.name = name;
        this.value = value;
        this.type = type;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getProductId() {
        return productId;
    }
    
    public void setProductId(Long productId) {
        this.productId = productId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getValue() {
        return value;
    }
    
    public void setValue(String value) {
        this.value = value;
    }
    
    public AttributeType getType() {
        return type;
    }
    
    public void setType(AttributeType type) {
        this.type = type;
    }
    
    public Boolean getIsFilterable() {
        return isFilterable;
    }
    
    public void setIsFilterable(Boolean isFilterable) {
        this.isFilterable = isFilterable;
    }
    
    public Boolean getIsRequired() {
        return isRequired;
    }
    
    public void setIsRequired(Boolean isRequired) {
        this.isRequired = isRequired;
    }
    
    public Integer getSortOrder() {
        return sortOrder;
    }
    
    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
    
    public String getUnit() {
        return unit;
    }
    
    public void setUnit(String unit) {
        this.unit = unit;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}