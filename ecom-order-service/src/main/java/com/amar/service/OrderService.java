package com.amar.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.amar.client.CartServiceClient;
import com.amar.client.CartServiceClient.CartValidationResponse;
import com.amar.client.InventoryServiceClient;
import com.amar.client.InventoryServiceClient.StockReservationItem;
import com.amar.client.InventoryServiceClient.StockReservationResponse;
import com.amar.client.PaymentServiceClient;
import com.amar.client.PaymentServiceClient.PaymentRequest;
import com.amar.client.PaymentServiceClient.PaymentResponse;
import com.amar.client.ProductServiceClient;
import com.amar.dto.CreateOrderItemRequest;
import com.amar.dto.CreateOrderRequest;
import com.amar.dto.OrderDto;
import com.amar.dto.OrderItemDto;
import com.amar.dto.ProductDto;
import com.amar.dto.UpdateOrderStatusRequest;
import com.amar.entity.order.ItemFulfillmentStatus;
import com.amar.entity.order.Order;
import com.amar.entity.order.OrderItem;
import com.amar.entity.order.OrderStatus;
import com.amar.entity.order.OrderStatusHistory;
import com.amar.entity.order.PaymentStatus;
import com.amar.kafka.OrderEventPublisher;
import com.amar.repository.OrderRepository;
import com.amar.repository.OrderStatusHistoryRepository;

