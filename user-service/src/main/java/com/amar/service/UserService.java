package com.amar.service;

import com.amar.dto.UserDto;
import com.amar.dto.UserProfileDto;
import com.amar.dto.UserAddressDto;
import com.amar.entity.user.User;
import com.amar.entity.user.UserProfile;
import com.amar.entity.user.UserAddress;
import com.amar.mapper.UserMapperMS;
import com.amar.mapper.UserProfileMapperMS;
import com.amar.mapper.UserAddressMapperMS;
import com.amar.repository.UserRepository;
import com.amar.repository.UserProfileRepository;
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
import java.util.Map;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserAddressRepository userAddressRepository;
    private final UserMapperMS userMapper;
    private final UserProfileMapperMS userProfileMapper;
    private final UserAddressMapperMS userAddressMapper;

    // User CRUD Operations
    public UserDto createUser(UserDto userDto) {
        log.info("Creating new user with email: {}", userDto.getEmail());
        
        // Validate unique constraints
        if (userRepository.existsByEmail(userDto.getEmail())) {
            throw new com.amar.exception.UserServiceExceptionHandler.DuplicateResourceException("User with email already exists: " + userDto.getEmail());
        }
        if (userRepository.existsByUsername(userDto.getUsername())) {
            throw new com.amar.exception.UserServiceExceptionHandler.DuplicateResourceException("User with username already exists: " + userDto.getUsername());
        }
        if (userDto.getKeycloakId() != null && userRepository.existsByKeycloakId(userDto.getKeycloakId())) {
            throw new com.amar.exception.UserServiceExceptionHandler.DuplicateResourceException("User with Keycloak ID already exists: " + userDto.getKeycloakId());
        }
        
        User user = userMapper.toEntity(userDto);
        user.setIsActive(true);
        User savedUser = userRepository.save(user);
        
        log.info("Successfully created user with ID: {}", savedUser.getId());
        return userMapper.toDto(savedUser);
    }

    @Transactional(readOnly = true)
    public Optional<UserDto> getUserById(Long id) {
        log.debug("Fetching user by ID: {}", id);
        return userRepository.findByIdWithProfileAndAddresses(id)
                .map(userMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<UserDto> getUserByKeycloakId(String keycloakId) {
        log.debug("Fetching user by Keycloak ID: {}", keycloakId);
        return userRepository.findByKeycloakIdWithProfileAndAddresses(keycloakId)
                .map(userMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<UserDto> getUserByEmail(String email) {
        log.debug("Fetching user by email: {}", email);
        return userRepository.findByEmail(email)
                .map(userMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<UserDto> getUserByUsername(String username) {
        log.debug("Fetching user by username: {}", username);
        return userRepository.findByUsername(username)
                .map(userMapper::toDto);
    }

    public UserDto updateUser(Long id, UserDto userDto) {
        log.info("Updating user with ID: {}", id);
        
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new com.amar.exception.UserServiceExceptionHandler.UserNotFoundException("User not found with ID: " + id));

        // Validate unique constraints for updates
        if (!existingUser.getEmail().equals(userDto.getEmail()) && 
            userRepository.existsByEmail(userDto.getEmail())) {
            throw new com.amar.exception.UserServiceExceptionHandler.DuplicateResourceException("User with email already exists: " + userDto.getEmail());
        }
        if (!existingUser.getUsername().equals(userDto.getUsername()) && 
            userRepository.existsByUsername(userDto.getUsername())) {
            throw new com.amar.exception.UserServiceExceptionHandler.DuplicateResourceException("User with username already exists: " + userDto.getUsername());
        }

        userMapper.updateEntityFromDto(userDto, existingUser);
        User updatedUser = userRepository.save(existingUser);
        
        log.info("Successfully updated user with ID: {}", updatedUser.getId());
        return userMapper.toDto(updatedUser);
    }

    public void deactivateUser(Long id) {
        log.info("Deactivating user with ID: {}", id);
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + id));
        
        user.setIsActive(false);
        userRepository.save(user);
        
        log.info("Successfully deactivated user with ID: {}", id);
    }

    public void activateUser(Long id) {
        log.info("Activating user with ID: {}", id);
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + id));
        
        user.setIsActive(true);
        userRepository.save(user);
        
        log.info("Successfully activated user with ID: {}", id);
    }

    public void deleteUser(Long id) {
        log.info("Deleting user with ID: {}", id);
        
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found with ID: " + id);
        }
        
        userRepository.deleteById(id);
        log.info("Successfully deleted user with ID: {}", id);
    }

    // User Search and Pagination
    @Transactional(readOnly = true)
    public Page<UserDto> getAllUsers(Pageable pageable) {
        log.debug("Fetching all users with pagination: {}", pageable);
        Page<User> userPage = userRepository.findAll(pageable);
        List<UserDto> userDtos = userMapper.toDtoList(userPage.getContent());
        return new PageImpl<>(userDtos, pageable, userPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Page<UserDto> getActiveUsers(Pageable pageable) {
        log.debug("Fetching active users with pagination: {}", pageable);
        Page<User> userPage = userRepository.findAllActiveUsers(pageable);
        List<UserDto> userDtos = userMapper.toDtoList(userPage.getContent());
        return new PageImpl<>(userDtos, pageable, userPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Page<UserDto> getUsersByStatus(Boolean isActive, Pageable pageable) {
        log.debug("Fetching users by status: {} with pagination: {}", isActive, pageable);
        Page<User> userPage = userRepository.findByIsActive(isActive, pageable);
        List<UserDto> userDtos = userMapper.toDtoList(userPage.getContent());
        return new PageImpl<>(userDtos, pageable, userPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Page<UserDto> searchUsers(String searchTerm, Pageable pageable) {
        log.debug("Searching users with term: {} and pagination: {}", searchTerm, pageable);
        Page<User> userPage = userRepository.findBySearchTerm(searchTerm, pageable);
        List<UserDto> userDtos = userMapper.toDtoList(userPage.getContent());
        return new PageImpl<>(userDtos, pageable, userPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Page<UserDto> searchActiveUsers(String searchTerm, Pageable pageable) {
        log.debug("Searching active users with term: {} and pagination: {}", searchTerm, pageable);
        Page<User> userPage = userRepository.findByIsActiveAndSearchTerm(true, searchTerm, pageable);
        List<UserDto> userDtos = userMapper.toDtoList(userPage.getContent());
        return new PageImpl<>(userDtos, pageable, userPage.getTotalElements());
    }

    // User Statistics
    @Transactional(readOnly = true)
    public Map<String, Object> getUserStats() {
        log.debug("Fetching user statistics");
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("activeUsers", userRepository.countActiveUsers());
        stats.put("inactiveUsers", userRepository.countInactiveUsers());
        stats.put("usersWithProfiles", userProfileRepository.countCompleteProfiles());
        stats.put("usersWithoutProfiles", userProfileRepository.countIncompleteProfiles());
        
        return stats;
    }

    // User Profile Management
    public UserDto createUserProfile(Long userId, UserProfileDto profileDto) {
        log.info("Creating profile for user ID: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
        
        if (userProfileRepository.existsByUserId(userId)) {
            throw new RuntimeException("Profile already exists for user ID: " + userId);
        }
        
        UserProfile profile = userProfileMapper.toEntity(profileDto);
        profile.setUser(user);
        UserProfile savedProfile = userProfileRepository.save(profile);
        
        // Fetch updated user with profile
        User updatedUser = userRepository.findByIdWithProfileAndAddresses(userId)
                .orElseThrow(() -> new RuntimeException("User not found after profile creation"));
        
        log.info("Successfully created profile for user ID: {}", userId);
        return userMapper.toDto(updatedUser);
    }

    public UserDto updateUserProfile(Long userId, UserProfileDto profileDto) {
        log.info("Updating profile for user ID: {}", userId);
        
        UserProfile existingProfile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Profile not found for user ID: " + userId));
        
        userProfileMapper.updateEntityFromDto(profileDto, existingProfile);
        userProfileRepository.save(existingProfile);
        
        // Fetch updated user with profile
        User updatedUser = userRepository.findByIdWithProfileAndAddresses(userId)
                .orElseThrow(() -> new RuntimeException("User not found after profile update"));
        
        log.info("Successfully updated profile for user ID: {}", userId);
        return userMapper.toDto(updatedUser);
    }

    // User Address Management  
    public UserDto addUserAddress(Long userId, UserAddressDto addressDto) {
        log.info("Adding address for user ID: {}", userId);
        
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
        
        // Fetch updated user with addresses
        User updatedUser = userRepository.findByIdWithProfileAndAddresses(userId)
                .orElseThrow(() -> new RuntimeException("User not found after address creation"));
        
        log.info("Successfully added address for user ID: {}", userId);
        return userMapper.toDto(updatedUser);
    }

    public UserDto updateUserAddress(Long userId, Long addressId, UserAddressDto addressDto) {
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
        userAddressRepository.save(existingAddress);
        
        // Fetch updated user with addresses
        User updatedUser = userRepository.findByIdWithProfileAndAddresses(userId)
                .orElseThrow(() -> new RuntimeException("User not found after address update"));
        
        log.info("Successfully updated address ID: {} for user ID: {}", addressId, userId);
        return userMapper.toDto(updatedUser);
    }

    public UserDto deleteUserAddress(Long userId, Long addressId) {
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
            }
        }
        
        // Fetch updated user with addresses
        User updatedUser = userRepository.findByIdWithProfileAndAddresses(userId)
                .orElseThrow(() -> new RuntimeException("User not found after address deletion"));
        
        log.info("Successfully deleted address ID: {} for user ID: {}", addressId, userId);
        return userMapper.toDto(updatedUser);
    }

    public UserDto setDefaultAddress(Long userId, Long addressId) {
        log.info("Setting default address ID: {} for user ID: {}", addressId, userId);
        
        UserAddress address = userAddressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found with ID: " + addressId));
        
        if (!address.getUser().getId().equals(userId)) {
            throw new RuntimeException("Address does not belong to user ID: " + userId);
        }
        
        userAddressRepository.clearDefaultAddressesExcept(userId, addressId);
        address.setIsDefault(true);
        userAddressRepository.save(address);
        
        // Fetch updated user with addresses
        User updatedUser = userRepository.findByIdWithProfileAndAddresses(userId)
                .orElseThrow(() -> new RuntimeException("User not found after setting default address"));
        
        log.info("Successfully set default address ID: {} for user ID: {}", addressId, userId);
        return userMapper.toDto(updatedUser);
    }

    // Validation Methods
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Transactional(readOnly = true)
    public boolean existsByKeycloakId(String keycloakId) {
        return userRepository.existsByKeycloakId(keycloakId);
    }
}