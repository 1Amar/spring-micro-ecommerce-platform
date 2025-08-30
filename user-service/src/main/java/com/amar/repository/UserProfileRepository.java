package com.amar.repository;

import com.amar.entity.UserProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    
    Optional<UserProfile> findByUserId(Long userId);
    
    Optional<UserProfile> findByUserKeycloakId(String keycloakId);
    
    boolean existsByUserId(Long userId);
    
    @Query("SELECT p FROM UserProfile p WHERE " +
           "LOWER(p.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(CONCAT(p.firstName, ' ', p.lastName)) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<UserProfile> findByNameContaining(@Param("search") String search, Pageable pageable);
    
    @Query("SELECT p FROM UserProfile p WHERE p.phone = :phone")
    List<UserProfile> findByPhone(@Param("phone") String phone);
    
    @Query("SELECT p FROM UserProfile p WHERE p.gender = :gender")
    Page<UserProfile> findByGender(@Param("gender") String gender, Pageable pageable);
    
    @Query("SELECT p FROM UserProfile p WHERE p.firstName IS NOT NULL AND p.lastName IS NOT NULL")
    Page<UserProfile> findCompleteProfiles(Pageable pageable);
    
    @Query("SELECT p FROM UserProfile p WHERE p.firstName IS NULL OR p.lastName IS NULL OR p.phone IS NULL")
    Page<UserProfile> findIncompleteProfiles(Pageable pageable);
    
    @Query("SELECT COUNT(p) FROM UserProfile p WHERE p.firstName IS NOT NULL AND p.lastName IS NOT NULL")
    Long countCompleteProfiles();
    
    @Query("SELECT COUNT(p) FROM UserProfile p WHERE p.firstName IS NULL OR p.lastName IS NULL")
    Long countIncompleteProfiles();
    
    @Query("SELECT p FROM UserProfile p JOIN FETCH p.user WHERE p.id = :id")
    Optional<UserProfile> findByIdWithUser(@Param("id") Long id);
    
    @Query("SELECT p FROM UserProfile p WHERE p.user.isActive = true")
    Page<UserProfile> findByActiveUsers(Pageable pageable);
}