package com.amar.service;

import com.amar.dto.UserProfileDto;
import com.amar.entity.User;
import com.amar.entity.UserProfile;
import com.amar.mapper.UserProfileMapperMS;
import com.amar.repository.UserRepository;
import com.amar.repository.UserProfileRepository;

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
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final UserRepository userRepository;
    private final UserProfileMapperMS userProfileMapper;

    public UserProfileDto createProfile(Long userId, UserProfileDto profileDto) {
        log.info("Creating profile for user ID: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
        
        if (userProfileRepository.existsByUserId(userId)) {
            throw new RuntimeException("Profile already exists for user ID: " + userId);
        }
        
        UserProfile profile = userProfileMapper.toEntity(profileDto);
        profile.setUser(user);
        UserProfile savedProfile = userProfileRepository.save(profile);
        
        log.info("Successfully created profile for user ID: {}", userId);
        return userProfileMapper.toDto(savedProfile);
    }

    @Transactional(readOnly = true)
    public Optional<UserProfileDto> getProfileByUserId(Long userId) {
        log.debug("Fetching profile for user ID: {}", userId);
        return userProfileRepository.findByUserId(userId)
                .map(userProfileMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<UserProfileDto> getProfileByUserKeycloakId(String keycloakId) {
        log.debug("Fetching profile for user Keycloak ID: {}", keycloakId);
        return userProfileRepository.findByUserKeycloakId(keycloakId)
                .map(userProfileMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<UserProfileDto> getProfileById(Long profileId) {
        log.debug("Fetching profile by ID: {}", profileId);
        return userProfileRepository.findById(profileId)
                .map(userProfileMapper::toDto);
    }

    public UserProfileDto updateProfile(Long userId, UserProfileDto profileDto) {
        log.info("Updating profile for user ID: {}", userId);
        
        UserProfile existingProfile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Profile not found for user ID: " + userId));
        
        userProfileMapper.updateEntityFromDto(profileDto, existingProfile);
        UserProfile updatedProfile = userProfileRepository.save(existingProfile);
        
        log.info("Successfully updated profile for user ID: {}", userId);
        return userProfileMapper.toDto(updatedProfile);
    }

    public UserProfileDto updateProfileById(Long profileId, UserProfileDto profileDto) {
        log.info("Updating profile with ID: {}", profileId);
        
        UserProfile existingProfile = userProfileRepository.findById(profileId)
                .orElseThrow(() -> new RuntimeException("Profile not found with ID: " + profileId));
        
        userProfileMapper.updateEntityFromDto(profileDto, existingProfile);
        UserProfile updatedProfile = userProfileRepository.save(existingProfile);
        
        log.info("Successfully updated profile with ID: {}", profileId);
        return userProfileMapper.toDto(updatedProfile);
    }

    public void deleteProfile(Long userId) {
        log.info("Deleting profile for user ID: {}", userId);
        
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Profile not found for user ID: " + userId));
        
        userProfileRepository.delete(profile);
        log.info("Successfully deleted profile for user ID: {}", userId);
    }

    public void deleteProfileById(Long profileId) {
        log.info("Deleting profile with ID: {}", profileId);
        
        if (!userProfileRepository.existsById(profileId)) {
            throw new RuntimeException("Profile not found with ID: " + profileId);
        }
        
        userProfileRepository.deleteById(profileId);
        log.info("Successfully deleted profile with ID: {}", profileId);
    }

    // Search and Pagination
    @Transactional(readOnly = true)
    public Page<UserProfileDto> getAllProfiles(Pageable pageable) {
        log.debug("Fetching all profiles with pagination: {}", pageable);
        Page<UserProfile> profilePage = userProfileRepository.findAll(pageable);
        List<UserProfileDto> profileDtos = profilePage.getContent().stream()
                .map(userProfileMapper::toDto)
                .toList();
        return new PageImpl<>(profileDtos, pageable, profilePage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Page<UserProfileDto> getActiveUserProfiles(Pageable pageable) {
        log.debug("Fetching profiles for active users with pagination: {}", pageable);
        Page<UserProfile> profilePage = userProfileRepository.findByActiveUsers(pageable);
        List<UserProfileDto> profileDtos = profilePage.getContent().stream()
                .map(userProfileMapper::toDto)
                .toList();
        return new PageImpl<>(profileDtos, pageable, profilePage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Page<UserProfileDto> searchProfilesByName(String searchTerm, Pageable pageable) {
        log.debug("Searching profiles by name with term: {} and pagination: {}", searchTerm, pageable);
        Page<UserProfile> profilePage = userProfileRepository.findByNameContaining(searchTerm, pageable);
        List<UserProfileDto> profileDtos = profilePage.getContent().stream()
                .map(userProfileMapper::toDto)
                .toList();
        return new PageImpl<>(profileDtos, pageable, profilePage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Page<UserProfileDto> getProfilesByGender(String gender, Pageable pageable) {
        log.debug("Fetching profiles by gender: {} with pagination: {}", gender, pageable);
        Page<UserProfile> profilePage = userProfileRepository.findByGender(gender, pageable);
        List<UserProfileDto> profileDtos = profilePage.getContent().stream()
                .map(userProfileMapper::toDto)
                .toList();
        return new PageImpl<>(profileDtos, pageable, profilePage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Page<UserProfileDto> getCompleteProfiles(Pageable pageable) {
        log.debug("Fetching complete profiles with pagination: {}", pageable);
        Page<UserProfile> profilePage = userProfileRepository.findCompleteProfiles(pageable);
        List<UserProfileDto> profileDtos = profilePage.getContent().stream()
                .map(userProfileMapper::toDto)
                .toList();
        return new PageImpl<>(profileDtos, pageable, profilePage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Page<UserProfileDto> getIncompleteProfiles(Pageable pageable) {
        log.debug("Fetching incomplete profiles with pagination: {}", pageable);
        Page<UserProfile> profilePage = userProfileRepository.findIncompleteProfiles(pageable);
        List<UserProfileDto> profileDtos = profilePage.getContent().stream()
                .map(userProfileMapper::toDto)
                .toList();
        return new PageImpl<>(profileDtos, pageable, profilePage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public List<UserProfileDto> getProfilesByPhone(String phone) {
        log.debug("Fetching profiles by phone: {}", phone);
        List<UserProfile> profiles = userProfileRepository.findByPhone(phone);
        return profiles.stream()
                .map(userProfileMapper::toDto)
                .toList();
    }

    // Validation Methods
    @Transactional(readOnly = true)
    public boolean profileExistsForUser(Long userId) {
        return userProfileRepository.existsByUserId(userId);
    }

    @Transactional(readOnly = true)
    public Long getCompleteProfilesCount() {
        return userProfileRepository.countCompleteProfiles();
    }

    @Transactional(readOnly = true)
    public Long getIncompleteProfilesCount() {
        return userProfileRepository.countIncompleteProfiles();
    }
}