package com.amar.controller;

import com.amar.dto.CreateOrderRequest;
import com.amar.dto.OrderDto;
import com.amar.dto.UpdateOrderStatusRequest;
import com.amar.entity.order.OrderStatus;
import com.amar.service.OrderService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/order-management")
public class OrderManagementController {

    private static final Logger logger = LoggerFactory.getLogger(OrderManagementController.class);

    private final OrderService orderService;

    @Autowired
    public OrderManagementController(OrderService orderService) {
        this.orderService = orderService;
    }

    // =====================================================
    // Order Creation
    // =====================================================

    @PostMapping
    public ResponseEntity<OrderDto> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        logger.info("Creating order for user: {}", request.getUserId());

        try {
            OrderDto order = orderService.createOrder(request);
            logger.info("Order created successfully: {}", order.getOrderNumber());
            return ResponseEntity.status(HttpStatus.CREATED).body(order);

        } catch (Exception e) {
            logger.error("Failed to create order for user: {}", request.getUserId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // =====================================================
    // Order Retrieval
    // =====================================================

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDto> getOrderById(@PathVariable UUID orderId) {
        logger.debug("Retrieving order by ID: {}", orderId);

        Optional<OrderDto> order = orderService.getOrderById(orderId);
        return order.map(o -> ResponseEntity.ok(o))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/number/{orderNumber}")
    public ResponseEntity<OrderDto> getOrderByNumber(@PathVariable String orderNumber) {
        logger.debug("Retrieving order by number: {}", orderNumber);

        Optional<OrderDto> order = orderService.getOrderByNumber(orderNumber);
        return order.map(o -> ResponseEntity.ok(o))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<OrderDto>> getOrdersByUserId(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        logger.debug("Retrieving orders for user: {}", userId);

        Pageable pageable = PageRequest.of(page, size);
        Page<OrderDto> orders = orderService.getOrdersByUserId(userId, pageable);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<Page<OrderDto>> getOrdersByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        logger.debug("Retrieving orders by status: {}", status);

        try {
            OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
            Pageable pageable = PageRequest.of(page, size);
            Page<OrderDto> orders = orderService.getOrdersByStatus(orderStatus, pageable);
            return ResponseEntity.ok(orders);

        } catch (IllegalArgumentException e) {
            logger.error("Invalid order status: {}", status);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/search")
    public ResponseEntity<Page<OrderDto>> searchOrders(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String customerEmail,
            @RequestParam(required = false) String orderNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        logger.debug("Searching orders with filters - userId: {}, status: {}, email: {}, orderNumber: {}",
                userId, status, customerEmail, orderNumber);

        try {
            OrderStatus orderStatus = null;
            if (status != null && !status.trim().isEmpty()) {
                orderStatus = OrderStatus.valueOf(status.toUpperCase());
            }

            Pageable pageable = PageRequest.of(page, size);
            Page<OrderDto> orders = orderService.searchOrders(userId, orderStatus, customerEmail, orderNumber, pageable);
            return ResponseEntity.ok(orders);

        } catch (IllegalArgumentException e) {
            logger.error("Invalid order status: {}", status);
            return ResponseEntity.badRequest().build();
        }
    }

    // =====================================================
    // Order Status Management
    // =====================================================

    @PutMapping("/{orderId}/status")
    public ResponseEntity<OrderDto> updateOrderStatus(
            @PathVariable UUID orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request) {

        logger.info("Updating order status: {} to {}", orderId, request.getStatus());

        try {
            OrderDto order = orderService.updateOrderStatus(orderId, request);
            logger.info("Order status updated successfully: {}", orderId);
            return ResponseEntity.ok(order);

        } catch (RuntimeException e) {
            logger.error("Failed to update order status: {}", orderId, e);
            return ResponseEntity.badRequest().build();

        } catch (Exception e) {
            logger.error("Unexpected error updating order status: {}", orderId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{orderId}/confirm")
    public ResponseEntity<OrderDto> confirmOrder(@PathVariable UUID orderId) {
        logger.info("Confirming order: {}", orderId);

        try {
            OrderDto order = orderService.confirmOrder(orderId);
            logger.info("Order confirmed successfully: {}", orderId);
            return ResponseEntity.ok(order);

        } catch (RuntimeException e) {
            logger.error("Failed to confirm order: {}", orderId, e);
            return ResponseEntity.badRequest().build();

        } catch (Exception e) {
            logger.error("Unexpected error confirming order: {}", orderId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<OrderDto> cancelOrder(
            @PathVariable UUID orderId,
            @RequestParam(required = false, defaultValue = "Order cancelled by request") String reason) {

        logger.info("Cancelling order: {} with reason: {}", orderId, reason);

        try {
            OrderDto order = orderService.cancelOrder(orderId, reason);
            logger.info("Order cancelled successfully: {}", orderId);
            return ResponseEntity.ok(order);

        } catch (RuntimeException e) {
            logger.error("Failed to cancel order: {}", orderId, e);
            return ResponseEntity.badRequest().build();

        } catch (Exception e) {
            logger.error("Unexpected error cancelling order: {}", orderId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // =====================================================
    // Health Check
    // =====================================================

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Order Management Service is healthy");
    }
}