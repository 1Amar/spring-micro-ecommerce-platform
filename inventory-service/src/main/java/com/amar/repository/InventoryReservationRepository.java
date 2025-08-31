package com.amar.repository;

import com.amar.entity.InventoryReservation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, UUID> {
    
    // Find reservation by order ID
    Optional<InventoryReservation> findByOrderId(UUID orderId);
    
    // Find all reservations for an order
    List<InventoryReservation> findByOrderIdOrderByCreatedAtAsc(UUID orderId);
    
    // Find reservations by product ID
    List<InventoryReservation> findByProductId(Long productId);
    
    // Find reservations by product ID paginated
    Page<InventoryReservation> findByProductId(Long productId, Pageable pageable);
    
    // Find reservations by user or session
    @Query("SELECT ir FROM InventoryReservation ir WHERE ir.userId = :userId OR ir.sessionId = :sessionId")
    List<InventoryReservation> findByUserOrSession(@Param("userId") String userId, @Param("sessionId") String sessionId);
    
    // Count reservations by expired status
    long countByIsExpired(Boolean isExpired);
    
    // Find active (non-expired) reservations by product
    @Query("SELECT ir FROM InventoryReservation ir WHERE ir.productId = :productId AND ir.isExpired = false AND ir.expiresAt > :currentTime")
    List<InventoryReservation> findActiveReservationsByProduct(@Param("productId") Long productId, 
                                                              @Param("currentTime") LocalDateTime currentTime);
    
    // Find reservations by user ID
    List<InventoryReservation> findByUserIdOrderByCreatedAtDesc(String userId);
    
    // Find reservations by session ID
    List<InventoryReservation> findBySessionIdOrderByCreatedAtDesc(String sessionId);
    
    // Find expired reservations that haven't been processed yet
    @Query("SELECT ir FROM InventoryReservation ir WHERE ir.expiresAt <= :currentTime AND ir.isExpired = false")
    List<InventoryReservation> findExpiredReservations(@Param("currentTime") LocalDateTime currentTime);
    
    // Find reservations expiring soon
    @Query("SELECT ir FROM InventoryReservation ir WHERE ir.expiresAt BETWEEN :currentTime AND :warningTime AND ir.isExpired = false")
    List<InventoryReservation> findReservationsExpiringSoon(@Param("currentTime") LocalDateTime currentTime, 
                                                           @Param("warningTime") LocalDateTime warningTime);
    
    // Find reservations by type
    List<InventoryReservation> findByReservationType(InventoryReservation.ReservationType reservationType);
    
    // Find reservations by type and product
    List<InventoryReservation> findByReservationTypeAndProductId(InventoryReservation.ReservationType reservationType, Long productId);
    
    // Mark reservations as expired
    @Modifying
    @Query("UPDATE InventoryReservation ir SET ir.isExpired = true WHERE ir.expiresAt <= :currentTime AND ir.isExpired = false")
    int markExpiredReservations(@Param("currentTime") LocalDateTime currentTime);
    
    // Delete expired reservations older than specified date
    @Modifying
    @Query("DELETE FROM InventoryReservation ir WHERE ir.isExpired = true AND ir.expiresAt < :cutoffDate")
    int deleteOldExpiredReservations(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    // Get total reserved quantity for a product
    @Query("SELECT COALESCE(SUM(ir.quantityReserved), 0) FROM InventoryReservation ir WHERE ir.productId = :productId AND ir.isExpired = false AND ir.expiresAt > :currentTime")
    Integer getTotalReservedQuantityForProduct(@Param("productId") Long productId, @Param("currentTime") LocalDateTime currentTime);
    
    // Get reservation statistics
    @Query("SELECT " +
           "COUNT(*) as totalReservations, " +
           "COUNT(CASE WHEN ir.isExpired = false AND ir.expiresAt > :currentTime THEN 1 END) as activeReservations, " +
           "COUNT(CASE WHEN ir.isExpired = true OR ir.expiresAt <= :currentTime THEN 1 END) as expiredReservations, " +
           "COALESCE(SUM(CASE WHEN ir.isExpired = false AND ir.expiresAt > :currentTime THEN ir.quantityReserved ELSE 0 END), 0) as totalActiveQuantity " +
           "FROM InventoryReservation ir")
    Object[] getReservationStatistics(@Param("currentTime") LocalDateTime currentTime);
    
    // Find reservations created within time range
    List<InventoryReservation> findByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime);
    
    // Find reservations by product and time range
    List<InventoryReservation> findByProductIdAndCreatedAtBetween(Long productId, LocalDateTime startTime, LocalDateTime endTime);
    
    // Check if user/session has active reservation for product
    @Query("SELECT COUNT(ir) > 0 FROM InventoryReservation ir WHERE ir.productId = :productId " +
           "AND (ir.userId = :userId OR ir.sessionId = :sessionId) " +
           "AND ir.isExpired = false AND ir.expiresAt > :currentTime")
    Boolean hasActiveReservation(@Param("productId") Long productId, 
                                @Param("userId") String userId, 
                                @Param("sessionId") String sessionId, 
                                @Param("currentTime") LocalDateTime currentTime);
    
    // Get reservation count by user
    @Query("SELECT COUNT(ir) FROM InventoryReservation ir WHERE ir.userId = :userId AND ir.isExpired = false AND ir.expiresAt > :currentTime")
    Long getActiveReservationCountByUser(@Param("userId") String userId, @Param("currentTime") LocalDateTime currentTime);
    
    // Get reservation count by session
    @Query("SELECT COUNT(ir) FROM InventoryReservation ir WHERE ir.sessionId = :sessionId AND ir.isExpired = false AND ir.expiresAt > :currentTime")
    Long getActiveReservationCountBySession(@Param("sessionId") String sessionId, @Param("currentTime") LocalDateTime currentTime);
    
    // Find reservations by multiple order IDs
    List<InventoryReservation> findByOrderIdIn(List<UUID> orderIds);
    
    // Extend reservation expiration time
    @Modifying
    @Query("UPDATE InventoryReservation ir SET ir.expiresAt = :newExpirationTime WHERE ir.id = :reservationId AND ir.isExpired = false")
    int extendReservationExpiration(@Param("reservationId") UUID reservationId, @Param("newExpirationTime") LocalDateTime newExpirationTime);
    
    // Find product IDs with active reservations
    @Query("SELECT DISTINCT ir.productId FROM InventoryReservation ir WHERE ir.isExpired = false AND ir.expiresAt > :currentTime")
    List<Long> findProductsWithActiveReservations(@Param("currentTime") LocalDateTime currentTime);
    
    // Complex query for reservation summary by product
    @Query("SELECT ir.productId, " +
           "COUNT(*) as totalReservations, " +
           "SUM(ir.quantityReserved) as totalQuantityReserved, " +
           "COUNT(CASE WHEN ir.isExpired = false AND ir.expiresAt > :currentTime THEN 1 END) as activeReservations, " +
           "COUNT(CASE WHEN ir.isExpired = true OR ir.expiresAt <= :currentTime THEN 1 END) as expiredReservations " +
           "FROM InventoryReservation ir " +
           "GROUP BY ir.productId " +
           "HAVING COUNT(*) > 0 " +
           "ORDER BY totalQuantityReserved DESC")
    List<Object[]> getReservationSummaryByProduct(@Param("currentTime") LocalDateTime currentTime);
}