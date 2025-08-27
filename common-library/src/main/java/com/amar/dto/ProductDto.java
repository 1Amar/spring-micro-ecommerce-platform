package com.amar.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ProductDto {
    
    private Long id;
    
    @NotBlank(message = "Product name is required")
    @Size(max = 200, message = "Product name must not exceed 200 characters")
    private String name;
    
    private String description;
    
    @NotBlank(message = "SKU is required")
    @Size(max = 50, message = "SKU must not exceed 50 characters")
    private String sku;
    
    private String slug;
    
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private BigDecimal price;
    
    @DecimalMin(value = "0.0", message = "Compare at price must be non-negative")
    private BigDecimal compareAtPrice;
    
    @DecimalMin(value = "0.0", message = "Cost must be non-negative")
    private BigDecimal cost;
    
    @Min(value = 0, message = "Stock quantity cannot be negative")
    private Integer stockQuantity;
    
    @Min(value = 0, message = "Low stock threshold cannot be negative")
    private Integer lowStockThreshold;
    
    private Boolean trackInventory;
    private Boolean isActive;
    private Boolean isFeatured;
    private BigDecimal weight;
    private String dimensions;
    
    @Size(max = 100, message = "Brand must not exceed 100 characters")
    private String brand;
    
    private String metaTitle;
    private String metaDescription;
    private String tags;
    private Integer sortOrder;
    
    // Image information
    private String imageUrl;
    
    // Amazon-specific fields
    private Double stars;
    private Integer reviewCount;
    private Integer boughtInLastMonth;
    private Boolean isBestSeller;
    
    // Category information
    private Long categoryId;
    private String categoryName;
    
    
    // Computed fields
    private Boolean inStock;
    private Boolean lowStock;
    private Boolean onSale;
    private BigDecimal savingsAmount;
    private Double savingsPercentage;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Constructors
    public ProductDto() {}
    
    public ProductDto(Long id, String name, String sku, BigDecimal price) {
        this.id = id;
        this.name = name;
        this.sku = sku;
        this.price = price;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getSku() {
        return sku;
    }
    
    public void setSku(String sku) {
        this.sku = sku;
    }
    
    public String getSlug() {
        return slug;
    }
    
    public void setSlug(String slug) {
        this.slug = slug;
    }
    
    public BigDecimal getPrice() {
        return price;
    }
    
    public void setPrice(BigDecimal price) {
        this.price = price;
    }
    
    public BigDecimal getCompareAtPrice() {
        return compareAtPrice;
    }
    
    public void setCompareAtPrice(BigDecimal compareAtPrice) {
        this.compareAtPrice = compareAtPrice;
    }
    
    public BigDecimal getCost() {
        return cost;
    }
    
    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }
    
    public Integer getStockQuantity() {
        return stockQuantity;
    }
    
    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }
    
    public Integer getLowStockThreshold() {
        return lowStockThreshold;
    }
    
    public void setLowStockThreshold(Integer lowStockThreshold) {
        this.lowStockThreshold = lowStockThreshold;
    }
    
    public Boolean getTrackInventory() {
        return trackInventory;
    }
    
    public void setTrackInventory(Boolean trackInventory) {
        this.trackInventory = trackInventory;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public Boolean getIsFeatured() {
        return isFeatured;
    }
    
    public void setIsFeatured(Boolean isFeatured) {
        this.isFeatured = isFeatured;
    }
    
    public BigDecimal getWeight() {
        return weight;
    }
    
    public void setWeight(BigDecimal weight) {
        this.weight = weight;
    }
    
    public String getDimensions() {
        return dimensions;
    }
    
    public void setDimensions(String dimensions) {
        this.dimensions = dimensions;
    }
    
    public String getBrand() {
        return brand;
    }
    
    public void setBrand(String brand) {
        this.brand = brand;
    }
    
    public String getMetaTitle() {
        return metaTitle;
    }
    
    public void setMetaTitle(String metaTitle) {
        this.metaTitle = metaTitle;
    }
    
    public String getMetaDescription() {
        return metaDescription;
    }
    
    public void setMetaDescription(String metaDescription) {
        this.metaDescription = metaDescription;
    }
    
    public String getTags() {
        return tags;
    }
    
    public void setTags(String tags) {
        this.tags = tags;
    }
    
    public Integer getSortOrder() {
        return sortOrder;
    }
    
    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
    
    public Long getCategoryId() {
        return categoryId;
    }
    
    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }
    
    public String getCategoryName() {
        return categoryName;
    }
    
    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }
    
    
    public Boolean getInStock() {
        return inStock;
    }
    
    public void setInStock(Boolean inStock) {
        this.inStock = inStock;
    }
    
    public Boolean getLowStock() {
        return lowStock;
    }
    
    public void setLowStock(Boolean lowStock) {
        this.lowStock = lowStock;
    }
    
    public Boolean getOnSale() {
        return onSale;
    }
    
    public void setOnSale(Boolean onSale) {
        this.onSale = onSale;
    }
    
    public BigDecimal getSavingsAmount() {
        return savingsAmount;
    }
    
    public void setSavingsAmount(BigDecimal savingsAmount) {
        this.savingsAmount = savingsAmount;
    }
    
    public Double getSavingsPercentage() {
        return savingsPercentage;
    }
    
    public void setSavingsPercentage(Double savingsPercentage) {
        this.savingsPercentage = savingsPercentage;
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
    
    // Image URL
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    // Amazon-specific fields
    public Double getStars() {
        return stars;
    }
    
    public void setStars(Double stars) {
        this.stars = stars;
    }
    
    public Integer getReviewCount() {
        return reviewCount;
    }
    
    public void setReviewCount(Integer reviewCount) {
        this.reviewCount = reviewCount;
    }
    
    public Integer getBoughtInLastMonth() {
        return boughtInLastMonth;
    }
    
    public void setBoughtInLastMonth(Integer boughtInLastMonth) {
        this.boughtInLastMonth = boughtInLastMonth;
    }
    
    public Boolean getIsBestSeller() {
        return isBestSeller;
    }
    
    public void setIsBestSeller(Boolean isBestSeller) {
        this.isBestSeller = isBestSeller;
    }
}