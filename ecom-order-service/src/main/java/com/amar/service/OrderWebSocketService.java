package com.amar.service;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.amar.dto.OrderDto;
import com.amar.dto.OrderStatusUpdate;
import com.amar.entity.order.Order;
import com.amar.entity.order.OrderStatus;
import com.amar.repository.OrderRepository;

/**
 * Service for managing WebSocket connections and broadcasting order status updates
 */
@Service
public class OrderWebSocketService {

    private static final Logger logger = LoggerFactory.getLogger(OrderWebSocketService.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final OrderRepository orderRepository;
    private final OrderService orderService;

    // Track active subscriptions for connection management
    private final ConcurrentMap<UUID, Long> activeOrderSubscriptions = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Long> activeUserSubscriptions = new ConcurrentHashMap<>();

    @Autowired
    public OrderWebSocketService(SimpMessagingTemplate messagingTemplate,
                                OrderRepository orderRepository,
                                @Lazy OrderService orderService) {
        this.messagingTemplate = messagingTemplate;
        this.orderRepository = orderRepository;
        this.orderService = orderService;
    }

    // =====================================================
    // Subscription Management
    // =====================================================

    /**
     * Handle new subscription to order updates
     */
    public void handleOrderSubscription(UUID orderId) {
        activeOrderSubscriptions.put(orderId, System.currentTimeMillis());
        logger.debug("Added order subscription for orderId: {} (Total active: {})", 
                    orderId, activeOrderSubscriptions.size());
        
        // Send current order status immediately upon subscription
        sendCurrentOrderStatus(orderId);
    }

    /**
     * Handle new subscription to user orders
     */
    public void handleUserOrdersSubscription(String userId) {
        activeUserSubscriptions.put(userId, System.currentTimeMillis());
        logger.debug("Added user orders subscription for userId: {} (Total active: {})", 
                    userId, activeUserSubscriptions.size());
        
        // Send current user orders immediately upon subscription
        sendCurrentUserOrders(userId);
    }

    /**
     * Handle subscription to order progress updates
     */
    public void handleOrderProgressSubscription(UUID orderId) {
        logger.debug("Client subscribed to order progress for orderId: {}", orderId);
        // Send current progress status
        sendOrderProgressUpdate(orderId);
    }

    // =====================================================
    // Real-time Broadcasting Methods
    // =====================================================

    /**
     * Broadcast order status update to all subscribed clients
     */
    public void broadcastOrderStatusUpdate(OrderStatusUpdate update) {
        try {
            // Send to order-specific topic
            String orderTopic = "/topic/orders/" + update.getOrderId();
            messagingTemplate.convertAndSend(orderTopic, update);
            logger.debug("Sent order status update to topic: {}", orderTopic);

            // Send to user-specific topic if userId is available
            if (update.getUserId() != null) {
                String userTopic = "/topic/orders/user/" + update.getUserId();
                messagingTemplate.convertAndSend(userTopic, update);
                logger.debug("Sent order status update to user topic: {}", userTopic);
            }

        } catch (Exception e) {
            logger.error("Failed to broadcast order status update for orderId: {}", 
                        update.getOrderId(), e);
        }
    }

    /**
     * Broadcast order progress update
     */
    public void broadcastOrderProgressUpdate(UUID orderId, OrderStatusUpdate.OrderProgressInfo progressInfo) {
        try {
            OrderStatusUpdate update = new OrderStatusUpdate();
            update.setOrderId(orderId);
            update.setProgressInfo(progressInfo);
            update.setReason("Progress update");

            String progressTopic = "/topic/orders/" + orderId + "/progress";
            messagingTemplate.convertAndSend(progressTopic, update);
            logger.debug("Sent order progress update to topic: {}", progressTopic);

        } catch (Exception e) {
            logger.error("Failed to broadcast order progress update for orderId: {}", orderId, e);
        }
    }

    /**
     * Send notification to all connected clients (global announcements)
     */
    public void broadcastGlobalNotification(String message) {
        try {
            messagingTemplate.convertAndSend("/topic/notifications", message);
            logger.debug("Sent global notification: {}", message);
        } catch (Exception e) {
            logger.error("Failed to send global notification", e);
        }
    }

    // =====================================================
    // Refresh Methods (called by controller)
    // =====================================================

    /**
     * Send current order status when client requests refresh
     */
    public void sendOrderStatusRefresh(UUID orderId) {
        sendCurrentOrderStatus(orderId);
    }

    /**
     * Send current user orders when client requests refresh
     */
    public void sendUserOrdersRefresh(String userId) {
        sendCurrentUserOrders(userId);
    }

    // =====================================================
    // Helper Methods
    // =====================================================

    /**
     * Send current order status immediately (for new subscriptions or refresh)
     */
    private void sendCurrentOrderStatus(UUID orderId) {
        try {
            Optional<OrderDto> orderOpt = orderService.getOrderById(orderId);
            if (orderOpt.isPresent()) {
                OrderDto order = orderOpt.get();
                OrderStatusUpdate update = createOrderStatusUpdate(order);
                
                String topic = "/topic/orders/" + orderId;
                messagingTemplate.convertAndSend(topic, update);
                logger.debug("Sent current order status to topic: {}", topic);
            } else {
                logger.warn("Order not found for status refresh: {}", orderId);
            }
        } catch (Exception e) {
            logger.error("Failed to send current order status for orderId: {}", orderId, e);
        }
    }

    /**
     * Send current user orders (for new subscriptions or refresh)
     */
    private void sendCurrentUserOrders(String userId) {
        try {
            Page<OrderDto> orders = orderService.getOrdersByUserId(userId, PageRequest.of(0, 10));
            
            for (OrderDto order : orders.getContent()) {
                OrderStatusUpdate update = createOrderStatusUpdate(order);
                String userTopic = "/topic/orders/user/" + userId;
                messagingTemplate.convertAndSend(userTopic, update);
            }
            
            logger.debug("Sent {} current orders to user topic for userId: {}", 
                        orders.getContent().size(), userId);
            
        } catch (Exception e) {
            logger.error("Failed to send current user orders for userId: {}", userId, e);
        }
    }

    /**
     * Send order progress update
     */
    private void sendOrderProgressUpdate(UUID orderId) {
        try {
            Optional<OrderDto> orderOpt = orderService.getOrderById(orderId);
            if (orderOpt.isPresent()) {
                OrderDto order = orderOpt.get();
                OrderStatusUpdate.OrderProgressInfo progressInfo = createProgressInfo(order.getStatus());
                broadcastOrderProgressUpdate(orderId, progressInfo);
            }
        } catch (Exception e) {
            logger.error("Failed to send order progress update for orderId: {}", orderId, e);
        }
    }

    /**
     * Create OrderStatusUpdate from OrderDto
     */
    private OrderStatusUpdate createOrderStatusUpdate(OrderDto order) {
        OrderStatusUpdate update = new OrderStatusUpdate();
        update.setOrderId(order.getId());
        update.setOrderNumber(order.getOrderNumber());
        update.setUserId(order.getUserId());
        update.setNewStatus(order.getStatus());
        update.setPaymentStatus(order.getPaymentStatus());
        update.setTrackingNumber(order.getTrackingNumber());
        update.setCarrier(order.getCarrier());
        update.setReason("Current status");
        update.setChangedBy("SYSTEM");
        update.setProgressInfo(createProgressInfo(order.getStatus()));
        
        return update;
    }

    /**
     * Create progress information based on order status
     */
    private OrderStatusUpdate.OrderProgressInfo createProgressInfo(String status) {
        OrderStatusUpdate.OrderProgressInfo progressInfo = new OrderStatusUpdate.OrderProgressInfo();
        
        switch (status) {
            case "PENDING":
                progressInfo.setCurrentStep(1);
                progressInfo.setTotalSteps(5);
                progressInfo.setCurrentStepName("Order Received");
                progressInfo.setNextStepName("Payment Processing");
                break;
            case "CONFIRMED":
                progressInfo.setCurrentStep(2);
                progressInfo.setTotalSteps(5);
                progressInfo.setCurrentStepName("Payment Confirmed");
                progressInfo.setNextStepName("Preparing Order");
                break;
            case "PROCESSING":
                progressInfo.setCurrentStep(3);
                progressInfo.setTotalSteps(5);
                progressInfo.setCurrentStepName("Preparing Order");
                progressInfo.setNextStepName("Ready to Ship");
                break;
            case "SHIPPED":
                progressInfo.setCurrentStep(4);
                progressInfo.setTotalSteps(5);
                progressInfo.setCurrentStepName("Order Shipped");
                progressInfo.setNextStepName("Out for Delivery");
                break;
            case "DELIVERED":
                progressInfo.setCurrentStep(5);
                progressInfo.setTotalSteps(5);
                progressInfo.setCurrentStepName("Order Delivered");
                progressInfo.setNextStepName("Complete");
                break;
            case "CANCELLED":
                progressInfo.setCurrentStep(0);
                progressInfo.setTotalSteps(5);
                progressInfo.setCurrentStepName("Order Cancelled");
                progressInfo.setNextStepName(null);
                break;
            default:
                progressInfo.setCurrentStep(1);
                progressInfo.setTotalSteps(5);
                progressInfo.setCurrentStepName("Processing");
                progressInfo.setNextStepName("Next Step");
                break;
        }
        
        return progressInfo;
    }

    // =====================================================
    // Connection Management
    // =====================================================

    /**
     * Clean up inactive subscriptions (can be called periodically)
     */
    public void cleanupInactiveSubscriptions() {
        long currentTime = System.currentTimeMillis();
        long timeout = 30 * 60 * 1000; // 30 minutes timeout

        // Clean up order subscriptions
        activeOrderSubscriptions.entrySet().removeIf(entry -> {
            boolean isInactive = (currentTime - entry.getValue()) > timeout;
            if (isInactive) {
                logger.debug("Removed inactive order subscription for orderId: {}", entry.getKey());
            }
            return isInactive;
        });

        // Clean up user subscriptions
        activeUserSubscriptions.entrySet().removeIf(entry -> {
            boolean isInactive = (currentTime - entry.getValue()) > timeout;
            if (isInactive) {
                logger.debug("Removed inactive user subscription for userId: {}", entry.getKey());
            }
            return isInactive;
        });

        logger.debug("Active subscriptions after cleanup - Orders: {}, Users: {}", 
                    activeOrderSubscriptions.size(), activeUserSubscriptions.size());
    }

    /**
     * Get statistics about active connections
     */
    public String getConnectionStats() {
        return String.format("Active WebSocket Subscriptions - Orders: %d, Users: %d", 
                           activeOrderSubscriptions.size(), activeUserSubscriptions.size());
    }
}