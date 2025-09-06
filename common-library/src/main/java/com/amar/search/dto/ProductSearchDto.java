package com.amar.search.dto;

import java.time.LocalDateTime;

public class ProductSearchDto {
    
    private Long productId;
    private String name;
    private String description;
    private String brand;
    private String categoryName;
    private Double price;
    private String imageUrl;
    private String sku;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Default constructor
    public ProductSearchDto() {}

    // All-args constructor
    public ProductSearchDto(Long productId, String name, String description, String brand, 
                           String categoryName, Double price, String imageUrl, String sku,
                           LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.productId = productId;
        this.name = name;
        this.description = description;
        this.brand = brand;
        this.categoryName = categoryName;
        this.price = price;
        this.imageUrl = imageUrl;
        this.sku = sku;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Builder pattern for easy construction
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private ProductSearchDto dto = new ProductSearchDto();

        public Builder productId(Long productId) {
            dto.productId = productId;
            return this;
        }

        public Builder name(String name) {
            dto.name = name;
            return this;
        }

        public Builder description(String description) {
            dto.description = description;
            return this;
        }

        public Builder brand(String brand) {
            dto.brand = brand;
            return this;
        }

        public Builder categoryName(String categoryName) {
            dto.categoryName = categoryName;
            return this;
        }

        public Builder price(Double price) {
            dto.price = price;
            return this;
        }

        public Builder imageUrl(String imageUrl) {
            dto.imageUrl = imageUrl;
            return this;
        }

        public Builder sku(String sku) {
            dto.sku = sku;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            dto.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(LocalDateTime updatedAt) {
            dto.updatedAt = updatedAt;
            return this;
        }

        public ProductSearchDto build() {
            return dto;
        }
    }

    // Getters and Setters
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
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
    public String toString() {
        return "ProductSearchDto{" +
                "productId=" + productId +
                ", name='" + name + '\'' +
                ", brand='" + brand + '\'' +
                ", categoryName='" + categoryName + '\'' +
                ", price=" + price +
                ", sku='" + sku + '\'' +
                '}';
    }
}