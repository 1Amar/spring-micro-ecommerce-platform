package com.amar.mapper;

import com.amar.dto.UserProfileDto;
import com.amar.entity.UserProfile;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface UserProfileMapperMS {
    
    @Mapping(target = "fullName", ignore = true)
    @Mapping(target = "initials", ignore = true)
    @Mapping(source = "phone", target = "phoneNumber")
    UserProfileDto toDto(UserProfile profile);
    
    @Mapping(target = "user", ignore = true)
    @Mapping(source = "phoneNumber", target = "phone")
    UserProfile toEntity(UserProfileDto dto);
    
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(source = "phoneNumber", target = "phone")
    void updateEntityFromDto(UserProfileDto dto, @MappingTarget UserProfile entity);
    
    @AfterMapping
    default void setComputedFields(@MappingTarget UserProfileDto dto, UserProfile entity) {
        if (entity != null) {
            dto.setFullName(entity.getFullName());
            dto.setInitials(entity.getInitials());
        }
    }
}