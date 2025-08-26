package com.amar.mapper;

import com.amar.dto.CategoryDto;
import com.amar.entity.Category;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", 
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CategoryMapperMS {
    
    // Full category mapping with parent info and children
    @Mapping(source = "parent.id", target = "parentId")
    @Mapping(source = "parent.name", target = "parentName")
    @Mapping(source = "children", target = "children", qualifiedByName = "mapActiveChildren")
    CategoryDto toDto(Category category);
    
    // Basic category mapping without children (to avoid recursion)
    @Mapping(source = "parent.id", target = "parentId")
    @Mapping(source = "parent.name", target = "parentName")
    @Mapping(target = "children", ignore = true)
    @Named("toBasicDto")
    CategoryDto toBasicDto(Category category);
    
    // Entity mapping
    @Mapping(target = "parent", ignore = true)
    @Mapping(target = "children", ignore = true)
    @Mapping(target = "products", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Category toEntity(CategoryDto categoryDto);
    
    // Update mapping
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "parent", ignore = true)
    @Mapping(target = "children", ignore = true)
    @Mapping(target = "products", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDto(CategoryDto categoryDto, @MappingTarget Category category);
    
    // List mappings
    List<CategoryDto> toDtoList(List<Category> categories);
    
    @IterableMapping(qualifiedByName = "toBasicDto")
    List<CategoryDto> toBasicDtoList(List<Category> categories);
    
    List<Category> toEntityList(List<CategoryDto> categoryDtos);
    
    // Custom mapping for active children only
    @Named("mapActiveChildren")
    default List<CategoryDto> mapActiveChildren(List<Category> children) {
        if (children == null || children.isEmpty()) {
            return null;
        }
        return children.stream()
                .filter(child -> child.getIsActive() != null && child.getIsActive())
                .map(this::toBasicDto)
                .toList();
    }
    
    // Utility method for hierarchy display
    @Mapping(source = "parent.id", target = "parentId")
    @Mapping(source = "parent.name", target = "parentName")
    @Mapping(target = "children", ignore = true)
    @Named("toDtoWithHierarchy")
    CategoryDto toDtoWithHierarchy(Category category);
    
    // Utility method for mapping parent ID to Category entity
    @Named("parentIdToParent")
    default Category mapParentIdToParent(Long parentId) {
        if (parentId == null) return null;
        Category parent = new Category();
        parent.setId(parentId);
        return parent;
    }
    
    // Utility method for mapping Category entity to parent ID
    @Named("parentToParentId")
    default Long mapParentToParentId(Category parent) {
        return parent != null ? parent.getId() : null;
    }
}