package com.amar.controller;

import com.amar.dto.UserProfileDto;
import com.amar.service.UserProfileService;
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
@RequestMapping("/api/v1/profiles")
@RequiredArgsConstructor
@Slf4j
// CORS handled by API Gateway - removed @CrossOrigin annotation
public class UserProfileController {

    private final UserProfileService userProfileService;

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        log.info("User Profile Service health check");
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "user-profile-service",
            "timestamp", LocalDateTime.now().toString()
        ));
    }

    /**
     * Create profile for a user
     */
    @PostMapping("/user/{userId}")
    public ResponseEntity<UserProfileDto> createProfile(
            @PathVariable Long userId, 
            @Valid @RequestBody UserProfileDto profileDto) {
        log.info("Creating profile for user ID: {}", userId);
        UserProfileDto createdProfile = userProfileService.createProfile(userId, profileDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdProfile);
    }

    /**
     * Get profile by profile ID
     */
    @GetMapping("/{profileId}")
    public ResponseEntity<UserProfileDto> getProfileById(@PathVariable Long profileId) {
        log.info("Fetching profile with ID: {}", profileId);
        Optional<UserProfileDto> profile = userProfileService.getProfileById(profileId);
        return profile.map(ResponseEntity::ok)
                     .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get profile by user ID
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<UserProfileDto> getProfileByUserId(@PathVariable Long userId) {
        log.info("Fetching profile for user ID: {}", userId);
        Optional<UserProfileDto> profile = userProfileService.getProfileByUserId(userId);
        return profile.map(ResponseEntity::ok)
                     .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get profile by user Keycloak ID
     */
    @GetMapping("/user/keycloak/{keycloakId}")
    public ResponseEntity<UserProfileDto> getProfileByUserKeycloakId(@PathVariable String keycloakId) {
        log.info("Fetching profile for user Keycloak ID: {}", keycloakId);
        Optional<UserProfileDto> profile = userProfileService.getProfileByUserKeycloakId(keycloakId);
        return profile.map(ResponseEntity::ok)
                     .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update profile by user ID
     */
    @PutMapping("/user/{userId}")
    public ResponseEntity<UserProfileDto> updateProfile(
            @PathVariable Long userId, 
            @Valid @RequestBody UserProfileDto profileDto) {
        log.info("Updating profile for user ID: {}", userId);
        UserProfileDto updatedProfile = userProfileService.updateProfile(userId, profileDto);
        return ResponseEntity.ok(updatedProfile);
    }

    /**
     * Update profile by profile ID
     */
    @PutMapping("/{profileId}")
    public ResponseEntity<UserProfileDto> updateProfileById(
            @PathVariable Long profileId, 
            @Valid @RequestBody UserProfileDto profileDto) {
        log.info("Updating profile with ID: {}", profileId);
        UserProfileDto updatedProfile = userProfileService.updateProfileById(profileId, profileDto);
        return ResponseEntity.ok(updatedProfile);
    }

    /**
     * Delete profile by user ID
     */
    @DeleteMapping("/user/{userId}")
    public ResponseEntity<Void> deleteProfile(@PathVariable Long userId) {
        log.info("Deleting profile for user ID: {}", userId);
        userProfileService.deleteProfile(userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Delete profile by profile ID
     */
    @DeleteMapping("/{profileId}")
    public ResponseEntity<Void> deleteProfileById(@PathVariable Long profileId) {
        log.info("Deleting profile with ID: {}", profileId);
        userProfileService.deleteProfileById(profileId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get all profiles with pagination
     */
    @GetMapping
    public ResponseEntity<Page<UserProfileDto>> getAllProfiles(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Fetching all profiles with pagination: {}", pageable);
        Page<UserProfileDto> profiles = userProfileService.getAllProfiles(pageable);
        return ResponseEntity.ok(profiles);
    }

    /**
     * Get profiles for active users
     */
    @GetMapping("/active")
    public ResponseEntity<Page<UserProfileDto>> getActiveUserProfiles(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Fetching profiles for active users with pagination: {}", pageable);
        Page<UserProfileDto> profiles = userProfileService.getActiveUserProfiles(pageable);
        return ResponseEntity.ok(profiles);
    }

    /**
     * Search profiles by name
     */
    @GetMapping("/search")
    public ResponseEntity<Page<UserProfileDto>> searchProfilesByName(
            @RequestParam String q,
            @PageableDefault(size = 20, sort = "firstName", direction = Sort.Direction.ASC) Pageable pageable) {
        log.info("Searching profiles by name with query: {} and pagination: {}", q, pageable);
        Page<UserProfileDto> profiles = userProfileService.searchProfilesByName(q, pageable);
        return ResponseEntity.ok(profiles);
    }

    /**
     * Get profiles by gender
     */
    @GetMapping("/gender/{gender}")
    public ResponseEntity<Page<UserProfileDto>> getProfilesByGender(
            @PathVariable String gender,
            @PageableDefault(size = 20, sort = "firstName", direction = Sort.Direction.ASC) Pageable pageable) {
        log.info("Fetching profiles by gender: {} with pagination: {}", gender, pageable);
        Page<UserProfileDto> profiles = userProfileService.getProfilesByGender(gender, pageable);
        return ResponseEntity.ok(profiles);
    }

    /**
     * Get complete profiles (with first name and last name)
     */
    @GetMapping("/complete")
    public ResponseEntity<Page<UserProfileDto>> getCompleteProfiles(
            @PageableDefault(size = 20, sort = "firstName", direction = Sort.Direction.ASC) Pageable pageable) {
        log.info("Fetching complete profiles with pagination: {}", pageable);
        Page<UserProfileDto> profiles = userProfileService.getCompleteProfiles(pageable);
        return ResponseEntity.ok(profiles);
    }

    /**
     * Get incomplete profiles (missing first name or last name)
     */
    @GetMapping("/incomplete")
    public ResponseEntity<Page<UserProfileDto>> getIncompleteProfiles(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Fetching incomplete profiles with pagination: {}", pageable);
        Page<UserProfileDto> profiles = userProfileService.getIncompleteProfiles(pageable);
        return ResponseEntity.ok(profiles);
    }

    /**
     * Get profiles by phone number
     */
    @GetMapping("/phone/{phone}")
    public ResponseEntity<List<UserProfileDto>> getProfilesByPhone(@PathVariable String phone) {
        log.info("Fetching profiles by phone: {}", phone);
        List<UserProfileDto> profiles = userProfileService.getProfilesByPhone(phone);
        return ResponseEntity.ok(profiles);
    }

    /**
     * Check if profile exists for user
     */
    @GetMapping("/exists/user/{userId}")
    public ResponseEntity<Map<String, Boolean>> checkProfileExists(@PathVariable Long userId) {
        log.debug("Checking if profile exists for user ID: {}", userId);
        boolean exists = userProfileService.profileExistsForUser(userId);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    /**
     * Get profile statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getProfileStats() {
        log.info("Fetching profile statistics");
        Map<String, Object> stats = Map.of(
            "completeProfiles", userProfileService.getCompleteProfilesCount(),
            "incompleteProfiles", userProfileService.getIncompleteProfilesCount(),
            "totalProfiles", userProfileService.getCompleteProfilesCount() + userProfileService.getIncompleteProfilesCount()
        );
        return ResponseEntity.ok(stats);
    }
}