@Service
@Transactional
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository statusHistoryRepository;
    private final OrderEventPublisher eventPublisher;
    private final InventoryServiceClient inventoryServiceClient;
    private final PaymentServiceClient paymentServiceClient;
    private final CartServiceClient cartServiceClient;
    private final ProductServiceClient productServiceClient;

    @Autowired
    public OrderService(OrderRepository orderRepository,
                       OrderStatusHistoryRepository statusHistoryRepository,
                       OrderEventPublisher eventPublisher,
                       InventoryServiceClient inventoryServiceClient,
                       PaymentServiceClient paymentServiceClient,
                       CartServiceClient cartServiceClient,
                       ProductServiceClient productServiceClient) {
        this.orderRepository = orderRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.eventPublisher = eventPublisher;
        this.inventoryServiceClient = inventoryServiceClient;
        this.paymentServiceClient = paymentServiceClient;
        this.cartServiceClient = cartServiceClient;
        this.productServiceClient = productServiceClient;
    }

    // =====================================================
    // Order Creation
    // =====================================================

    @Transactional
    public OrderDto createOrder(CreateOrderRequest request) {
        logger.info("Creating order for user: {} with {} items", request.getUserId(), request.getItems().size());
        Order order = null;
        boolean inventoryReserved = false;
        boolean paymentProcessed = false;

        try {
            // Step 1: Validate cart if cartId is provided
            if (request.getCartId() != null && !request.getCartId().trim().isEmpty()) {
                logger.debug("Validating cart for checkout: {}", request.getCartId());
                CartValidationResponse cartValidation = cartServiceClient.validateCartForCheckout(request.getCartId());
                
                if (!cartValidation.isValid()) {
                    throw new RuntimeException("Cart validation failed: " + cartValidation.getMessage());
                }
                logger.info("Cart validation successful for: {}", request.getCartId());
            }

            // Step 2: Check inventory availability for all items
            logger.debug("Checking inventory availability for {} items", request.getItems().size());
            Map<Long, Integer> productQuantities = request.getItems().stream()
                .collect(Collectors.toMap(
                    CreateOrderItemRequest::getProductId,
                    CreateOrderItemRequest::getQuantity,
                    Integer::sum  // Handle duplicate products by summing quantities
                ));
            
            Map<Long, Boolean> availabilityCheck = inventoryServiceClient.checkBulkStockAvailability(productQuantities);
            
            // Validate all items are available
            for (CreateOrderItemRequest item : request.getItems()) {
                Boolean available = availabilityCheck.get(item.getProductId());
                if (available == null || !available) {
                    throw new RuntimeException("Insufficient stock for product ID: " + item.getProductId() + " - " + item.getProductName());
                }
            }
            
            // Step 3: Generate order number and calculate totals
            String orderNumber = generateOrderNumber();
            BigDecimal subtotal = calculateSubtotal(request.getItems());
            BigDecimal taxAmount = calculateTax(subtotal);
            BigDecimal shippingCost = calculateShipping(request);
            BigDecimal discountAmount = calculateDiscount(request.getCouponCode(), subtotal);
            BigDecimal totalAmount = subtotal.add(taxAmount).add(shippingCost).subtract(discountAmount);

            // Step 4: Create order entity (but don't set status to confirmed yet)
            order = new Order();
            order.setUserId(request.getUserId());
            order.setOrderNumber(orderNumber);
            order.setStatus(OrderStatus.PENDING);
            order.setTotalAmount(totalAmount);
            order.setSubtotal(subtotal);
            order.setTaxAmount(taxAmount);
            order.setShippingCost(shippingCost);
            order.setDiscountAmount(discountAmount);
            order.setPaymentMethod(request.getPaymentMethod());
            order.setPaymentStatus(PaymentStatus.PENDING);
            order.setCustomerEmail(request.getCustomerEmail());
            order.setCustomerPhone(request.getCustomerPhone());
            order.setShippingMethod(request.getShippingMethod());
            order.setNotes(request.getNotes());

            // Set addresses
            setBillingAddress(order, request);
            setShippingAddress(order, request);

            // Save order first to get ID
            order = orderRepository.save(order);
            logger.debug("Order saved with ID: {}", order.getId());

            // Step 5: Reserve inventory for the order
            logger.debug("Reserving inventory for order: {}", order.getId());
            List<StockReservationItem> reservationItems = request.getItems().stream()
                .map(item -> new StockReservationItem(item.getProductId(), item.getQuantity()))
                .collect(Collectors.toList());
            
            StockReservationResponse reservationResponse = inventoryServiceClient.reserveStock(
                order.getId(), reservationItems, request.getUserId());
            
            if (!reservationResponse.isSuccess()) {
                throw new RuntimeException("Failed to reserve inventory: " + reservationResponse.getMessage());
            }
            
            inventoryReserved = true;
            logger.info("Inventory reserved successfully for order: {}", order.getId());

            // Step 6: Process payment
            logger.debug("Processing payment for order: {} amount: {}", order.getId(), totalAmount);
            PaymentRequest paymentRequest = createPaymentRequest(order, request);
            PaymentResponse paymentResponse = paymentServiceClient.processPayment(paymentRequest);
            
            if (!paymentResponse.isSuccess()) {
                throw new RuntimeException("Payment failed: " + paymentResponse.getMessage());
            }
            
            paymentProcessed = true;
            
            // Update order with payment information
            order.setPaymentStatus(PaymentStatus.PAID);
            order.setPaymentTransactionId(paymentResponse.getTransactionId());
            order.setStatus(OrderStatus.CONFIRMED);  // Move to confirmed after successful payment
            
            logger.info("Payment processed successfully for order: {} - Transaction ID: {}", 
                       order.getId(), paymentResponse.getTransactionId());

            // Step 7: Create order items
            List<OrderItem> orderItems = createOrderItems(order, request.getItems());
            order.setItems(orderItems);

            // Step 8: Clear or mark cart as ordered
            if (request.getCartId() != null && !request.getCartId().trim().isEmpty()) {
                boolean cartCleared = cartServiceClient.clearCart(request.getCartId());
                if (cartCleared) {
                    logger.info("Cart cleared successfully after order: {}", request.getCartId());
                } else {
                    logger.warn("Failed to clear cart after order: {}", request.getCartId());
                }
            }

            // Step 9: Create status history
            createStatusHistory(order, null, OrderStatus.PENDING.name(), "Order created", "SYSTEM");
            createStatusHistory(order, OrderStatus.PENDING.name(), OrderStatus.CONFIRMED.name(), 
                              "Order confirmed with successful payment", "SYSTEM");

            // Step 10: Save updated order
            order = orderRepository.save(order);

            // Step 11: Publish order events
            eventPublisher.publishOrderCreated(
                order.getId(),
                order.getUserId(),
                convertOrderItemsToEventItems(orderItems),
                order.getTotalAmount().toString(),
                order.getStatus().name()
            );

            eventPublisher.publishOrderConfirmed(order.getId(), order.getOrderNumber());

            logger.info("Order created and confirmed successfully: {} for user: {}", orderNumber, request.getUserId());
            return convertToDto(order);

        } catch (Exception e) {
            logger.error("Failed to create order for user: {}", request.getUserId(), e);
            
            // Rollback operations if something went wrong
            if (order != null && inventoryReserved && !paymentProcessed) {
                // Release inventory reservation if payment failed
                logger.warn("Rolling back inventory reservation for failed order: {}", order.getId());
                inventoryServiceClient.releaseReservation(order.getId());
            }
            
            if (order != null) {
                // Mark order as failed
                order.setStatus(OrderStatus.CANCELLED);
                order.setCancellationReason("Order creation failed: " + e.getMessage());
                order.setCancelledAt(LocalDateTime.now());
                orderRepository.save(order);
            }
            
            throw new RuntimeException("Failed to create order: " + e.getMessage());
        }
    }

    // =====================================================
    // Order Retrieval
    // =====================================================

    @Transactional(readOnly = true)
    public Optional<OrderDto> getOrderById(UUID orderId) {
        logger.debug("Retrieving order by ID: {}", orderId);
        return orderRepository.findByIdWithItems(orderId)
                .map(this::convertToDto);
    }

    @Transactional(readOnly = true)
    public Optional<OrderDto> getOrderByNumber(String orderNumber) {
        logger.debug("Retrieving order by number: {}", orderNumber);
        return orderRepository.findByOrderNumberWithItems(orderNumber)
                .map(this::convertToDto);
    }

    @Transactional(readOnly = true)
    public Page<OrderDto> getOrdersByUserId(String userId, Pageable pageable) {
        logger.debug("Retrieving orders for user: {}", userId);
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::convertToDto);
    }

    @Transactional(readOnly = true)
    public Page<OrderDto> getOrdersByStatus(OrderStatus status, Pageable pageable) {
        logger.debug("Retrieving orders by status: {}", status);
        return orderRepository.findByStatusOrderByCreatedAtDesc(status, pageable)
                .map(this::convertToDto);
    }

    @Transactional(readOnly = true)
    public Page<OrderDto> searchOrders(String userId, OrderStatus status, String customerEmail, 
                                      String orderNumber, Pageable pageable) {
        logger.debug("Searching orders with filters - userId: {}, status: {}, email: {}, orderNumber: {}", 
                    userId, status, customerEmail, orderNumber);
        return orderRepository.searchOrders(userId, status, customerEmail, orderNumber, pageable)
                .map(this::convertToDto);
    }

    // =====================================================
    // Order Status Management
    // =====================================================

    @Transactional
    public OrderDto updateOrderStatus(UUID orderId, UpdateOrderStatusRequest request) {
        logger.debug("Updating order status: {} to {}", orderId, request.getStatus());

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        OrderStatus previousStatus = order.getStatus();
        OrderStatus newStatus = OrderStatus.valueOf(request.getStatus());

        // Validate status transition
        validateStatusTransition(previousStatus, newStatus);

        // Update order
        order.setStatus(newStatus);
        order.setAdminNotes(request.getAdminNotes());

        // Handle status-specific updates
        handleStatusSpecificUpdates(order, newStatus, request);

        // Create status history
        createStatusHistory(order, previousStatus.name(), newStatus.name(), 
                          request.getReason(), "ADMIN");

        order = orderRepository.save(order);

        // Publish status update event
        eventPublisher.publishOrderUpdated(orderId, newStatus.name(), 
                                         previousStatus.name(), request.getReason());

        logger.info("Order status updated: {} from {} to {}", orderId, previousStatus, newStatus);
        return convertToDto(order);
    }

    @Transactional
    public OrderDto cancelOrder(UUID orderId, String reason) {
        logger.debug("Cancelling order: {} with reason: {}", orderId, reason);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new RuntimeException("Order is already cancelled");
        }

        if (order.getStatus() == OrderStatus.DELIVERED) {
            throw new RuntimeException("Cannot cancel delivered order");
        }

        // Step 1: Release inventory reservation
        logger.debug("Releasing inventory reservation for cancelled order: {}", orderId);
        boolean reservationReleased = inventoryServiceClient.releaseReservation(orderId);
        
        if (!reservationReleased) {
            logger.warn("Failed to release inventory reservation for order: {}. Continuing with cancellation.", orderId);
            // We log the warning but don't fail the cancellation as the order should still be cancelled
        } else {
            logger.info("Inventory reservation released successfully for cancelled order: {}", orderId);
        }

        OrderStatus previousStatus = order.getStatus();
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancellationReason(reason);
        order.setCancelledAt(LocalDateTime.now());

        // Create status history
        createStatusHistory(order, previousStatus.name(), OrderStatus.CANCELLED.name(), 
                          reason + " (Inventory released)", "SYSTEM");

        order = orderRepository.save(order);

        // Publish cancellation event
        eventPublisher.publishOrderCancelled(orderId, reason, "SYSTEM");

        logger.info("Order cancelled: {} - Reason: {}", orderId, reason);
        return convertToDto(order);
    }

    // =====================================================
    // Order Confirmation
    // =====================================================

    @Transactional
    public OrderDto confirmOrder(UUID orderId) {
        logger.debug("Confirming order: {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Only pending orders can be confirmed");
        }

        // Step 1: Commit the stock reservation (convert to permanent allocation)
        logger.debug("Committing stock reservation for order: {}", orderId);
        boolean stockCommitted = inventoryServiceClient.commitStock(orderId);
        
        if (!stockCommitted) {
            logger.error("Failed to commit stock for order: {}", orderId);
            throw new RuntimeException("Failed to commit inventory for order. Please try again.");
        }
        
        logger.info("Stock committed successfully for order: {}", orderId);

        order.setStatus(OrderStatus.CONFIRMED);

        // Create status history
        createStatusHistory(order, OrderStatus.PENDING.name(), OrderStatus.CONFIRMED.name(), 
                          "Order confirmed with stock commitment", "SYSTEM");

        order = orderRepository.save(order);

        // Publish confirmation event
        eventPublisher.publishOrderConfirmed(orderId, order.getOrderNumber());

        logger.info("Order confirmed: {}", orderId);
        return convertToDto(order);
    }

    // =====================================================
    // Helper Methods
    // =====================================================

    private String generateOrderNumber() {
        String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = orderRepository.count() + 1;
        return String.format("ORD-%s-%05d", dateTime, count);
    }

    private BigDecimal calculateSubtotal(List<CreateOrderItemRequest> items) {
        return items.stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateTax(BigDecimal subtotal) {
        // Simple tax calculation - 8.5%
        return subtotal.multiply(new BigDecimal("0.085"));
    }

    private BigDecimal calculateShipping(CreateOrderRequest request) {
        // Simple shipping calculation
        return new BigDecimal("9.99");
    }

    private BigDecimal calculateDiscount(String couponCode, BigDecimal subtotal) {
        // Simple discount logic
        if ("SAVE10".equals(couponCode)) {
            return subtotal.multiply(new BigDecimal("0.10"));
        }
        return BigDecimal.ZERO;
    }

    private PaymentRequest createPaymentRequest(Order order, CreateOrderRequest request) {
        PaymentRequest paymentRequest = new PaymentRequest(
            order.getId(), 
            order.getTotalAmount(), 
            request.getPaymentMethod(), 
            request.getCustomerEmail()
        );
        
        // Set customer information
        paymentRequest.setCustomerName(request.getBillingFirstName() + " " + request.getBillingLastName());
        paymentRequest.setDescription("Payment for order: " + order.getOrderNumber());
        
        // Set billing address for payment processing
        paymentRequest.setBillingFirstName(request.getBillingFirstName());
        paymentRequest.setBillingLastName(request.getBillingLastName());
        paymentRequest.setBillingStreet(request.getBillingStreet());
        paymentRequest.setBillingCity(request.getBillingCity());
        paymentRequest.setBillingState(request.getBillingState());
        paymentRequest.setBillingPostalCode(request.getBillingPostalCode());
        paymentRequest.setBillingCountry(request.getBillingCountry());
        
        // Set callback URL (for future webhook support)
        paymentRequest.setCallbackUrl("http://order-service/api/v1/payments/callback/" + order.getId());
        
        return paymentRequest;
    }

    private void setBillingAddress(Order order, CreateOrderRequest request) {
        order.setBillingFirstName(request.getBillingFirstName());
        order.setBillingLastName(request.getBillingLastName());
        order.setBillingCompany(request.getBillingCompany());
        order.setBillingStreet(request.getBillingStreet());
        order.setBillingCity(request.getBillingCity());
        order.setBillingState(request.getBillingState());
        order.setBillingPostalCode(request.getBillingPostalCode());
        order.setBillingCountry(request.getBillingCountry());
    }

    private void setShippingAddress(Order order, CreateOrderRequest request) {
        if (request.isSameAsBilling()) {
            order.setShippingFirstName(request.getBillingFirstName());
            order.setShippingLastName(request.getBillingLastName());
            order.setShippingCompany(request.getBillingCompany());
            order.setShippingStreet(request.getBillingStreet());
            order.setShippingCity(request.getBillingCity());
            order.setShippingState(request.getBillingState());
            order.setShippingPostalCode(request.getBillingPostalCode());
            order.setShippingCountry(request.getBillingCountry());
        } else {
            order.setShippingFirstName(request.getShippingFirstName());
            order.setShippingLastName(request.getShippingLastName());
            order.setShippingCompany(request.getShippingCompany());
            order.setShippingStreet(request.getShippingStreet());
            order.setShippingCity(request.getShippingCity());
            order.setShippingState(request.getShippingState());
            order.setShippingPostalCode(request.getShippingPostalCode());
            order.setShippingCountry(request.getShippingCountry());
        }
    }

    private List<OrderItem> createOrderItems(Order order, List<CreateOrderItemRequest> itemRequests) {
        return itemRequests.stream()
                .map(itemRequest -> {
                    OrderItem orderItem = new OrderItem();
                    orderItem.setOrder(order);
                    orderItem.setProductId(itemRequest.getProductId());
                    orderItem.setQuantity(itemRequest.getQuantity());
                    orderItem.setUnitPrice(itemRequest.getUnitPrice());
                    orderItem.setTotalPrice(itemRequest.getTotalPrice());
                    orderItem.setFulfillmentStatus(ItemFulfillmentStatus.PENDING);
                    
                    // Fetch product details from Product Service
                    try {
                        Optional<ProductDto> productOpt = productServiceClient.getProduct(itemRequest.getProductId());
                        if (productOpt.isPresent()) {
                            ProductDto product = productOpt.get();
                            orderItem.setProductName(product.getName() != null ? product.getName() : "Unknown Product");
                            orderItem.setProductSku(product.getSku());
                            orderItem.setProductImageUrl(product.getImageUrl());
                            orderItem.setProductBrand(product.getBrand());
                            orderItem.setProductDescription(product.getDescription());
                        } else {
                            // Fallback if product not found
                            logger.warn("Product not found for ID: {}, using fallback values", itemRequest.getProductId());
                            orderItem.setProductName("Product ID: " + itemRequest.getProductId());
                            orderItem.setProductSku("UNKNOWN");
                        }
                    } catch (Exception e) {
                        // Fallback in case of service failure
                        logger.error("Failed to fetch product details for ID: {}, using fallback", itemRequest.getProductId(), e);
                        orderItem.setProductName("Product ID: " + itemRequest.getProductId());
                        orderItem.setProductSku("UNKNOWN");
                    }
                    
                    return orderItem;
                })
                .collect(Collectors.toList());
    }

    private void createStatusHistory(Order order, String previousStatus, String newStatus, 
                                   String reason, String changedBy) {
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setPreviousStatus(previousStatus);
        history.setNewStatus(newStatus);
        history.setReason(reason);
        history.setChangedBy(changedBy);
        statusHistoryRepository.save(history);
    }

    private void validateStatusTransition(OrderStatus from, OrderStatus to) {
        // Basic validation logic - can be enhanced
        if (from == OrderStatus.CANCELLED && to != OrderStatus.CANCELLED) {
            throw new RuntimeException("Cannot change status of cancelled order");
        }
        if (from == OrderStatus.DELIVERED && to != OrderStatus.RETURNED) {
            throw new RuntimeException("Can only return delivered orders");
        }
    }

    private void handleStatusSpecificUpdates(Order order, OrderStatus newStatus, UpdateOrderStatusRequest request) {
        switch (newStatus) {
            case SHIPPED:
                order.setShippedAt(LocalDateTime.now());
                order.setTrackingNumber(request.getTrackingNumber());
                order.setCarrier(request.getCarrier());
                break;
            case DELIVERED:
                order.setDeliveredAt(LocalDateTime.now());
                break;
            case CANCELLED:
                order.setCancelledAt(LocalDateTime.now());
                order.setCancellationReason(request.getReason());
                break;
        }
    }

    private List<OrderEventPublisher.OrderItem> convertOrderItemsToEventItems(List<OrderItem> items) {
        return items.stream()
                .map(item -> new OrderEventPublisher.OrderItem(
                        item.getProductId(),
                        item.getQuantity(),
                        item.getUnitPrice().toString(),
                        item.getTotalPrice().toString(),
                        item.getProductName()
                ))
                .collect(Collectors.toList());
    }

    private OrderDto convertToDto(Order order) {
        OrderDto dto = new OrderDto();
        dto.setId(order.getId());
        dto.setUserId(order.getUserId());
        dto.setOrderNumber(order.getOrderNumber());
        dto.setStatus(order.getStatus().name());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setSubtotal(order.getSubtotal());
        dto.setTaxAmount(order.getTaxAmount());
        dto.setShippingCost(order.getShippingCost());
        dto.setDiscountAmount(order.getDiscountAmount());
        dto.setPaymentStatus(order.getPaymentStatus() != null ? order.getPaymentStatus().name() : null);
        dto.setPaymentMethod(order.getPaymentMethod());
        dto.setPaymentTransactionId(order.getPaymentTransactionId());
        dto.setShippingMethod(order.getShippingMethod());
        dto.setTrackingNumber(order.getTrackingNumber());
        dto.setCarrier(order.getCarrier());
        dto.setCustomerEmail(order.getCustomerEmail());
        dto.setCustomerPhone(order.getCustomerPhone());
        
        // Billing address
        dto.setBillingFirstName(order.getBillingFirstName());
        dto.setBillingLastName(order.getBillingLastName());
        dto.setBillingCompany(order.getBillingCompany());
        dto.setBillingStreet(order.getBillingStreet());
        dto.setBillingCity(order.getBillingCity());
        dto.setBillingState(order.getBillingState());
        dto.setBillingPostalCode(order.getBillingPostalCode());
        dto.setBillingCountry(order.getBillingCountry());
        
        // Shipping address
        dto.setShippingFirstName(order.getShippingFirstName());
        dto.setShippingLastName(order.getShippingLastName());
        dto.setShippingCompany(order.getShippingCompany());
        dto.setShippingStreet(order.getShippingStreet());
        dto.setShippingCity(order.getShippingCity());
        dto.setShippingState(order.getShippingState());
        dto.setShippingPostalCode(order.getShippingPostalCode());
        dto.setShippingCountry(order.getShippingCountry());
        
        // Convert order items
        if (order.getItems() != null) {
            List<OrderItemDto> itemDtos = order.getItems().stream()
                    .map(this::convertItemToDto)
                    .collect(Collectors.toList());
            dto.setItems(itemDtos);
        }
        
        // Timestamps
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());
        dto.setShippedAt(order.getShippedAt());
        dto.setDeliveredAt(order.getDeliveredAt());
        dto.setCancelledAt(order.getCancelledAt());
        
        // Notes
        dto.setNotes(order.getNotes());
        dto.setAdminNotes(order.getAdminNotes());
        dto.setCancellationReason(order.getCancellationReason());
        dto.setFulfillmentStatus(order.getFulfillmentStatus() != null ? order.getFulfillmentStatus().name() : null);
        
        return dto;
    }

    private OrderItemDto convertItemToDto(OrderItem item) {
        OrderItemDto dto = new OrderItemDto();
        dto.setId(item.getId());
        dto.setOrderId(item.getOrder().getId());
        dto.setProductId(item.getProductId());
        dto.setProductName(item.getProductName());
        dto.setProductSku(item.getProductSku());
        dto.setProductDescription(item.getProductDescription());
        dto.setProductImageUrl(item.getProductImageUrl());
        dto.setProductBrand(item.getProductBrand());
        dto.setProductCategory(item.getProductCategory());
        dto.setQuantity(item.getQuantity());
        dto.setUnitPrice(item.getUnitPrice());
        dto.setTotalPrice(item.getTotalPrice());
        dto.setListPrice(item.getListPrice());
        dto.setDiscountAmount(item.getDiscountAmount());
        dto.setTaxAmount(item.getTaxAmount());
        dto.setFulfillmentStatus(item.getFulfillmentStatus() != null ? item.getFulfillmentStatus().name() : null);
        dto.setQuantityShipped(item.getQuantityShipped());
        dto.setQuantityDelivered(item.getQuantityDelivered());
        dto.setQuantityCancelled(item.getQuantityCancelled());
        dto.setQuantityReturned(item.getQuantityReturned());
        return dto;
    }
}