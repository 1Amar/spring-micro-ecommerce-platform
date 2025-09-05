package com.amar.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.amar.entity.order.Order;
import com.amar.entity.order.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
    
    // Find by order number
    Optional<Order> findByOrderNumber(String orderNumber);
    
    // Find orders by user ID
    Page<Order> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    
    // Find orders by status
    Page<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status, Pageable pageable);
    
    // Find orders by user ID and status
    Page<Order> findByUserIdAndStatusOrderByCreatedAtDesc(String userId, OrderStatus status, Pageable pageable);
    
    // Find orders by customer email
    Page<Order> findByCustomerEmailOrderByCreatedAtDesc(String customerEmail, Pageable pageable);
    
    // Find orders created between dates
    Page<Order> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    
    // Find orders by tracking number
    Optional<Order> findByTrackingNumber(String trackingNumber);
    
    // Count orders by user ID
    long countByUserId(String userId);
    
    // Count orders by status
    long countByStatus(OrderStatus status);
    
    // Find orders that need processing (pending, confirmed)
    @Query("SELECT o FROM Order o WHERE o.status IN ('PENDING', 'CONFIRMED') ORDER BY o.createdAt ASC")
    List<Order> findOrdersNeedingProcessing();
    
    // Find orders by payment status
    @Query("SELECT o FROM Order o WHERE o.paymentStatus = :paymentStatus ORDER BY o.createdAt DESC")
    Page<Order> findByPaymentStatus(@Param("paymentStatus") String paymentStatus, Pageable pageable);
    
    // Find orders with items by user ID (with eager loading)
    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items WHERE o.userId = :userId ORDER BY o.createdAt DESC")
    List<Order> findByUserIdWithItems(@Param("userId") String userId);
    
    // Find order with items by order number
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.orderNumber = :orderNumber")
    Optional<Order> findByOrderNumberWithItems(@Param("orderNumber") String orderNumber);
    
    // Find order with items by ID
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.id = :id")
    Optional<Order> findByIdWithItems(@Param("id") UUID id);
    
    // Find recent orders for dashboard
    @Query("SELECT o FROM Order o ORDER BY o.createdAt DESC")
    List<Order> findRecentOrders(Pageable pageable);
    
    // Count orders by user ID and status
    long countByUserIdAndStatus(String userId, OrderStatus status);
    
    // Find orders with specific fulfillment status
    @Query("SELECT o FROM Order o WHERE o.fulfillmentStatus = :fulfillmentStatus ORDER BY o.updatedAt ASC")
    List<Order> findByFulfillmentStatus(@Param("fulfillmentStatus") String fulfillmentStatus);
    
    // Custom search query
    @Query("SELECT o FROM Order o WHERE " +
           "(:userId IS NULL OR o.userId = :userId) AND " +
           "(:status IS NULL OR o.status = :status) AND " +
           "(:customerEmail IS NULL OR LOWER(o.customerEmail) LIKE LOWER(CONCAT('%', :customerEmail, '%'))) AND " +
           "(:orderNumber IS NULL OR LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :orderNumber, '%'))) " +
           "ORDER BY o.createdAt DESC")
    Page<Order> searchOrders(@Param("userId") String userId,
                            @Param("status") OrderStatus status,
                            @Param("customerEmail") String customerEmail,
                            @Param("orderNumber") String orderNumber,
                            Pageable pageable);
}