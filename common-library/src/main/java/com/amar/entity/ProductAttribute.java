package com.amar.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "product_attributes")
public class ProductAttribute {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    
    @NotBlank(message = "Attribute name is required")
    @Size(max = 100, message = "Attribute name must not exceed 100 characters")
    @Column(name = "name", nullable = false, length = 100)
    private String name;
    
    @NotBlank(message = "Attribute value is required")
    @Size(max = 500, message = "Attribute value must not exceed 500 characters")
    @Column(name = "value", nullable = false, length = 500)
    private String value;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private AttributeType type = AttributeType.TEXT;
    
    @Column(name = "is_filterable")
    private Boolean isFilterable = false;
    
    @Column(name = "is_required")
    private Boolean isRequired = false;
    
    @Column(name = "sort_order")
    private Integer sortOrder = 0;
    
    @Size(max = 100, message = "Unit must not exceed 100 characters")
    @Column(name = "unit", length = 100)
    private String unit;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum AttributeType {
        TEXT,
        NUMBER,
        BOOLEAN,
        DATE,
        COLOR,
        SIZE,
        MATERIAL,
        CUSTOM
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Constructors
    public ProductAttribute() {}
    
    public ProductAttribute(Product product, String name, String value) {
        this.product = product;
        this.name = name;
        this.value = value;
    }
    
    public ProductAttribute(Product product, String name, String value, AttributeType type) {
        this.product = product;
        this.name = name;
        this.value = value;
        this.type = type;
    }
    
    // Helper methods
    public String getFormattedValue() {
        if (unit != null && !unit.trim().isEmpty()) {
            return value + " " + unit;
        }
        return value;
    }
    
    public boolean isNumeric() {
        return type == AttributeType.NUMBER;
    }
    
    public boolean isBoolean() {
        return type == AttributeType.BOOLEAN;
    }
    
    public Double getNumericValue() {
        if (!isNumeric()) return null;
        
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    public Boolean getBooleanValue() {
        if (!isBoolean()) return null;
        
        return Boolean.parseBoolean(value);
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Product getProduct() {
        return product;
    }
    
    public void setProduct(Product product) {
        this.product = product;
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
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductAttribute that = (ProductAttribute) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "ProductAttribute{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", value='" + value + '\'' +
                ", type=" + type +
                ", isFilterable=" + isFilterable +
                '}';
    }
}