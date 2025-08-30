package com.amar.mapper;

import com.amar.dto.UserAddressDto;
import com.amar.entity.UserAddress;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserAddressMapperMS {
    
    @Mapping(target = "formattedAddress", ignore = true)
    @Mapping(target = "shortAddress", ignore = true)
    UserAddressDto toDto(UserAddress address);
    
    @Mapping(target = "user", ignore = true)
    UserAddress toEntity(UserAddressDto dto);
    
    List<UserAddressDto> toDtoList(List<UserAddress> addresses);
    
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDto(UserAddressDto dto, @MappingTarget UserAddress entity);
    
    @AfterMapping
    default void setComputedFields(@MappingTarget UserAddressDto dto, UserAddress entity) {
        if (entity != null) {
            dto.setFormattedAddress(entity.getFormattedAddress());
            dto.setShortAddress(entity.getShortAddress());
        }
    }
}