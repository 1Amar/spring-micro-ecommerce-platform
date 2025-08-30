package com.amar.controller;

import com.amar.dto.UserDto;
import com.amar.dto.UserProfileDto;
import com.amar.dto.UserAddressDto;
import com.amar.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
// CORS handled by API Gateway - removed @CrossOrigin annotation
public class UserController {

    private final UserService userService;

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        log.info("User Service health check");
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "user-service",
            "port", "8082",
            "timestamp", LocalDateTime.now().toString()
        ));
    }

    /**
     * Create a new user
     */
    @PostMapping
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody UserDto userDto) {
        log.info("Creating user with email: {}", userDto.getEmail());
        UserDto createdUser = userService.createUser(userDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    /**
     * Get user by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long id) {
        log.info("Fetching user with ID: {}", id);
        Optional<UserDto> user = userService.getUserById(id);
        return user.map(ResponseEntity::ok)
                  .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get user by Keycloak ID
     */
    @GetMapping("/keycloak/{keycloakId}")
    public ResponseEntity<UserDto> getUserByKeycloakId(@PathVariable String keycloakId) {
        log.info("Fetching user with Keycloak ID: {}", keycloakId);
        Optional<UserDto> user = userService.getUserByKeycloakId(keycloakId);
        return user.map(ResponseEntity::ok)
                  .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get user by email
     */
    @GetMapping("/email/{email}")
    public ResponseEntity<UserDto> getUserByEmail(@PathVariable String email) {
        log.info("Fetching user with email: {}", email);
        Optional<UserDto> user = userService.getUserByEmail(email);
        return user.map(ResponseEntity::ok)
                  .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get user by username
     */
    @GetMapping("/username/{username}")
    public ResponseEntity<UserDto> getUserByUsername(@PathVariable String username) {
        log.info("Fetching user with username: {}", username);
        Optional<UserDto> user = userService.getUserByUsername(username);
        return user.map(ResponseEntity::ok)
                  .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update user
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserDto> updateUser(@PathVariable Long id, @Valid @RequestBody UserDto userDto) {
        log.info("Updating user with ID: {}", id);
        UserDto updatedUser = userService.updateUser(id, userDto);
        return ResponseEntity.ok(updatedUser);
    }

    /**
     * Deactivate user
     */
    @PutMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateUser(@PathVariable Long id) {
        log.info("Deactivating user with ID: {}", id);
        userService.deactivateUser(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Activate user
     */
    @PutMapping("/{id}/activate")
    public ResponseEntity<Void> activateUser(@PathVariable Long id) {
        log.info("Activating user with ID: {}", id);
        userService.activateUser(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Delete user
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        log.info("Deleting user with ID: {}", id);
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get all users with pagination
     */
    @GetMapping
    public ResponseEntity<Page<UserDto>> getAllUsers(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Fetching all users with pagination: {}", pageable);
        Page<UserDto> users = userService.getAllUsers(pageable);
        return ResponseEntity.ok(users);
    }

    /**
     * Get active users with pagination
     */
    @GetMapping("/active")
    public ResponseEntity<Page<UserDto>> getActiveUsers(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Fetching active users with pagination: {}", pageable);
        Page<UserDto> users = userService.getActiveUsers(pageable);
        return ResponseEntity.ok(users);
    }

    /**
     * Get users by status
     */
    @GetMapping("/status")
    public ResponseEntity<Page<UserDto>> getUsersByStatus(
            @RequestParam Boolean isActive,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Fetching users by status: {} with pagination: {}", isActive, pageable);
        Page<UserDto> users = userService.getUsersByStatus(isActive, pageable);
        return ResponseEntity.ok(users);
    }

    /**
     * Search users
     */
    @GetMapping("/search")
    public ResponseEntity<Page<UserDto>> searchUsers(
            @RequestParam String q,
            @RequestParam(defaultValue = "false") Boolean activeOnly,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Searching users with query: {} (activeOnly: {}) and pagination: {}", q, activeOnly, pageable);
        Page<UserDto> users = activeOnly ? 
            userService.searchActiveUsers(q, pageable) : 
            userService.searchUsers(q, pageable);
        return ResponseEntity.ok(users);
    }

    /**
     * Get user statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getUserStats() {
        log.info("Fetching user statistics");
        Map<String, Object> stats = userService.getUserStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * Check if email exists
     */
    @GetMapping("/exists/email")
    public ResponseEntity<Map<String, Boolean>> checkEmailExists(@RequestParam String email) {
        log.debug("Checking if email exists: {}", email);
        boolean exists = userService.existsByEmail(email);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    /**
     * Check if username exists
     */
    @GetMapping("/exists/username")
    public ResponseEntity<Map<String, Boolean>> checkUsernameExists(@RequestParam String username) {
        log.debug("Checking if username exists: {}", username);
        boolean exists = userService.existsByUsername(username);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    /**
     * Check if Keycloak ID exists
     */
    @GetMapping("/exists/keycloak")
    public ResponseEntity<Map<String, Boolean>> checkKeycloakIdExists(@RequestParam String keycloakId) {
        log.debug("Checking if Keycloak ID exists: {}", keycloakId);
        boolean exists = userService.existsByKeycloakId(keycloakId);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    // Profile Management Endpoints
    @PostMapping("/{userId}/profile")
    public ResponseEntity<UserDto> createUserProfile(@PathVariable Long userId, @Valid @RequestBody UserProfileDto profileDto) {
        log.debug("Creating profile for user ID: {}", userId);
        UserDto user = userService.createUserProfile(userId, profileDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @PutMapping("/{userId}/profile")
    public ResponseEntity<UserDto> updateUserProfile(@PathVariable Long userId, @Valid @RequestBody UserProfileDto profileDto) {
        log.debug("Updating profile for user ID: {}", userId);
        UserDto user = userService.updateUserProfile(userId, profileDto);
        return ResponseEntity.ok(user);
    }

    // Address Management Endpoints
    @PostMapping("/{userId}/addresses")
    public ResponseEntity<UserDto> addUserAddress(@PathVariable Long userId, @Valid @RequestBody UserAddressDto addressDto) {
        log.debug("Adding address for user ID: {}", userId);
        UserDto user = userService.addUserAddress(userId, addressDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @PutMapping("/{userId}/addresses/{addressId}")
    public ResponseEntity<UserDto> updateUserAddress(@PathVariable Long userId, @PathVariable Long addressId, @Valid @RequestBody UserAddressDto addressDto) {
        log.debug("Updating address ID: {} for user ID: {}", addressId, userId);
        UserDto user = userService.updateUserAddress(userId, addressId, addressDto);
        return ResponseEntity.ok(user);
    }

    @DeleteMapping("/{userId}/addresses/{addressId}")
    public ResponseEntity<UserDto> deleteUserAddress(@PathVariable Long userId, @PathVariable Long addressId) {
        log.debug("Deleting address ID: {} for user ID: {}", addressId, userId);
        UserDto user = userService.deleteUserAddress(userId, addressId);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/{userId}/addresses/{addressId}/default")
    public ResponseEntity<UserDto> setDefaultAddress(@PathVariable Long userId, @PathVariable Long addressId) {
        log.debug("Setting default address ID: {} for user ID: {}", addressId, userId);
        UserDto user = userService.setDefaultAddress(userId, addressId);
        return ResponseEntity.ok(user);
    }
}