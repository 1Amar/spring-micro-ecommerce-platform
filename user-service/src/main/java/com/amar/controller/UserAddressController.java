package com.amar.controller;

import com.amar.dto.UserAddressDto;
import com.amar.entity.user.UserAddress;
import com.amar.service.UserAddressService;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/addresses")
@RequiredArgsConstructor
@Slf4j
// CORS handled by API Gateway - removed @CrossOrigin annotation
public class UserAddressController {

    private final UserAddressService userAddressService;

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        log.info("User Address Service health check");
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "user-address-service",
            "timestamp", LocalDateTime.now().toString()
        ));
    }

    /**
     * Create address for a user
     */
    @PostMapping("/user/{userId}")
    public ResponseEntity<UserAddressDto> createAddress(
            @PathVariable Long userId, 
            @Valid @RequestBody UserAddressDto addressDto) {
        log.info("Creating address for user ID: {}", userId);
        UserAddressDto createdAddress = userAddressService.createAddress(userId, addressDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdAddress);
    }

    /**
     * Get address by ID
     */
    @GetMapping("/{addressId}")
    public ResponseEntity<UserAddressDto> getAddressById(@PathVariable Long addressId) {
        log.info("Fetching address with ID: {}", addressId);
        Optional<UserAddressDto> address = userAddressService.getAddressById(addressId);
        return address.map(ResponseEntity::ok)
                     .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all addresses for a user
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<UserAddressDto>> getAddressesByUserId(@PathVariable Long userId) {
        log.info("Fetching addresses for user ID: {}", userId);
        List<UserAddressDto> addresses = userAddressService.getAddressesByUserId(userId);
        return ResponseEntity.ok(addresses);
    }

    /**
     * Get addresses for a user with pagination
     */
    @GetMapping("/user/{userId}/paginated")
    public ResponseEntity<Page<UserAddressDto>> getAddressesByUserId(
            @PathVariable Long userId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Fetching addresses for user ID: {} with pagination: {}", userId, pageable);
        Page<UserAddressDto> addresses = userAddressService.getAddressesByUserId(userId, pageable);
        return ResponseEntity.ok(addresses);
    }

    /**
     * Get addresses by user Keycloak ID
     */
    @GetMapping("/user/keycloak/{keycloakId}")
    public ResponseEntity<List<UserAddressDto>> getAddressesByUserKeycloakId(@PathVariable String keycloakId) {
        log.info("Fetching addresses for user Keycloak ID: {}", keycloakId);
        List<UserAddressDto> addresses = userAddressService.getAddressesByUserKeycloakId(keycloakId);
        return ResponseEntity.ok(addresses);
    }

    /**
     * Get default address for a user
     */
    @GetMapping("/user/{userId}/default")
    public ResponseEntity<UserAddressDto> getDefaultAddress(@PathVariable Long userId) {
        log.info("Fetching default address for user ID: {}", userId);
        Optional<UserAddressDto> address = userAddressService.getDefaultAddress(userId);
        return address.map(ResponseEntity::ok)
                     .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get default address by user Keycloak ID
     */
    @GetMapping("/user/keycloak/{keycloakId}/default")
    public ResponseEntity<UserAddressDto> getDefaultAddressByKeycloakId(@PathVariable String keycloakId) {
        log.info("Fetching default address for user Keycloak ID: {}", keycloakId);
        Optional<UserAddressDto> address = userAddressService.getDefaultAddressByKeycloakId(keycloakId);
        return address.map(ResponseEntity::ok)
                     .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update address
     */
    @PutMapping("/{addressId}/user/{userId}")
    public ResponseEntity<UserAddressDto> updateAddress(
            @PathVariable Long userId,
            @PathVariable Long addressId, 
            @Valid @RequestBody UserAddressDto addressDto) {
        log.info("Updating address ID: {} for user ID: {}", addressId, userId);
        UserAddressDto updatedAddress = userAddressService.updateAddress(userId, addressId, addressDto);
        return ResponseEntity.ok(updatedAddress);
    }

    /**
     * Delete address
     */
    @DeleteMapping("/{addressId}/user/{userId}")
    public ResponseEntity<Void> deleteAddress(
            @PathVariable Long userId,
            @PathVariable Long addressId) {
        log.info("Deleting address ID: {} for user ID: {}", addressId, userId);
        userAddressService.deleteAddress(userId, addressId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Set default address
     */
    @PutMapping("/{addressId}/user/{userId}/set-default")
    public ResponseEntity<UserAddressDto> setDefaultAddress(
            @PathVariable Long userId,
            @PathVariable Long addressId) {
        log.info("Setting default address ID: {} for user ID: {}", addressId, userId);
        UserAddressDto updatedAddress = userAddressService.setDefaultAddress(userId, addressId);
        return ResponseEntity.ok(updatedAddress);
    }

    /**
     * Get addresses by type for a user
     */
    @GetMapping("/user/{userId}/type/{type}")
    public ResponseEntity<List<UserAddressDto>> getAddressesByType(
            @PathVariable Long userId,
            @PathVariable UserAddress.AddressType type) {
        log.info("Fetching addresses by type: {} for user ID: {}", type, userId);
        List<UserAddressDto> addresses = userAddressService.getAddressesByType(userId, type);
        return ResponseEntity.ok(addresses);
    }

    /**
     * Get addresses by type for a user with pagination
     */
    @GetMapping("/user/{userId}/type/{type}/paginated")
    public ResponseEntity<Page<UserAddressDto>> getAddressesByType(
            @PathVariable Long userId,
            @PathVariable UserAddress.AddressType type,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Fetching addresses by type: {} for user ID: {} with pagination: {}", type, userId, pageable);
        Page<UserAddressDto> addresses = userAddressService.getAddressesByType(userId, type, pageable);
        return ResponseEntity.ok(addresses);
    }

    /**
     * Search addresses
     */
    @GetMapping("/search")
    public ResponseEntity<Page<UserAddressDto>> searchAddresses(
            @RequestParam String q,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Searching addresses with query: {} and pagination: {}", q, pageable);
        Page<UserAddressDto> addresses = userAddressService.searchAddresses(q, pageable);
        return ResponseEntity.ok(addresses);
    }

    /**
     * Search addresses for a specific user
     */
    @GetMapping("/user/{userId}/search")
    public ResponseEntity<Page<UserAddressDto>> searchUserAddresses(
            @PathVariable Long userId,
            @RequestParam String q,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Searching addresses for user ID: {} with query: {} and pagination: {}", userId, q, pageable);
        Page<UserAddressDto> addresses = userAddressService.searchUserAddresses(userId, q, pageable);
        return ResponseEntity.ok(addresses);
    }

    /**
     * Get addresses by city
     */
    @GetMapping("/city/{city}")
    public ResponseEntity<Page<UserAddressDto>> getAddressesByCity(
            @PathVariable String city,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Fetching addresses by city: {} with pagination: {}", city, pageable);
        Page<UserAddressDto> addresses = userAddressService.getAddressesByCity(city, pageable);
        return ResponseEntity.ok(addresses);
    }

    /**
     * Get addresses by state
     */
    @GetMapping("/state/{state}")
    public ResponseEntity<Page<UserAddressDto>> getAddressesByState(
            @PathVariable String state,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Fetching addresses by state: {} with pagination: {}", state, pageable);
        Page<UserAddressDto> addresses = userAddressService.getAddressesByState(state, pageable);
        return ResponseEntity.ok(addresses);
    }

    /**
     * Get addresses by country
     */
    @GetMapping("/country/{country}")
    public ResponseEntity<Page<UserAddressDto>> getAddressesByCountry(
            @PathVariable String country,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Fetching addresses by country: {} with pagination: {}", country, pageable);
        Page<UserAddressDto> addresses = userAddressService.getAddressesByCountry(country, pageable);
        return ResponseEntity.ok(addresses);
    }

    /**
     * Get addresses by ZIP code
     */
    @GetMapping("/zip/{zipCode}")
    public ResponseEntity<List<UserAddressDto>> getAddressesByZipCode(@PathVariable String zipCode) {
        log.info("Fetching addresses by ZIP code: {}", zipCode);
        List<UserAddressDto> addresses = userAddressService.getAddressesByZipCode(zipCode);
        return ResponseEntity.ok(addresses);
    }

    /**
     * Get address count for user
     */
    @GetMapping("/user/{userId}/count")
    public ResponseEntity<Map<String, Object>> getAddressCountForUser(@PathVariable Long userId) {
        log.debug("Getting address count for user ID: {}", userId);
        Long count = userAddressService.getAddressCountForUser(userId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * Get address count by type for user
     */
    @GetMapping("/user/{userId}/count/type/{type}")
    public ResponseEntity<Map<String, Object>> getAddressCountByType(
            @PathVariable Long userId,
            @PathVariable UserAddress.AddressType type) {
        log.debug("Getting address count by type: {} for user ID: {}", type, userId);
        Long count = userAddressService.getAddressCountByType(userId, type);
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * Check if user has default address
     */
    @GetMapping("/user/{userId}/has-default")
    public ResponseEntity<Map<String, Boolean>> hasDefaultAddress(@PathVariable Long userId) {
        log.debug("Checking if user ID: {} has default address", userId);
        boolean hasDefault = userAddressService.hasDefaultAddress(userId);
        return ResponseEntity.ok(Map.of("hasDefault", hasDefault));
    }

    /**
     * Get cities by state
     */
    @GetMapping("/cities/state/{state}")
    public ResponseEntity<List<String>> getCitiesByState(@PathVariable String state) {
        log.debug("Fetching cities for state: {}", state);
        List<String> cities = userAddressService.getCitiesByState(state);
        return ResponseEntity.ok(cities);
    }

    /**
     * Get states by country
     */
    @GetMapping("/states/country/{country}")
    public ResponseEntity<List<String>> getStatesByCountry(@PathVariable String country) {
        log.debug("Fetching states for country: {}", country);
        List<String> states = userAddressService.getStatesByCountry(country);
        return ResponseEntity.ok(states);
    }

    /**
     * Clear all default addresses for user (admin function)
     */
    @PutMapping("/user/{userId}/clear-defaults")
    public ResponseEntity<Void> clearAllDefaultAddresses(@PathVariable Long userId) {
        log.info("Clearing all default addresses for user ID: {}", userId);
        userAddressService.clearAllDefaultAddresses(userId);
        return ResponseEntity.ok().build();
    }

    /**
     * Get all addresses with pagination (admin function)
     */
    @GetMapping
    public ResponseEntity<Page<UserAddressDto>> getAllAddresses(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Fetching all addresses with pagination: {}", pageable);
        Page<UserAddressDto> addresses = userAddressService.getAllAddresses(pageable);
        return ResponseEntity.ok(addresses);
    }
}