package com.amar.mapper;

import com.amar.dto.UserDto;
import com.amar.entity.user.User;
import com.amar.dto.UserAddressDto;

import org.mapstruct.*;

import java.util.List;

@Mapper(
    componentModel = "spring",
    uses = {UserProfileMapperMS.class, UserAddressMapperMS.class}
)
public interface UserMapperMS {
    
    @Mapping(target = "fullName", ignore = true)
    @Mapping(target = "defaultAddress", ignore = true)
    UserDto toDto(User user);

    @Mapping(target = "profile", ignore = true)
    @Mapping(target = "addresses", ignore = true)
    User toEntity(UserDto userDto);
    
    List<UserDto> toDtoList(List<User> users);
    
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "keycloakId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "profile", ignore = true)
    @Mapping(target = "addresses", ignore = true)
    void updateEntityFromDto(UserDto dto, @MappingTarget User entity);
    
    @AfterMapping
    default void setComputedFields(@MappingTarget UserDto dto, User entity) {
        if (entity != null) {
            dto.setFullName(entity.getFullName());
            dto.setDefaultAddress(mapDefaultAddress(entity));
        }
    }
    
    default UserAddressDto mapDefaultAddress(User user) {
        if (user == null || user.getDefaultAddress() == null) {
            return null;
        }
        UserAddressDto addressDto = new UserAddressDto();
        var address = user.getDefaultAddress();
        addressDto.setId(address.getId());
        addressDto.setType(address.getType());
        addressDto.setStreet(address.getStreet());
        addressDto.setCity(address.getCity());
        addressDto.setState(address.getState());
        addressDto.setZipCode(address.getZipCode());
        addressDto.setCountry(address.getCountry());
        addressDto.setIsDefault(address.getIsDefault());
        addressDto.setFormattedAddress(address.getFormattedAddress());
        addressDto.setShortAddress(address.getShortAddress());
        return addressDto;
    }
}