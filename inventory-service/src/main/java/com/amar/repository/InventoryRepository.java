package com.amar.repository;

import com.amar.entity.Inventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, UUID> {
    
    // Find by product ID with pessimistic locking for concurrent access
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.productId = :productId")
    Optional<Inventory> findByProductIdWithLock(@Param("productId") Long productId);
    
    // Find by product ID without locking for read operations
    Optional<Inventory> findByProductId(Long productId);
    
    // Find multiple products by IDs
    List<Inventory> findByProductIdIn(List<Long> productIds);
    
    // Find products with low stock
    @Query("SELECT i FROM Inventory i WHERE i.availableQuantity <= i.reorderLevel AND i.availableQuantity >= 0")
    List<Inventory> findLowStockProducts();
    
    // Find products with low stock paginated
    @Query("SELECT i FROM Inventory i WHERE i.availableQuantity <= i.reorderLevel AND i.availableQuantity >= 0")
    Page<Inventory> findLowStockProducts(Pageable pageable);
    
    // Find out of stock products
    @Query("SELECT i FROM Inventory i WHERE i.availableQuantity = 0")
    List<Inventory> findOutOfStockProducts();
    
    // Find out of stock products paginated
    @Query("SELECT i FROM Inventory i WHERE i.availableQuantity = 0")
    Page<Inventory> findOutOfStockProducts(Pageable pageable);
    
    // Find products below specific threshold
    @Query("SELECT i FROM Inventory i WHERE i.availableQuantity <= :threshold")
    List<Inventory> findProductsBelowThreshold(@Param("threshold") Integer threshold);
    
    // Find low stock items (alias for findProductsBelowThreshold)
    @Query("SELECT i FROM Inventory i WHERE i.availableQuantity <= :threshold")
    List<Inventory> findLowStockItems(@Param("threshold") Integer threshold);
    
    // Find products with available quantity between range
    @Query("SELECT i FROM Inventory i WHERE i.availableQuantity BETWEEN :minQuantity AND :maxQuantity")
    Page<Inventory> findByAvailableQuantityBetween(@Param("minQuantity") Integer minQuantity, 
                                                  @Param("maxQuantity") Integer maxQuantity, 
                                                  Pageable pageable);
    
    // Check if product has sufficient stock
    @Query("SELECT i.availableQuantity >= :requestedQuantity FROM Inventory i WHERE i.productId = :productId")
    Boolean hasSufficientStock(@Param("productId") Long productId, @Param("requestedQuantity") Integer requestedQuantity);
    
    // Get available quantity for product
    @Query("SELECT i.availableQuantity FROM Inventory i WHERE i.productId = :productId")
    Optional<Integer> getAvailableQuantity(@Param("productId") Long productId);
    
    // Bulk update reserved quantity (for reservation operations)
    @Modifying
    @Query("UPDATE Inventory i SET i.reservedQuantity = i.reservedQuantity + :quantityChange WHERE i.productId = :productId")
    int updateReservedQuantity(@Param("productId") Long productId, @Param("quantityChange") Integer quantityChange);
    
    // Bulk update total quantity (for stock adjustments)
    @Modifying
    @Query("UPDATE Inventory i SET i.quantity = i.quantity + :quantityChange WHERE i.productId = :productId")
    int updateTotalQuantity(@Param("productId") Long productId, @Param("quantityChange") Integer quantityChange);
    
    // Update reorder level for product
    @Modifying
    @Query("UPDATE Inventory i SET i.reorderLevel = :newLevel WHERE i.productId = :productId")
    int updateReorderLevel(@Param("productId") Long productId, @Param("newLevel") Integer newLevel);
    
    // Update max stock level for product
    @Modifying
    @Query("UPDATE Inventory i SET i.maxStockLevel = :newLevel WHERE i.productId = :productId")
    int updateMaxStockLevel(@Param("productId") Long productId, @Param("newLevel") Integer newLevel);
    
    // Get inventory statistics
    @Query("SELECT " +
           "COUNT(*) as totalProducts, " +
           "COUNT(CASE WHEN i.availableQuantity > i.reorderLevel THEN 1 END) as inStockProducts, " +
           "COUNT(CASE WHEN i.availableQuantity <= i.reorderLevel AND i.availableQuantity > 0 THEN 1 END) as lowStockProducts, " +
           "COUNT(CASE WHEN i.availableQuantity = 0 THEN 1 END) as outOfStockProducts, " +
           "COALESCE(SUM(i.quantity), 0) as totalQuantity, " +
           "COALESCE(SUM(i.reservedQuantity), 0) as totalReservedQuantity, " +
           "COALESCE(SUM(i.availableQuantity), 0) as totalAvailableQuantity " +
           "FROM Inventory i")
    Object[] getInventoryStatistics();
    
    // Find products that need reordering (below reorder level)
    @Query("SELECT i FROM Inventory i WHERE i.availableQuantity <= i.reorderLevel ORDER BY (i.reorderLevel - i.availableQuantity) DESC")
    List<Inventory> findProductsNeedingReorder();
    
    // Find products with excessive stock (above max level)
    @Query("SELECT i FROM Inventory i WHERE i.quantity > i.maxStockLevel")
    List<Inventory> findProductsWithExcessiveStock();
    
    // Find products by reserved quantity greater than threshold
    @Query("SELECT i FROM Inventory i WHERE i.reservedQuantity > :threshold")
    List<Inventory> findProductsWithHighReservations(@Param("threshold") Integer threshold);
    
    // Get total stock value (if we had price information)
    @Query("SELECT COUNT(i) FROM Inventory i WHERE i.availableQuantity > 0")
    Long countProductsInStock();
    
    // Get products modified after specific date
    @Query("SELECT i FROM Inventory i WHERE i.updatedAt > :timestamp ORDER BY i.updatedAt DESC")
    List<Inventory> findRecentlyModifiedInventory(@Param("timestamp") java.time.LocalDateTime timestamp);
    
    // Find products by multiple criteria with native query for complex filtering
    @Query(value = "SELECT * FROM inventory_service_schema.inventory i " +
                   "WHERE (:productIds IS NULL OR i.product_id = ANY(:productIds)) " +
                   "AND (:minQuantity IS NULL OR i.available_quantity >= :minQuantity) " +
                   "AND (:maxQuantity IS NULL OR i.available_quantity <= :maxQuantity) " +
                   "AND (:lowStock IS NULL OR (:lowStock = true AND i.available_quantity <= i.reorder_level) OR (:lowStock = false AND i.available_quantity > i.reorder_level)) " +
                   "ORDER BY i.updated_at DESC", 
           nativeQuery = true)
    Page<Inventory> findByCriteria(@Param("productIds") List<Long> productIds,
                                  @Param("minQuantity") Integer minQuantity,
                                  @Param("maxQuantity") Integer maxQuantity,
                                  @Param("lowStock") Boolean lowStock,
                                  Pageable pageable);
}