package com.amar.repository;

import com.amar.entity.UserAddress;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.List;

@Repository
public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {
    
    List<UserAddress> findByUserId(Long userId);
    
    Page<UserAddress> findByUserId(Long userId, Pageable pageable);
    
    List<UserAddress> findByUserKeycloakId(String keycloakId);
    
    Optional<UserAddress> findByUserIdAndIsDefaultTrue(Long userId);
    
    Optional<UserAddress> findByUserKeycloakIdAndIsDefaultTrue(String keycloakId);
    
    List<UserAddress> findByUserIdAndType(Long userId, UserAddress.AddressType type);
    
    @Query("SELECT a FROM UserAddress a WHERE a.user.id = :userId AND a.type = :type")
    Page<UserAddress> findByUserIdAndType(@Param("userId") Long userId, 
                                          @Param("type") UserAddress.AddressType type, 
                                          Pageable pageable);
    
    @Query("SELECT a FROM UserAddress a WHERE " +
           "LOWER(a.street) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(a.city) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(a.state) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(a.zipCode) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<UserAddress> findByAddressContaining(@Param("search") String search, Pageable pageable);
    
    @Query("SELECT a FROM UserAddress a WHERE a.user.id = :userId AND " +
           "(LOWER(a.street) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(a.city) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(a.state) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(a.zipCode) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<UserAddress> findByUserIdAndAddressContaining(@Param("userId") Long userId, 
                                                       @Param("search") String search, 
                                                       Pageable pageable);
    
    @Query("SELECT a FROM UserAddress a WHERE a.city = :city")
    Page<UserAddress> findByCity(@Param("city") String city, Pageable pageable);
    
    @Query("SELECT a FROM UserAddress a WHERE a.state = :state")
    Page<UserAddress> findByState(@Param("state") String state, Pageable pageable);
    
    @Query("SELECT a FROM UserAddress a WHERE a.country = :country")
    Page<UserAddress> findByCountry(@Param("country") String country, Pageable pageable);
    
    @Query("SELECT a FROM UserAddress a WHERE a.zipCode = :zipCode")
    List<UserAddress> findByZipCode(@Param("zipCode") String zipCode);
    
    @Query("SELECT COUNT(a) FROM UserAddress a WHERE a.user.id = :userId")
    Long countByUserId(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(a) FROM UserAddress a WHERE a.user.id = :userId AND a.type = :type")
    Long countByUserIdAndType(@Param("userId") Long userId, @Param("type") UserAddress.AddressType type);
    
    boolean existsByUserIdAndIsDefaultTrue(Long userId);
    
    @Modifying
    @Transactional
    @Query("UPDATE UserAddress a SET a.isDefault = false WHERE a.user.id = :userId AND a.id != :excludeId")
    void clearDefaultAddressesExcept(@Param("userId") Long userId, @Param("excludeId") Long excludeId);
    
    @Modifying
    @Transactional
    @Query("UPDATE UserAddress a SET a.isDefault = false WHERE a.user.id = :userId")
    void clearAllDefaultAddresses(@Param("userId") Long userId);
    
    @Query("SELECT a FROM UserAddress a JOIN FETCH a.user WHERE a.id = :id")
    Optional<UserAddress> findByIdWithUser(@Param("id") Long id);
    
    @Query("SELECT DISTINCT a.city FROM UserAddress a WHERE a.state = :state ORDER BY a.city")
    List<String> findDistinctCitiesByState(@Param("state") String state);
    
    @Query("SELECT DISTINCT a.state FROM UserAddress a WHERE a.country = :country ORDER BY a.state")
    List<String> findDistinctStatesByCountry(@Param("country") String country);
}