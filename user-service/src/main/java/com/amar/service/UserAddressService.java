package com.amar.service;

import com.amar.dto.UserAddressDto;
import com.amar.entity.User;
import com.amar.entity.UserAddress;
import com.amar.mapper.UserAddressMapperMS;
import com.amar.repository.UserRepository;
import com.amar.repository.UserAddressRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserAddressService {

    private final UserAddressRepository userAddressRepository;
    private final UserRepository userRepository;
    private final UserAddressMapperMS userAddressMapper;

    // Address CRUD Operations
    public UserAddressDto createAddress(Long userId, UserAddressDto addressDto) {
        log.info("Creating address for user ID: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
        
        UserAddress address = userAddressMapper.toEntity(addressDto);
        address.setUser(user);
        
        // If this is set as default or it's the first address, handle default logic
        if (Boolean.TRUE.equals(addressDto.getIsDefault()) || 
            userAddressRepository.countByUserId(userId) == 0) {
            userAddressRepository.clearAllDefaultAddresses(userId);
            address.setIsDefault(true);
        }
        
        UserAddress savedAddress = userAddressRepository.save(address);
        
        log.info("Successfully created address for user ID: {}", userId);
        return userAddressMapper.toDto(savedAddress);
    }

    @Transactional(readOnly = true)
    public Optional<UserAddressDto> getAddressById(Long addressId) {
        log.debug("Fetching address by ID: {}", addressId);
        return userAddressRepository.findById(addressId)
                .map(userAddressMapper::toDto);
    }

    @Transactional(readOnly = true)
    public List<UserAddressDto> getAddressesByUserId(Long userId) {
        log.debug("Fetching addresses for user ID: {}", userId);
        List<UserAddress> addresses = userAddressRepository.findByUserId(userId);
        return userAddressMapper.toDtoList(addresses);
    }

    @Transactional(readOnly = true)
    public Page<UserAddressDto> getAddressesByUserId(Long userId, Pageable pageable) {
        log.debug("Fetching addresses for user ID: {} with pagination: {}", userId, pageable);
        Page<UserAddress> addressPage = userAddressRepository.findByUserId(userId, pageable);
        List<UserAddressDto> addressDtos = userAddressMapper.toDtoList(addressPage.getContent());
        return new PageImpl<>(addressDtos, pageable, addressPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public List<UserAddressDto> getAddressesByUserKeycloakId(String keycloakId) {
        log.debug("Fetching addresses for user Keycloak ID: {}", keycloakId);
        List<UserAddress> addresses = userAddressRepository.findByUserKeycloakId(keycloakId);
        return userAddressMapper.toDtoList(addresses);
    }

    @Transactional(readOnly = true)
    public Optional<UserAddressDto> getDefaultAddress(Long userId) {
        log.debug("Fetching default address for user ID: {}", userId);
        return userAddressRepository.findByUserIdAndIsDefaultTrue(userId)
                .map(userAddressMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<UserAddressDto> getDefaultAddressByKeycloakId(String keycloakId) {
        log.debug("Fetching default address for user Keycloak ID: {}", keycloakId);
        return userAddressRepository.findByUserKeycloakIdAndIsDefaultTrue(keycloakId)
                .map(userAddressMapper::toDto);
    }

    public UserAddressDto updateAddress(Long userId, Long addressId, UserAddressDto addressDto) {
        log.info("Updating address ID: {} for user ID: {}", addressId, userId);
        
        UserAddress existingAddress = userAddressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found with ID: " + addressId));
        
        if (!existingAddress.getUser().getId().equals(userId)) {
            throw new RuntimeException("Address does not belong to user ID: " + userId);
        }
        
        // Handle default address logic
        if (Boolean.TRUE.equals(addressDto.getIsDefault()) && !existingAddress.getIsDefault()) {
            userAddressRepository.clearDefaultAddressesExcept(userId, addressId);
        }
        
        userAddressMapper.updateEntityFromDto(addressDto, existingAddress);
        UserAddress updatedAddress = userAddressRepository.save(existingAddress);
        
        log.info("Successfully updated address ID: {} for user ID: {}", addressId, userId);
        return userAddressMapper.toDto(updatedAddress);
    }

    public void deleteAddress(Long userId, Long addressId) {
        log.info("Deleting address ID: {} for user ID: {}", addressId, userId);
        
        UserAddress address = userAddressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found with ID: " + addressId));
        
        if (!address.getUser().getId().equals(userId)) {
            throw new RuntimeException("Address does not belong to user ID: " + userId);
        }
        
        boolean wasDefault = address.getIsDefault();
        userAddressRepository.delete(address);
        
        // If we deleted the default address, make another address default if available
        if (wasDefault) {
            List<UserAddress> remainingAddresses = userAddressRepository.findByUserId(userId);
            if (!remainingAddresses.isEmpty()) {
                UserAddress newDefault = remainingAddresses.get(0);
                newDefault.setIsDefault(true);
                userAddressRepository.save(newDefault);
                log.info("Set new default address ID: {} for user ID: {}", newDefault.getId(), userId);
            }
        }
        
        log.info("Successfully deleted address ID: {} for user ID: {}", addressId, userId);
    }

    public UserAddressDto setDefaultAddress(Long userId, Long addressId) {
        log.info("Setting default address ID: {} for user ID: {}", addressId, userId);
        
        UserAddress address = userAddressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found with ID: " + addressId));
        
        if (!address.getUser().getId().equals(userId)) {
            throw new RuntimeException("Address does not belong to user ID: " + userId);
        }
        
        userAddressRepository.clearDefaultAddressesExcept(userId, addressId);
        address.setIsDefault(true);
        UserAddress updatedAddress = userAddressRepository.save(address);
        
        log.info("Successfully set default address ID: {} for user ID: {}", addressId, userId);
        return userAddressMapper.toDto(updatedAddress);
    }

    // Address Search and Filtering
    @Transactional(readOnly = true)
    public List<UserAddressDto> getAddressesByType(Long userId, UserAddress.AddressType type) {
        log.debug("Fetching addresses by type: {} for user ID: {}", type, userId);
        List<UserAddress> addresses = userAddressRepository.findByUserIdAndType(userId, type);
        return userAddressMapper.toDtoList(addresses);
    }

    @Transactional(readOnly = true)
    public Page<UserAddressDto> getAddressesByType(Long userId, UserAddress.AddressType type, Pageable pageable) {
        log.debug("Fetching addresses by type: {} for user ID: {} with pagination: {}", type, userId, pageable);
        Page<UserAddress> addressPage = userAddressRepository.findByUserIdAndType(userId, type, pageable);
        List<UserAddressDto> addressDtos = userAddressMapper.toDtoList(addressPage.getContent());
        return new PageImpl<>(addressDtos, pageable, addressPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Page<UserAddressDto> searchAddresses(String searchTerm, Pageable pageable) {
        log.debug("Searching addresses with term: {} and pagination: {}", searchTerm, pageable);
        Page<UserAddress> addressPage = userAddressRepository.findByAddressContaining(searchTerm, pageable);
        List<UserAddressDto> addressDtos = userAddressMapper.toDtoList(addressPage.getContent());
        return new PageImpl<>(addressDtos, pageable, addressPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Page<UserAddressDto> searchUserAddresses(Long userId, String searchTerm, Pageable pageable) {
        log.debug("Searching addresses for user ID: {} with term: {} and pagination: {}", userId, searchTerm, pageable);
        Page<UserAddress> addressPage = userAddressRepository.findByUserIdAndAddressContaining(userId, searchTerm, pageable);
        List<UserAddressDto> addressDtos = userAddressMapper.toDtoList(addressPage.getContent());
        return new PageImpl<>(addressDtos, pageable, addressPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Page<UserAddressDto> getAddressesByCity(String city, Pageable pageable) {
        log.debug("Fetching addresses by city: {} with pagination: {}", city, pageable);
        Page<UserAddress> addressPage = userAddressRepository.findByCity(city, pageable);
        List<UserAddressDto> addressDtos = userAddressMapper.toDtoList(addressPage.getContent());
        return new PageImpl<>(addressDtos, pageable, addressPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Page<UserAddressDto> getAddressesByState(String state, Pageable pageable) {
        log.debug("Fetching addresses by state: {} with pagination: {}", state, pageable);
        Page<UserAddress> addressPage = userAddressRepository.findByState(state, pageable);
        List<UserAddressDto> addressDtos = userAddressMapper.toDtoList(addressPage.getContent());
        return new PageImpl<>(addressDtos, pageable, addressPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Page<UserAddressDto> getAddressesByCountry(String country, Pageable pageable) {
        log.debug("Fetching addresses by country: {} with pagination: {}", country, pageable);
        Page<UserAddress> addressPage = userAddressRepository.findByCountry(country, pageable);
        List<UserAddressDto> addressDtos = userAddressMapper.toDtoList(addressPage.getContent());
        return new PageImpl<>(addressDtos, pageable, addressPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public List<UserAddressDto> getAddressesByZipCode(String zipCode) {
        log.debug("Fetching addresses by ZIP code: {}", zipCode);
        List<UserAddress> addresses = userAddressRepository.findByZipCode(zipCode);
        return userAddressMapper.toDtoList(addresses);
    }

    // Statistics and Utilities
    @Transactional(readOnly = true)
    public Long getAddressCountForUser(Long userId) {
        log.debug("Getting address count for user ID: {}", userId);
        return userAddressRepository.countByUserId(userId);
    }

    @Transactional(readOnly = true)
    public Long getAddressCountByType(Long userId, UserAddress.AddressType type) {
        log.debug("Getting address count by type: {} for user ID: {}", type, userId);
        return userAddressRepository.countByUserIdAndType(userId, type);
    }

    @Transactional(readOnly = true)
    public boolean hasDefaultAddress(Long userId) {
        log.debug("Checking if user ID: {} has default address", userId);
        return userAddressRepository.existsByUserIdAndIsDefaultTrue(userId);
    }

    @Transactional(readOnly = true)
    public List<String> getCitiesByState(String state) {
        log.debug("Fetching cities for state: {}", state);
        return userAddressRepository.findDistinctCitiesByState(state);
    }

    @Transactional(readOnly = true)
    public List<String> getStatesByCountry(String country) {
        log.debug("Fetching states for country: {}", country);
        return userAddressRepository.findDistinctStatesByCountry(country);
    }

    // Batch Operations
    public void clearAllDefaultAddresses(Long userId) {
        log.info("Clearing all default addresses for user ID: {}", userId);
        userAddressRepository.clearAllDefaultAddresses(userId);
    }

    @Transactional(readOnly = true)
    public Page<UserAddressDto> getAllAddresses(Pageable pageable) {
        log.debug("Fetching all addresses with pagination: {}", pageable);
        Page<UserAddress> addressPage = userAddressRepository.findAll(pageable);
        List<UserAddressDto> addressDtos = userAddressMapper.toDtoList(addressPage.getContent());
        return new PageImpl<>(addressDtos, pageable, addressPage.getTotalElements());
    }
}