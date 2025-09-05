package com.amar.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.amar.entity.order.OrderStatusHistory;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, UUID> {
    
    // Find status history by order ID
    List<OrderStatusHistory> findByOrderIdOrderByCreatedAtDesc(UUID orderId);
    
    // Find status history by order ID and status
    @Query("SELECT h FROM OrderStatusHistory h WHERE h.order.id = :orderId AND " +
           "(h.previousStatus = :status OR h.newStatus = :status) ORDER BY h.createdAt DESC")
    List<OrderStatusHistory> findByOrderIdAndStatus(@Param("orderId") UUID orderId, 
                                                   @Param("status") String status);
    
    // Find latest status change for an order
    @Query("SELECT h FROM OrderStatusHistory h WHERE h.order.id = :orderId ORDER BY h.createdAt DESC LIMIT 1")
    OrderStatusHistory findLatestByOrderId(@Param("orderId") UUID orderId);
    
    // Find all status changes by changed by user
    List<OrderStatusHistory> findByChangedByOrderByCreatedAtDesc(String changedBy);
}