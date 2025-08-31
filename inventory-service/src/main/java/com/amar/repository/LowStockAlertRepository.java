package com.amar.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.amar.entity.LowStockAlert;

@Repository
public interface LowStockAlertRepository extends JpaRepository<LowStockAlert, Long> {
    
    // Find alert by product ID
    Optional<LowStockAlert> findByProductId(Long productId);
    
    // Find alerts by product ID and status
    Optional<LowStockAlert> findByProductIdAndStatusOrderByCreatedAtDesc(Long productId, LowStockAlert.AlertStatus status);
    
    // Find alerts by status ordered by creation date
    List<LowStockAlert> findByStatusOrderByCreatedAtDesc(LowStockAlert.AlertStatus status);
    
    // Find alerts by product ID ordered by creation date
    List<LowStockAlert> findByProductIdOrderByCreatedAtDesc(Long productId);
    
    // Count alerts by status
    long countByStatus(LowStockAlert.AlertStatus status);
    
    // Find alerts by threshold
    List<LowStockAlert> findByThreshold(Integer threshold);
    
    // Find alerts with threshold above/below values
    List<LowStockAlert> findByThresholdGreaterThan(Integer threshold);
    List<LowStockAlert> findByThresholdLessThan(Integer threshold);
    
    // Basic query to find all active alerts
    List<LowStockAlert> findByStatus(LowStockAlert.AlertStatus status);
    
    
    
    // Find alerts created within date range
    List<LowStockAlert> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    
    // Find alerts by multiple product IDs
    List<LowStockAlert> findByProductIdIn(List<Long> productIds);
    
    
    
    
    
    // Update alert threshold
    @Modifying
    @Query("UPDATE LowStockAlert lsa SET lsa.threshold = :threshold WHERE lsa.productId = :productId")
    int updateThreshold(@Param("productId") Long productId, @Param("threshold") Integer threshold);
    
    
    // Get alert statistics
    @Query("SELECT COUNT(*) as totalAlerts, AVG(lsa.threshold) as avgThreshold FROM LowStockAlert lsa")
    Object[] getAlertStatistics();
    
    // Find alerts by threshold range
    List<LowStockAlert> findByThresholdBetween(Integer minThreshold, Integer maxThreshold);
    
    
    // Check if alert exists for product
    Boolean existsByProductId(Long productId);
    
    
    // Count alerts by threshold
    @Query("SELECT COUNT(lsa) FROM LowStockAlert lsa WHERE lsa.threshold = :threshold")
    Long countAlertsByThreshold(@Param("threshold") Integer threshold);
    
    
    
    // Find alerts with threshold criteria
    @Query("SELECT lsa FROM LowStockAlert lsa WHERE " +
           "(:minThreshold IS NULL OR lsa.threshold >= :minThreshold) AND " +
           "(:maxThreshold IS NULL OR lsa.threshold <= :maxThreshold) " +
           "ORDER BY lsa.createdAt DESC")
    Page<LowStockAlert> findAlertsByThresholdRange(@Param("minThreshold") Integer minThreshold,
                                                  @Param("maxThreshold") Integer maxThreshold,
                                                  Pageable pageable);
    
    
    // Delete alerts for products (cleanup when products are deleted)
    @Modifying
    @Query("DELETE FROM LowStockAlert lsa WHERE lsa.productId = :productId")
    int deleteByProductId(@Param("productId") Long productId);
    
    // Bulk delete alerts for multiple products
    @Modifying
    @Query("DELETE FROM LowStockAlert lsa WHERE lsa.productId IN :productIds")
    int deleteByProductIdIn(@Param("productIds") List<Long> productIds);
}