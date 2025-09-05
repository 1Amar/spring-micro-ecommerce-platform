package com.amar.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.amar.entity.inventory.StockMovement;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, UUID> {
    
    // Find movements by product ID
    List<StockMovement> findByProductIdOrderByCreatedAtDesc(Long productId);
    
    // Find movements by product ID paginated
    Page<StockMovement> findByProductIdOrderByCreatedAtDesc(Long productId, Pageable pageable);
    
    // Find movements by movement type
    List<StockMovement> findByMovementTypeOrderByCreatedAtDesc(StockMovement.MovementType movementType);
    
    // Find movements by movement type paginated
    Page<StockMovement> findByMovementTypeOrderByCreatedAtDesc(StockMovement.MovementType movementType, Pageable pageable);
    
    // Find movements by product and type
    List<StockMovement> findByProductIdAndMovementTypeOrderByCreatedAtDesc(Long productId, StockMovement.MovementType movementType);
    
    // Find movements by reference ID (order ID, etc.)
    List<StockMovement> findByReferenceId(UUID referenceId);
    
    // Find movements by reference ID ordered by creation date
    List<StockMovement> findByReferenceIdOrderByCreatedAtDesc(UUID referenceId);
    
    // Find movements by reference type
    List<StockMovement> findByReferenceTypeOrderByCreatedAtDesc(String referenceType);
    
    // Find movements by performed by (user/system)
    List<StockMovement> findByPerformedByOrderByCreatedAtDesc(String performedBy);
    
    // Find movements within date range
    List<StockMovement> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime startDate, LocalDateTime endDate);
    
    // Find movements within date range paginated
    Page<StockMovement> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    
    // Find movements by product and date range
    List<StockMovement> findByProductIdAndCreatedAtBetweenOrderByCreatedAtDesc(Long productId, LocalDateTime startDate, LocalDateTime endDate);
    
    // Find movements by product and date range paginated
    Page<StockMovement> findByProductIdAndCreatedAtBetweenOrderByCreatedAtDesc(Long productId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    
    // Get total quantity moved for a product
    @Query("SELECT COALESCE(SUM(sm.quantityChange), 0) FROM StockMovement sm WHERE sm.productId = :productId")
    Integer getTotalQuantityMovedForProduct(@Param("productId") Long productId);
    
    // Get total inbound quantity for a product
    @Query("SELECT COALESCE(SUM(sm.quantityChange), 0) FROM StockMovement sm WHERE sm.productId = :productId AND sm.quantityChange > 0")
    Integer getTotalInboundQuantityForProduct(@Param("productId") Long productId);
    
    // Get total outbound quantity for a product
    @Query("SELECT COALESCE(SUM(ABS(sm.quantityChange)), 0) FROM StockMovement sm WHERE sm.productId = :productId AND sm.quantityChange < 0")
    Integer getTotalOutboundQuantityForProduct(@Param("productId") Long productId);
    
    // Get movement statistics for a product within date range
    @Query("SELECT " +
           "COUNT(*) as totalMovements, " +
           "COALESCE(SUM(CASE WHEN sm.quantityChange > 0 THEN sm.quantityChange ELSE 0 END), 0) as totalInbound, " +
           "COALESCE(SUM(CASE WHEN sm.quantityChange < 0 THEN ABS(sm.quantityChange) ELSE 0 END), 0) as totalOutbound, " +
           "COALESCE(SUM(sm.quantityChange), 0) as netChange " +
           "FROM StockMovement sm WHERE sm.productId = :productId " +
           "AND sm.createdAt BETWEEN :startDate AND :endDate")
    Object[] getMovementStatisticsForProduct(@Param("productId") Long productId, 
                                           @Param("startDate") LocalDateTime startDate, 
                                           @Param("endDate") LocalDateTime endDate);
    
    // Get global movement statistics within date range
    @Query("SELECT " +
           "COUNT(*) as totalMovements, " +
           "COUNT(DISTINCT sm.productId) as uniqueProducts, " +
           "COALESCE(SUM(CASE WHEN sm.quantityChange > 0 THEN sm.quantityChange ELSE 0 END), 0) as totalInbound, " +
           "COALESCE(SUM(CASE WHEN sm.quantityChange < 0 THEN ABS(sm.quantityChange) ELSE 0 END), 0) as totalOutbound " +
           "FROM StockMovement sm WHERE sm.createdAt BETWEEN :startDate AND :endDate")
    Object[] getGlobalMovementStatistics(@Param("startDate") LocalDateTime startDate, 
                                        @Param("endDate") LocalDateTime endDate);
    
    // Find most active products (by movement count)
    @Query("SELECT sm.productId, COUNT(*) as movementCount FROM StockMovement sm " +
           "WHERE sm.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY sm.productId ORDER BY movementCount DESC")
    List<Object[]> findMostActiveProducts(@Param("startDate") LocalDateTime startDate, 
                                        @Param("endDate") LocalDateTime endDate, 
                                        Pageable pageable);
    
    // Find movements by movement type and date range
    List<StockMovement> findByMovementTypeAndCreatedAtBetweenOrderByCreatedAtDesc(
        StockMovement.MovementType movementType, 
        LocalDateTime startDate, 
        LocalDateTime endDate);
    
    // Get movement count by type within date range
    @Query("SELECT sm.movementType, COUNT(*) FROM StockMovement sm " +
           "WHERE sm.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY sm.movementType ORDER BY COUNT(*) DESC")
    List<Object[]> getMovementCountByType(@Param("startDate") LocalDateTime startDate, 
                                         @Param("endDate") LocalDateTime endDate);
    
    // Get recent movements for dashboard
    @Query("SELECT sm FROM StockMovement sm ORDER BY sm.createdAt DESC")
    List<StockMovement> findRecentMovements(Pageable pageable);
    
    // Find large movements (above threshold)
    @Query("SELECT sm FROM StockMovement sm WHERE ABS(sm.quantityChange) >= :threshold ORDER BY sm.createdAt DESC")
    List<StockMovement> findLargeMovements(@Param("threshold") Integer threshold);
    
    // Find movements with notes/reasons containing text
    @Query("SELECT sm FROM StockMovement sm WHERE " +
           "(sm.notes IS NOT NULL AND LOWER(sm.notes) LIKE LOWER(CONCAT('%', :searchText, '%'))) OR " +
           "(sm.reason IS NOT NULL AND LOWER(sm.reason) LIKE LOWER(CONCAT('%', :searchText, '%'))) " +
           "ORDER BY sm.createdAt DESC")
    List<StockMovement> findMovementsByNotesOrReason(@Param("searchText") String searchText);
    
    // Find movements by multiple products
    List<StockMovement> findByProductIdInOrderByCreatedAtDesc(List<Long> productIds);
    
    // Find adjustments only
    @Query("SELECT sm FROM StockMovement sm WHERE sm.movementType = 'ADJUSTMENT' ORDER BY sm.createdAt DESC")
    List<StockMovement> findAllAdjustments();
    
    // Find manual adjustments by admin user
    @Query("SELECT sm FROM StockMovement sm WHERE sm.movementType = 'ADJUSTMENT' AND sm.performedBy = :adminUser ORDER BY sm.createdAt DESC")
    List<StockMovement> findAdjustmentsByAdmin(@Param("adminUser") String adminUser);
    
    // Get daily movement summary for charts/reports
    @Query("SELECT DATE(sm.createdAt) as movementDate, " +
           "COUNT(*) as totalMovements, " +
           "COALESCE(SUM(CASE WHEN sm.quantityChange > 0 THEN sm.quantityChange ELSE 0 END), 0) as totalInbound, " +
           "COALESCE(SUM(CASE WHEN sm.quantityChange < 0 THEN ABS(sm.quantityChange) ELSE 0 END), 0) as totalOutbound " +
           "FROM StockMovement sm " +
           "WHERE sm.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY DATE(sm.createdAt) " +
           "ORDER BY movementDate DESC")
    List<Object[]> getDailyMovementSummary(@Param("startDate") LocalDateTime startDate, 
                                          @Param("endDate") LocalDateTime endDate);
    
    // Find system vs user movements
    @Query("SELECT " +
           "COUNT(CASE WHEN sm.performedBy = 'SYSTEM' THEN 1 END) as systemMovements, " +
           "COUNT(CASE WHEN sm.performedBy != 'SYSTEM' THEN 1 END) as userMovements " +
           "FROM StockMovement sm " +
           "WHERE sm.createdAt BETWEEN :startDate AND :endDate")
    Object[] getSystemVsUserMovements(@Param("startDate") LocalDateTime startDate, 
                                     @Param("endDate") LocalDateTime endDate);
}