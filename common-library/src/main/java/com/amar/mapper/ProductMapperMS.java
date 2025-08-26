package com.amar.mapper;

import com.amar.dto.ProductDto;
import com.amar.entity.Product;
import com.amar.entity.Category;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", 
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ProductMapperMS {
    
    // Product mappings
    @Mapping(source = "category.id", target = "categoryId")
    @Mapping(source = "category.name", target = "categoryName")
    @Mapping(target = "inStock", expression = "java(product.isInStock())")
    @Mapping(target = "lowStock", expression = "java(product.isLowStock())")
    @Mapping(target = "onSale", expression = "java(product.isOnSale())")
    @Mapping(target = "savingsAmount", expression = "java(product.getSavingsAmount())")
    @Mapping(target = "savingsPercentage", expression = "java(product.getSavingsPercentage())")
    ProductDto toDto(Product product);
    
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Product toEntity(ProductDto productDto);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDto(ProductDto productDto, @MappingTarget Product product);
    
    List<ProductDto> toDtoList(List<Product> products);
    List<Product> toEntityList(List<ProductDto> productDtos);
    
    // Utility method for mapping category ID to Category entity
    @Named("categoryIdToCategory")
    default Category mapCategoryIdToCategory(Long categoryId) {
        if (categoryId == null) return null;
        Category category = new Category();
        category.setId(categoryId);
        return category;
    }
    
    // Utility method for mapping Category entity to ID
    @Named("categoryToCategoryId") 
    default Long mapCategoryToCategoryId(Category category) {
        return category != null ? category.getId() : null;
    }
}