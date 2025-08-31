package com.amar.service;

import com.amar.dto.InventoryReservationDto;
import com.amar.dto.request.StockReservationRequest;
import com.amar.dto.response.StockReservationResponse;
import com.amar.entity.Inventory;
import com.amar.entity.InventoryReservation;
import com.amar.repository.InventoryRepository;
import com.amar.repository.InventoryReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class StockReservationService {

    private static final Logger logger = LoggerFactory.getLogger(StockReservationService.class);

    private final InventoryRepository inventoryRepository;
    private final InventoryReservationRepository reservationRepository;
    private final StockMovementService stockMovementService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${inventory.reservation.default-ttl-minutes:15}")
    private Integer defaultTtlMinutes;

    @Value("${inventory.reservation.max-ttl-minutes:60}")
    private Integer maxTtlMinutes;

    @Autowired
    public StockReservationService(InventoryRepository inventoryRepository,
                                  InventoryReservationRepository reservationRepository,
                                  StockMovementService stockMovementService,
                                  KafkaTemplate<String, Object> kafkaTemplate) {
        this.inventoryRepository = inventoryRepository;
        this.reservationRepository = reservationRepository;
        this.stockMovementService = stockMovementService;
        this.kafkaTemplate = kafkaTemplate;
    }

    // =====================================================
    // Stock Reservation Operations
    // =====================================================

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public StockReservationResponse reserveStock(StockReservationRequest request) {
        return createReservation(request);
    }
    
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public StockReservationResponse createReservation(StockReservationRequest request) {
        logger.info("Creating stock reservation for order ID: {} with {} items", 
                   request.getOrderId(), request.getItems().size());

        // Validate TTL
        Integer ttlMinutes = validateTtl(request.getExpirationMinutes());
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(ttlMinutes);

        // Check for existing reservation for this order
        Optional<InventoryReservation> existingReservation = reservationRepository.findByOrderId(request.getOrderId());
        if (existingReservation.isPresent()) {
            throw new IllegalStateException("Reservation already exists for order ID: " + request.getOrderId());
        }

        List<StockReservationResponse.ReservationItem> successfulReservations = new ArrayList<>();
        List<StockReservationResponse.ReservationError> errors = new ArrayList<>();

        // Process each item in the reservation request
        for (StockReservationRequest.ReservationItem item : request.getItems()) {
            try {
                StockReservationResponse.ReservationItem reservation = processItemReservation(
                    item, request.getOrderId(), request.getUserId(), request.getSessionId(),
                    request.getReservationType(), expiresAt
                );
                successfulReservations.add(reservation);
            } catch (Exception ex) {
                logger.warn("Failed to reserve stock for product ID: {} in order: {}", 
                           item.getProductId(), request.getOrderId(), ex);
                
                StockReservationResponse.ReservationError error = new StockReservationResponse.ReservationError();
                error.setProductId(item.getProductId());
                error.setRequestedQuantity(item.getQuantity());
                error.setErrorCode(getErrorCode(ex));
                error.setErrorMessage(ex.getMessage());
                errors.add(error);
            }
        }

        // If no items could be reserved, fail the entire operation
        if (successfulReservations.isEmpty()) {
            throw new IllegalStateException("No items could be reserved for order ID: " + request.getOrderId());
        }

        // Calculate totals
        Integer totalItemsReserved = successfulReservations.size();
        Integer totalQuantityReserved = successfulReservations.stream()
            .mapToInt(item -> item.getReservedQuantity())
            .sum();

        boolean partialReservation = !errors.isEmpty();

        // Publish reservation event
        publishReservationEvent("stock.reserved", request.getOrderId(), successfulReservations);

        StockReservationResponse response = new StockReservationResponse();
        response.setSuccess(true);
        response.setOrderId(request.getOrderId());
        response.setReservations(successfulReservations);
        
        response.setExpiresAt(expiresAt);
        response.setMessage(partialReservation ? "Partial reservation successful" : "Reservation successful");
        response.setErrors(errors.isEmpty() ? null : errors);
        response.setTotalItemsReserved(totalItemsReserved);
        response.setTotalQuantityReserved(totalQuantityReserved);
        response.setPartialReservation(partialReservation);
        return response;
    }

    private StockReservationResponse.ReservationItem processItemReservation(StockReservationRequest.ReservationItem item,
                                                          UUID orderId, String userId, String sessionId,
                                                          String reservationType, LocalDateTime expiresAt) {
        // Get inventory with pessimistic lock
        Optional<Inventory> inventoryOpt = inventoryRepository.findByProductIdWithLock(item.getProductId());
        if (inventoryOpt.isEmpty()) {
            throw new IllegalArgumentException("Product not found in inventory: " + item.getProductId());
        }

        Inventory inventory = inventoryOpt.get();
        
        // Check if sufficient stock is available
        if (!inventory.canReserve(item.getQuantity())) {
            throw new IllegalStateException(
                String.format("Insufficient stock. Available: %d, Requested: %d", 
                             inventory.getAvailableQuantity(), item.getQuantity())
            );
        }

        // Reserve the stock
        inventory.reserveStock(item.getQuantity());
        inventoryRepository.save(inventory);

        // Create reservation record
        InventoryReservation reservation = new InventoryReservation();
        reservation.setProductId(item.getProductId());
        reservation.setOrderId(orderId);
        reservation.setUserId(userId);
        reservation.setSessionId(sessionId);
        reservation.setQuantityReserved(item.getQuantity());
        reservation.setReservedBy(userId != null ? userId : sessionId);
        reservation.setReservationType(parseReservationType(reservationType));
        reservation.setExpiresAt(expiresAt);
        reservation.setIsExpired(false);

        reservation = reservationRepository.save(reservation);

        // Record stock movement
        stockMovementService.recordReservation(item.getProductId(), item.getQuantity(), orderId, 
                                             userId != null ? userId : sessionId);

        return new StockReservationResponse.ReservationItem(
            item.getProductId(), item.getQuantity(), item.getQuantity(), "RESERVED");
    }

    @Transactional
    public void commitReservation(UUID orderId) {
        logger.info("Committing reservation for order ID: {}", orderId);

        List<InventoryReservation> reservations = reservationRepository.findByOrderIdOrderByCreatedAtAsc(orderId);
        if (reservations.isEmpty()) {
            throw new IllegalArgumentException("No reservations found for order ID: " + orderId);
        }

        for (InventoryReservation reservation : reservations) {
            // Get inventory with lock
            Optional<Inventory> inventoryOpt = inventoryRepository.findByProductIdWithLock(reservation.getProductId());
            if (inventoryOpt.isEmpty()) {
                logger.error("Inventory not found for product ID: {} during commit", reservation.getProductId());
                continue;
            }

            Inventory inventory = inventoryOpt.get();
            
            // Remove from total quantity and reserved quantity
            inventory.removeStock(reservation.getQuantityReserved());
            inventory.releaseReservedStock(reservation.getQuantityReserved());
            
            inventoryRepository.save(inventory);

            // Record stock movement
            stockMovementService.recordOutbound(reservation.getProductId(), reservation.getQuantityReserved(),
                                               orderId, "ORDER", "Stock sold", reservation.getReservedBy());

            // Mark reservation as expired (to indicate it's been processed)
            reservation.setIsExpired(true);
            reservationRepository.save(reservation);
        }

        // Publish commit event
        publishReservationEntityEvent("stock.reservation.committed", orderId, reservations);
    }

    @Transactional
    public void releaseReservation(UUID orderId) {
        logger.info("Releasing reservation for order ID: {}", orderId);

        List<InventoryReservation> reservations = reservationRepository.findByOrderIdOrderByCreatedAtAsc(orderId);
        if (reservations.isEmpty()) {
            throw new IllegalArgumentException("No reservations found for order ID: " + orderId);
        }

        for (InventoryReservation reservation : reservations) {
            if (reservation.getIsExpired()) {
                continue; // Already processed
            }

            // Get inventory with lock
            Optional<Inventory> inventoryOpt = inventoryRepository.findByProductIdWithLock(reservation.getProductId());
            if (inventoryOpt.isEmpty()) {
                logger.error("Inventory not found for product ID: {} during release", reservation.getProductId());
                continue;
            }

            Inventory inventory = inventoryOpt.get();
            
            // Release reserved stock
            inventory.releaseReservedStock(reservation.getQuantityReserved());
            inventoryRepository.save(inventory);

            // Record stock movement
            stockMovementService.recordReservationRelease(reservation.getProductId(), 
                                                         reservation.getQuantityReserved(), orderId, 
                                                         reservation.getReservedBy());

            // Mark reservation as expired
            reservation.setIsExpired(true);
            reservationRepository.save(reservation);
        }

        // Publish release event
        publishReservationEntityEvent("stock.reservation.released", orderId, reservations);
    }

    // =====================================================
    // Reservation Management
    // =====================================================

    @Transactional(readOnly = true)
    public List<InventoryReservationDto> getReservationsForOrder(UUID orderId) {
        List<InventoryReservation> reservations = reservationRepository.findByOrderIdOrderByCreatedAtAsc(orderId);
        return reservations.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<InventoryReservationDto> getActiveReservationsForUser(String userId, String sessionId) {
        List<InventoryReservation> reservations = reservationRepository.findByUserOrSession(userId, sessionId);
        return reservations.stream()
            .filter(r -> !r.getIsExpired())
            .map(this::mapToDto)
            .collect(Collectors.toList());
    }

    @Transactional
    public boolean extendReservation(UUID reservationId, Integer additionalMinutes) {
        logger.info("Extending reservation ID: {} by {} minutes", reservationId, additionalMinutes);

        Optional<InventoryReservation> reservationOpt = reservationRepository.findById(reservationId);
        if (reservationOpt.isEmpty()) {
            return false;
        }

        InventoryReservation reservation = reservationOpt.get();
        if (!reservation.canExtend()) {
            return false;
        }

        // Validate extension doesn't exceed max TTL
        LocalDateTime newExpiration = reservation.getExpiresAt().plusMinutes(additionalMinutes);
        LocalDateTime maxAllowedExpiration = reservation.getCreatedAt().plusMinutes(maxTtlMinutes);
        
        if (newExpiration.isAfter(maxAllowedExpiration)) {
            newExpiration = maxAllowedExpiration;
        }

        reservation.setExpiresAt(newExpiration);
        reservationRepository.save(reservation);

        logger.info("Extended reservation ID: {} to {}", reservationId, newExpiration);
        return true;
    }

    // =====================================================
    // Background Jobs
    // =====================================================

    @Scheduled(fixedRateString = "${inventory.reservation.cleanup-interval-minutes:5}000")
    @Transactional
    public void cleanupExpiredReservations() {
        logger.debug("Running expired reservations cleanup job");

        LocalDateTime now = LocalDateTime.now();
        
        // Find expired reservations
        List<InventoryReservation> expiredReservations = reservationRepository.findExpiredReservations(now);
        
        if (expiredReservations.isEmpty()) {
            return;
        }

        logger.info("Processing {} expired reservations", expiredReservations.size());

        for (InventoryReservation reservation : expiredReservations) {
            try {
                processExpiredReservation(reservation);
            } catch (Exception ex) {
                logger.error("Failed to process expired reservation ID: {}", reservation.getId(), ex);
            }
        }

        // Mark them as expired in bulk
        int markedCount = reservationRepository.markExpiredReservations(now);
        logger.info("Marked {} reservations as expired", markedCount);

        // Clean up old expired reservations (older than 7 days)
        LocalDateTime cutoffDate = now.minusDays(7);
        int deletedCount = reservationRepository.deleteOldExpiredReservations(cutoffDate);
        if (deletedCount > 0) {
            logger.info("Deleted {} old expired reservations", deletedCount);
        }
    }

    private void processExpiredReservation(InventoryReservation reservation) {
        if (reservation.getIsExpired()) {
            return; // Already processed
        }

        // Get inventory with lock
        Optional<Inventory> inventoryOpt = inventoryRepository.findByProductIdWithLock(reservation.getProductId());
        if (inventoryOpt.isEmpty()) {
            logger.warn("Inventory not found for expired reservation. Product ID: {}", reservation.getProductId());
            return;
        }

        Inventory inventory = inventoryOpt.get();
        
        // Release reserved stock
        inventory.releaseReservedStock(reservation.getQuantityReserved());
        inventoryRepository.save(inventory);

        // Record stock movement
        stockMovementService.recordReservationRelease(reservation.getProductId(), 
                                                     reservation.getQuantityReserved(), 
                                                     reservation.getOrderId(), "SYSTEM");

        // Publish expiration event
        publishReservationEntityEvent("stock.reservation.expired", reservation.getOrderId(), 
                               List.of(reservation));
    }

    // =====================================================
    // Helper Methods
    // =====================================================

    private Integer validateTtl(Integer requestedTtl) {
        if (requestedTtl == null) {
            return defaultTtlMinutes;
        }
        if (requestedTtl < 1) {
            return defaultTtlMinutes;
        }
        if (requestedTtl > maxTtlMinutes) {
            return maxTtlMinutes;
        }
        return requestedTtl;
    }

    private InventoryReservation.ReservationType parseReservationType(String type) {
        if (type == null) {
            return InventoryReservation.ReservationType.CHECKOUT;
        }
        try {
            return InventoryReservation.ReservationType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return InventoryReservation.ReservationType.CHECKOUT;
        }
    }

    private String getErrorCode(Exception ex) {
        if (ex instanceof IllegalArgumentException) {
            return "PRODUCT_NOT_FOUND";
        } else if (ex instanceof IllegalStateException && ex.getMessage().contains("Insufficient stock")) {
            return "INSUFFICIENT_STOCK";
        } else if (ex instanceof IllegalStateException && ex.getMessage().contains("already exists")) {
            return "DUPLICATE_RESERVATION";
        } else {
            return "RESERVATION_ERROR";
        }
    }

    private void publishReservationEvent(String eventType, UUID orderId, List<StockReservationResponse.ReservationItem> reservations) {
        try {
            Map<String, Object> event = Map.of(
                "eventType", eventType,
                "orderId", orderId,
                "reservations", reservations,
                "reservationCount", reservations.size(),
                "timestamp", LocalDateTime.now()
            );
            
            kafkaTemplate.send("inventory-reservation-events", orderId.toString(), event);
            logger.debug("Published reservation event: {} for order ID: {}", eventType, orderId);
        } catch (Exception ex) {
            logger.error("Failed to publish reservation event for order ID: {}", orderId, ex);
        }
    }
    
    private void publishReservationEntityEvent(String eventType, UUID orderId, List<InventoryReservation> reservations) {
        try {
            List<Map<String, Object>> reservationData = reservations.stream()
                .map(reservation -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("productId", reservation.getProductId());
                    data.put("quantityReserved", reservation.getQuantityReserved());
                    data.put("reservationType", reservation.getReservationType().toString());
                    data.put("expiresAt", reservation.getExpiresAt());
                    return data;
                })
                .collect(Collectors.toList());
                
            Map<String, Object> event = Map.of(
                "eventType", eventType,
                "orderId", orderId,
                "reservations", reservationData,
                "reservationCount", reservations.size(),
                "timestamp", LocalDateTime.now()
            );
            
            kafkaTemplate.send("inventory-reservation-events", orderId.toString(), event);
            logger.debug("Published reservation event: {} for order ID: {}", eventType, orderId);
        } catch (Exception ex) {
            logger.error("Failed to publish reservation event for order ID: {}", orderId, ex);
        }
    }
    
    // Manual mapping method to replace InventoryReservationMapper
    private InventoryReservationDto mapToDto(InventoryReservation reservation) {
        InventoryReservationDto dto = new InventoryReservationDto();
        dto.setId(reservation.getId());
        dto.setProductId(reservation.getProductId());
        dto.setOrderId(reservation.getOrderId());
        dto.setUserId(reservation.getUserId());
        dto.setSessionId(reservation.getSessionId());
        dto.setQuantityRequested(reservation.getQuantityReserved()); // Using reserved as requested for now
        dto.setQuantityReserved(reservation.getQuantityReserved());
        dto.setReservedBy(reservation.getReservedBy());
        dto.setReservationType(reservation.getReservationType().toString());
        dto.setExpiresAt(reservation.getExpiresAt());
        dto.setIsExpired(reservation.getIsExpired());
        dto.setCreatedAt(reservation.getCreatedAt());
        return dto;
    }
    
    // Additional methods needed by controllers
    public Map<String, Object> getReservationStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        
        long totalReservations = reservationRepository.count();
        long activeReservations = reservationRepository.countByIsExpired(false);
        long expiredReservations = reservationRepository.countByIsExpired(true);
        
        statistics.put("totalReservations", totalReservations);
        statistics.put("activeReservations", activeReservations);
        statistics.put("expiredReservations", expiredReservations);
        statistics.put("generatedAt", LocalDateTime.now());
        
        return statistics;
    }
}