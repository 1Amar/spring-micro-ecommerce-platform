package com.amar.entity.product;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "products")
public class Product {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Product name is required")
    @Size(max = 200, message = "Product name must not exceed 200 characters")
    @Column(name = "name", nullable = false, length = 200)
    private String name;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @NotBlank(message = "SKU is required")
    @Size(max = 50, message = "SKU must not exceed 50 characters")
    @Column(name = "sku", nullable = false, unique = true, length = 50)
    private String sku;
    
    @Column(name = "slug", unique = true, length = 200)
    private String slug;
    
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
    
    @DecimalMin(value = "0.0", message = "Compare at price must be non-negative")
    @Column(name = "compare_at_price", precision = 10, scale = 2)
    private BigDecimal compareAtPrice;
    
    @DecimalMin(value = "0.0", message = "Cost must be non-negative")
    @Column(name = "cost", precision = 10, scale = 2)
    private BigDecimal cost;
    
    @Min(value = 0, message = "Stock quantity cannot be negative")
    @Column(name = "stock_quantity")
    private Integer stockQuantity = 0;
    
    @Min(value = 0, message = "Low stock threshold cannot be negative")
    @Column(name = "low_stock_threshold")
    private Integer lowStockThreshold = 1;
    
    @Column(name = "track_inventory")
    private Boolean trackInventory = true;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "is_featured")
    private Boolean isFeatured = false;
    
    @Column(name = "weight", precision = 8, scale = 3)
    private BigDecimal weight;
    
    @Column(name = "dimensions")
    private String dimensions;
    
    @Size(max = 100, message = "Brand must not exceed 100 characters")
    @Column(name = "brand", length = 100)
    private String brand;
    
    // Product image fields (single image per product)
    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;
    
    @Size(max = 200, message = "Image alt text must not exceed 200 characters")
    @Column(name = "image_alt_text", length = 200)
    private String imageAltText;
    
    @Column(name = "product_url", columnDefinition = "TEXT")
    private String productUrl;
    
    @DecimalMin(value = "0.0", message = "Stars must be non-negative")
    @DecimalMax(value = "5.0", message = "Stars cannot exceed 5.0")
    @Column(name = "stars", precision = 3, scale = 2)
    private BigDecimal stars;
    
    @Min(value = 0, message = "Review count cannot be negative")
    @Column(name = "review_count")
    private Integer reviewCount = 0;
    
    @Column(name = "is_best_seller")
    private Boolean isBestSeller = false;
    
    @Min(value = 0, message = "Bought in last month cannot be negative")
    @Column(name = "bought_in_last_month")
    private Integer boughtInLastMonth = 0;
    
    @Column(name = "meta_title")
    private String metaTitle;
    
    @Column(name = "meta_description")
    private String metaDescription;
    
    @Column(name = "tags", columnDefinition = "TEXT[]")
    private String[] tags;
    
    @Column(name = "sort_order")
    private Integer sortOrder = 0;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;
    
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (slug == null && name != null) {
            slug = generateSlug(name);
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Constructors
    public Product() {}
    
    public Product(String name, String sku, BigDecimal price) {
        this.name = name;
        this.sku = sku;
        this.price = price;
        this.slug = generateSlug(name);
    }
    
    private String generateSlug(String name) {
        if (name == null) return null;
        return name.toLowerCase()
                   .replaceAll("[^a-z0-9\\s-]", "")
                   .replaceAll("\\s+", "-")
                   .replaceAll("-+", "-")
                   .trim();
    }
    
    // Helper methods
    public boolean isInStock() {
        return !trackInventory || stockQuantity > 0;
    }
    
    public boolean isLowStock() {
        return trackInventory && stockQuantity <= lowStockThreshold;
    }
    
    public boolean isOnSale() {
        return compareAtPrice != null && compareAtPrice.compareTo(price) > 0;
    }
    
    public BigDecimal getSavingsAmount() {
        if (!isOnSale()) return BigDecimal.ZERO;
        return compareAtPrice.subtract(price);
    }
    
    public double getSavingsPercentage() {
        if (!isOnSale()) return 0.0;
        return getSavingsAmount().divide(compareAtPrice, 4, java.math.RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100)).doubleValue();
    }
    
    // Amazon-specific helper methods
    public boolean hasRating() {
        return stars != null && stars.compareTo(BigDecimal.ZERO) > 0;
    }
    
    public boolean isHighlyRated() {
        return hasRating() && stars.compareTo(new BigDecimal("4.0")) >= 0;
    }
    
    public boolean isPopular() {
        return boughtInLastMonth != null && boughtInLastMonth > 100;
    }
    
    public String getFormattedRating() {
        if (!hasRating()) return "No rating";
        return String.format("%.1f (%d reviews)", stars.doubleValue(), reviewCount != null ? reviewCount : 0);
    }
    
    public String getPopularityText() {
        if (isBestSeller != null && isBestSeller) return "Best Seller";
        if (isPopular()) return String.format("%d+ bought last month", boughtInLastMonth);
        return "";
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
        this.slug = generateSlug(name);
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
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    public String getImageAltText() {
        return imageAltText;
    }
    
    public void setImageAltText(String imageAltText) {
        this.imageAltText = imageAltText;
    }
    
    public String getProductUrl() {
        return productUrl;
    }
    
    public void setProductUrl(String productUrl) {
        this.productUrl = productUrl;
    }
    
    public BigDecimal getStars() {
        return stars;
    }
    
    public void setStars(BigDecimal stars) {
        this.stars = stars;
    }
    
    public Integer getReviewCount() {
        return reviewCount;
    }
    
    public void setReviewCount(Integer reviewCount) {
        this.reviewCount = reviewCount;
    }
    
    public Boolean getIsBestSeller() {
        return isBestSeller;
    }
    
    public void setIsBestSeller(Boolean isBestSeller) {
        this.isBestSeller = isBestSeller;
    }
    
    public Integer getBoughtInLastMonth() {
        return boughtInLastMonth;
    }
    
    public void setBoughtInLastMonth(Integer boughtInLastMonth) {
        this.boughtInLastMonth = boughtInLastMonth;
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
    
    public String[] getTags() {
        return tags;
    }
    
    public void setTags(String[] tags) {
        this.tags = tags;
    }
    
    public Integer getSortOrder() {
        return sortOrder;
    }
    
    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
    
    public Category getCategory() {
        return category;
    }
    
    public void setCategory(Category category) {
        this.category = category;
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
        Product product = (Product) o;
        return Objects.equals(id, product.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Product{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", sku='" + sku + '\'' +
                ", price=" + price +
                ", isActive=" + isActive +
                '}';
    }
}