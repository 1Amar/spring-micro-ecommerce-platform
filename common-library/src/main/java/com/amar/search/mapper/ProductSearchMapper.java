package com.amar.search.mapper;

import com.amar.dto.ProductDto;
import com.amar.search.dto.ProductSearchDto;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ProductSearchMapper {

    /**
     * Convert ProductDto to ProductSearchDto
     */
    public ProductSearchDto toSearchDto(ProductDto productDto) {
        if (productDto == null) {
            return null;
        }

        return ProductSearchDto.builder()
                .productId(productDto.getId())
                .name(productDto.getName())
                .description(productDto.getDescription())
                .brand(productDto.getBrand())
                .categoryName(productDto.getCategoryName() != null ? productDto.getCategoryName() : "")
                .price(productDto.getPrice() != null ? productDto.getPrice().doubleValue() : 0.0)
                .imageUrl(productDto.getImageUrl())
                .sku(productDto.getSku())
                .createdAt(productDto.getCreatedAt())
                .updatedAt(productDto.getUpdatedAt())
                .build();
    }

    /**
     * Convert ProductSearchDto back to ProductDto (if needed)
     */
    public ProductDto toProductDto(ProductSearchDto searchDto) {
        if (searchDto == null) {
            return null;
        }

        ProductDto productDto = new ProductDto();
        productDto.setId(searchDto.getProductId());
        productDto.setName(searchDto.getName());
        productDto.setDescription(searchDto.getDescription());
        productDto.setBrand(searchDto.getBrand());
        productDto.setImageUrl(searchDto.getImageUrl());
        productDto.setSku(searchDto.getSku());
        productDto.setCreatedAt(searchDto.getCreatedAt());
        productDto.setUpdatedAt(searchDto.getUpdatedAt());
        
        if (searchDto.getPrice() != null) {
            productDto.setPrice(java.math.BigDecimal.valueOf(searchDto.getPrice()));
        }

        // Note: Category object reconstruction would need additional logic
        // For now, we'll leave it null as this is primarily for search results
        
        return productDto;
    }
}