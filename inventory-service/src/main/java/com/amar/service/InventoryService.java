package com.amar.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import com.amar.dto.InventoryReservationDto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.amar.dto.InventoryDto;
import com.amar.dto.request.StockAdjustmentRequest;
import com.amar.dto.request.StockReservationRequest;
import com.amar.dto.response.InventoryAvailabilityResponse;
import com.amar.dto.response.StockReservationResponse;
import com.amar.entity.inventory.Inventory;
// InventoryMapper removed - using manual mapping
import com.amar.repository.InventoryRepository;
import com.amar.kafka.InventoryEventPublisher;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

@Service
@Transactional
public class InventoryService {

    private static final Logger logger = LoggerFactory.getLogger(InventoryService.class);

    private final InventoryRepository inventoryRepository;
    // InventoryMapper removed - using manual mapping
    private final StockReservationService stockReservationService;
    private final StockMovementService stockMovementService;
    private final ProductValidationService productValidationService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final InventoryEventPublisher eventPublisher;

    @Autowired
    public InventoryService(InventoryRepository inventoryRepository,
                           StockReservationService stockReservationService,
                           StockMovementService stockMovementService,
                           ProductValidationService productValidationService,
                           KafkaTemplate<String, Object> kafkaTemplate,
                           InventoryEventPublisher eventPublisher) {
        this.inventoryRepository = inventoryRepository;
        this.stockReservationService = stockReservationService;
        this.stockMovementService = stockMovementService;
        this.productValidationService = productValidationService;
        this.kafkaTemplate = kafkaTemplate;
        this.eventPublisher = eventPublisher;
    }

    // =====================================================
    // Core Inventory Management
    // =====================================================

    @Cacheable(value = "inventory", key = "#productId")
    @Transactional(readOnly = true)
    public Optional<InventoryDto> getInventoryByProductId(Long productId) {
        logger.debug("Getting inventory for product ID: {}", productId);
        
        Optional<Inventory> inventory = inventoryRepository.findByProductId(productId);
        return inventory.map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public List<InventoryDto> getInventoryForProducts(List<Long> productIds) {
        logger.debug("Getting inventory for {} products", productIds.size());
        
        List<Inventory> inventories = inventoryRepository.findByProductIdIn(productIds);
        return inventories.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @CacheEvict(value = "inventory", key = "#productId")
    public InventoryDto createOrUpdateInventory(Long productId, Integer quantity, 
                                               Integer reorderLevel, Integer maxStockLevel) {
        logger.info("Creating/updating inventory for product ID: {} with quantity: {}", productId, quantity);
        
        // Validate product exists
        if (!productValidationService.validateProductExists(productId)) {
            throw new IllegalArgumentException("Product with ID " + productId + " does not exist");
        }

        Optional<Inventory> existingInventory = inventoryRepository.findByProductId(productId);
        
        Inventory inventory;
        if (existingInventory.isPresent()) {
            inventory = existingInventory.get();
            Integer oldQuantity = inventory.getQuantity();
            inventory.setQuantity(quantity != null ? quantity : inventory.getQuantity());
            inventory.setReorderLevel(reorderLevel != null ? reorderLevel : inventory.getReorderLevel());
            inventory.setMaxStockLevel(maxStockLevel != null ? maxStockLevel : inventory.getMaxStockLevel());
            
            // Record stock movement for quantity changes
            if (quantity != null && !quantity.equals(oldQuantity)) {
                Integer quantityChange = quantity - oldQuantity;
                stockMovementService.recordAdjustment(productId, quantityChange, 
                    "Inventory update", "SYSTEM", "Inventory level adjustment");
            }
        } else {
            inventory = new Inventory();
            inventory.setProductId(productId);
            inventory.setQuantity(quantity != null ? quantity : 0);
            inventory.setReorderLevel(reorderLevel != null ? reorderLevel : 10);
            inventory.setMaxStockLevel(maxStockLevel != null ? maxStockLevel : 1000);
            inventory.setReservedQuantity(0);
            
            // Record initial stock movement
            if (quantity != null && quantity > 0) {
                stockMovementService.recordInbound(productId, quantity, null, 
                    "INITIAL_STOCK", "Initial inventory setup", "SYSTEM");
            }
        }

        inventory = inventoryRepository.save(inventory);
        
        // Publish inventory event
        publishInventoryEvent("inventory.updated", inventory);
        
        return mapToDto(inventory);
    }

    // =====================================================
    // Stock Availability Operations
    // =====================================================

    @Transactional(readOnly = true)
    public InventoryAvailabilityResponse checkAvailability(Long productId, Integer requestedQuantity) {
        logger.debug("Checking availability for product ID: {} quantity: {}", productId, requestedQuantity);
        
        Optional<Inventory> inventoryOpt = inventoryRepository.findByProductId(productId);
        
        if (inventoryOpt.isEmpty()) {
            InventoryAvailabilityResponse response = new InventoryAvailabilityResponse();
            response.setProductId(productId);
            response.setAvailable(false);
            response.setAvailableQuantity(0);
            response.setRequestedQuantity(requestedQuantity);
            response.setStockStatus("NOT_TRACKED");
            response.setMessage("Product inventory not tracked");
            return response;
        }

        Inventory inventory = inventoryOpt.get();
        Integer availableQuantity = inventory.getAvailableQuantity();
        boolean available = availableQuantity != null && availableQuantity >= requestedQuantity;
        
        InventoryAvailabilityResponse response = new InventoryAvailabilityResponse();
        response.setProductId(productId);
        response.setAvailable(available);
        response.setAvailableQuantity(availableQuantity);
        response.setRequestedQuantity(requestedQuantity);
        response.setTotalQuantity(inventory.getQuantity());
        response.setReservedQuantity(inventory.getReservedQuantity());
        response.setStockStatus(inventory.getStockStatus());
        response.setMessage(available ? "Stock available" : "Insufficient stock");
        response.setSuggestedQuantity(available ? requestedQuantity : availableQuantity);
        return response;
    }

    @Transactional(readOnly = true)
    public InventoryAvailabilityResponse bulkCheckAvailability(Map<Long, Integer> productQuantityMap) {
        logger.debug("Bulk checking availability for {} products", productQuantityMap.size());
        
        Map<Long, Boolean> bulkAvailability = productQuantityMap.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> {
                    InventoryAvailabilityResponse response = checkAvailability(entry.getKey(), entry.getValue());
                    return response.getAvailable();
                }
            ));

        Map<Long, InventoryAvailabilityResponse.ProductAvailabilityDetails> bulkDetails = 
            productQuantityMap.entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> {
                        InventoryAvailabilityResponse response = checkAvailability(entry.getKey(), entry.getValue());
                        InventoryAvailabilityResponse.ProductAvailabilityDetails details = 
                            new InventoryAvailabilityResponse.ProductAvailabilityDetails();
                        details.setAvailable(response.getAvailable());
                        details.setAvailableQuantity(response.getAvailableQuantity());
                        details.setRequestedQuantity(response.getRequestedQuantity());
                        details.setStockStatus(response.getStockStatus());
                        details.setMessage(response.getMessage());
                        return details;
                    }
                ));

        InventoryAvailabilityResponse bulkResponse = new InventoryAvailabilityResponse();
        bulkResponse.setBulkAvailability(bulkAvailability);
        bulkResponse.setBulkDetails(bulkDetails);
        return bulkResponse;
    }

    // =====================================================
    // Stock Reservation Operations
    // =====================================================

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public StockReservationResponse reserveStock(StockReservationRequest request) {
        logger.info("Reserving stock for order ID: {} with {} items", request.getOrderId(), request.getItems().size());
        
        StockReservationResponse response = stockReservationService.createReservation(request);
        
        // Publish reservation events
        if (response.getSuccess() && response.getReservations() != null) {
            for (var item : request.getItems()) {
                eventPublisher.publishStockReserved(request.getOrderId(), item.getProductId(), 
                    item.getQuantity(), LocalDateTime.now().plusMinutes(15));
            }
        }
        
        return response;
    }

    public void commitReservation(UUID orderId) {
        logger.info("Committing stock reservation for order ID: {}", orderId);
        
        // Get reservation details before committing (for event publishing)
        List<InventoryReservationDto> reservations = stockReservationService.getReservationsForOrder(orderId);
        
        // Commit the reservations
        stockReservationService.commitReservation(orderId);
        
        // Publish commitment events for each reservation with null safety
        if (reservations != null && !reservations.isEmpty()) {
            for (InventoryReservationDto reservation : reservations) {
                if (reservation != null && reservation.getProductId() != null && reservation.getQuantityReserved() != null) {
                    eventPublisher.publishReservationCommitted(orderId, reservation.getProductId(), reservation.getQuantityReserved());
                } else {
                    logger.warn("Skipping event publication for null reservation or null values - Order ID: {}, Reservation: {}", 
                               orderId, reservation);
                }
            }
        } else {
            logger.warn("No reservations found for order ID: {} during commit event publication", orderId);
        }
    }

    public void releaseReservation(UUID orderId) {
        logger.info("Releasing stock reservation for order ID: {}", orderId);
        
        // Get reservation details before releasing (for event publishing)
        List<InventoryReservationDto> reservations = stockReservationService.getReservationsForOrder(orderId);
        
        // Release the reservations
        stockReservationService.releaseReservation(orderId);
        
        // Publish release events for each reservation with null safety
        if (reservations != null && !reservations.isEmpty()) {
            for (InventoryReservationDto reservation : reservations) {
                if (reservation != null && reservation.getProductId() != null && reservation.getQuantityReserved() != null) {
                    eventPublisher.publishReservationReleased(orderId, reservation.getProductId(), reservation.getQuantityReserved());
                } else {
                    logger.warn("Skipping event publication for null reservation or null values - Order ID: {}, Reservation: {}", 
                               orderId, reservation);
                }
            }
        } else {
            logger.warn("No reservations found for order ID: {} during release event publication", orderId);
        }
    }

    // =====================================================
    // Stock Movement Operations
    // =====================================================

    @CacheEvict(value = "inventory", key = "#productId")
    public InventoryDto addStock(Long productId, Integer quantity, String reason, String performedBy) {
        logger.info("Adding {} units to product ID: {}", quantity, productId);
        
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        Optional<Inventory> inventoryOpt = inventoryRepository.findByProductIdWithLock(productId);
        if (inventoryOpt.isEmpty()) {
            throw new IllegalArgumentException("Inventory not found for product ID: " + productId);
        }

        Inventory inventory = inventoryOpt.get();
        inventory.addStock(quantity);
        inventory = inventoryRepository.save(inventory);

        // Record stock movement
        stockMovementService.recordInbound(productId, quantity, null, "STOCK_ADD", 
            reason != null ? reason : "Stock addition", performedBy != null ? performedBy : "SYSTEM");

        // Publish inventory event
        publishInventoryEvent("inventory.stock.added", inventory);

        return mapToDto(inventory);
    }

    @CacheEvict(value = "inventory", key = "#productId")
    public InventoryDto removeStock(Long productId, Integer quantity, String reason, String performedBy) {
        logger.info("Removing {} units from product ID: {}", quantity, productId);
        
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        Optional<Inventory> inventoryOpt = inventoryRepository.findByProductIdWithLock(productId);
        if (inventoryOpt.isEmpty()) {
            throw new IllegalArgumentException("Inventory not found for product ID: " + productId);
        }

        Inventory inventory = inventoryOpt.get();
        if (!inventory.removeStock(quantity)) {
            throw new IllegalStateException("Insufficient stock. Available: " + inventory.getQuantity() + ", Requested: " + quantity);
        }

        inventory = inventoryRepository.save(inventory);

        // Record stock movement
        stockMovementService.recordOutbound(productId, quantity, null, "STOCK_REMOVE", 
            reason != null ? reason : "Stock removal", performedBy != null ? performedBy : "SYSTEM");

        // Publish inventory event
        publishInventoryEvent("inventory.stock.removed", inventory);

        return mapToDto(inventory);
    }

    @Transactional
    public void processStockAdjustments(StockAdjustmentRequest request) {
        logger.info("Processing {} stock adjustments", request.getAdjustments().size());
        
        for (StockAdjustmentRequest.AdjustmentItem adjustment : request.getAdjustments()) {
            processStockAdjustment(adjustment, request.getReason(), request.getPerformedBy(), request.getNotes());
        }
    }

    @CacheEvict(value = "inventory", key = "#adjustment.productId")
    private void processStockAdjustment(StockAdjustmentRequest.AdjustmentItem adjustment, 
                                       String reason, String performedBy, String notes) {
        Optional<Inventory> inventoryOpt = inventoryRepository.findByProductIdWithLock(adjustment.getProductId());
        if (inventoryOpt.isEmpty()) {
            throw new IllegalArgumentException("Inventory not found for product ID: " + adjustment.getProductId());
        }

        Inventory inventory = inventoryOpt.get();
        
        // Apply quantity change
        if (adjustment.getQuantityChange() != 0) {
            if (adjustment.getQuantityChange() > 0) {
                inventory.addStock(adjustment.getQuantityChange());
            } else {
                if (!inventory.removeStock(Math.abs(adjustment.getQuantityChange()))) {
                    throw new IllegalStateException("Insufficient stock for adjustment. Product ID: " + adjustment.getProductId());
                }
            }
        }

        // Update reorder level if specified
        if (adjustment.getNewReorderLevel() != null) {
            inventory.setReorderLevel(adjustment.getNewReorderLevel());
        }

        // Update max stock level if specified
        if (adjustment.getNewMaxStockLevel() != null) {
            inventory.setMaxStockLevel(adjustment.getNewMaxStockLevel());
        }

        inventory = inventoryRepository.save(inventory);

        // Record stock movement if quantity changed
        if (adjustment.getQuantityChange() != 0) {
            stockMovementService.recordAdjustment(adjustment.getProductId(), adjustment.getQuantityChange(), 
                reason, performedBy, notes);
        }

        // Publish inventory event
        publishInventoryEvent("inventory.adjusted", inventory);
    }

    // =====================================================
    // Inventory Queries
    // =====================================================

    @Transactional(readOnly = true)
    public Page<InventoryDto> getLowStockProducts(Pageable pageable) {
        Page<Inventory> lowStockInventories = inventoryRepository.findLowStockProducts(pageable);
        return lowStockInventories.map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public Page<InventoryDto> getOutOfStockProducts(Pageable pageable) {
        Page<Inventory> outOfStockInventories = inventoryRepository.findOutOfStockProducts(pageable);
        return outOfStockInventories.map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public List<InventoryDto> getProductsNeedingReorder() {
        List<Inventory> inventories = inventoryRepository.findProductsNeedingReorder();
        return inventories.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getInventoryStatistics() {
        Object[] stats = inventoryRepository.getInventoryStatistics();
        
        return Map.of(
            "totalProducts", stats[0],
            "inStockProducts", stats[1],
            "lowStockProducts", stats[2],
            "outOfStockProducts", stats[3],
            "totalQuantity", stats[4],
            "totalReservedQuantity", stats[5],
            "totalAvailableQuantity", stats[6]
        );
    }

    // =====================================================
    // Circuit Breaker Methods
    // =====================================================

    @CircuitBreaker(name = "product-service", fallbackMethod = "fallbackProductValidation")
    @Retry(name = "product-service")
    public boolean validateProductExistsWithCircuitBreaker(Long productId) {
        return productValidationService.validateProductExists(productId);
    }

    private boolean fallbackProductValidation(Long productId, Exception ex) {
        logger.warn("Product validation failed for product ID: {}, using fallback", productId, ex);
        // In fallback, we assume product exists to not block inventory operations
        return true;
    }

    // =====================================================
    // Private Helper Methods
    // =====================================================

    private void publishInventoryEvent(String eventType, Inventory inventory) {
        try {
            // Use the dedicated event publisher with proper stock change events
            switch (eventType) {
                case "inventory.stock.added":
                    // We need to track old quantity for stock added events
                    eventPublisher.publishStockAdded(inventory.getProductId(), 
                        null, // quantity added - would need to track this
                        inventory.getQuantity(), "Stock addition");
                    break;
                case "inventory.stock.removed": 
                    eventPublisher.publishStockRemoved(inventory.getProductId(),
                        null, // quantity removed - would need to track this  
                        inventory.getQuantity(), "Stock removal");
                    break;
                case "inventory.updated":
                case "inventory.adjusted":
                    eventPublisher.publishStockUpdated(inventory.getProductId(),
                        null, // old quantity - would need to track this
                        inventory.getQuantity(),
                        inventory.getAvailableQuantity(),
                        inventory.getStockStatus());
                    break;
                default:
                    // Fallback to generic inventory event
                    eventPublisher.publishInventoryEvent(eventType, inventory.getProductId(), Map.of(
                        "quantity", inventory.getQuantity(),
                        "availableQuantity", inventory.getAvailableQuantity(),
                        "reservedQuantity", inventory.getReservedQuantity(),
                        "stockStatus", inventory.getStockStatus()
                    ));
            }
            logger.debug("Published inventory event: {} for product ID: {}", eventType, inventory.getProductId());
        } catch (Exception ex) {
            logger.error("Failed to publish inventory event for product ID: {}", inventory.getProductId(), ex);
        }
    }
    
    // Manual mapping method to replace InventoryMapper
    private InventoryDto mapToDto(Inventory inventory) {
        InventoryDto dto = new InventoryDto();
        dto.setId(inventory.getId());
        dto.setProductId(inventory.getProductId());
        dto.setQuantity(inventory.getQuantity() != null ? inventory.getQuantity() : 0);
        dto.setReservedQuantity(inventory.getReservedQuantity() != null ? inventory.getReservedQuantity() : 0);
        
        // Handle computed availableQuantity - calculate if null
        Integer availableQuantity = inventory.getAvailableQuantity();
        if (availableQuantity == null) {
            int quantity = inventory.getQuantity() != null ? inventory.getQuantity() : 0;
            int reserved = inventory.getReservedQuantity() != null ? inventory.getReservedQuantity() : 0;
            availableQuantity = Math.max(0, quantity - reserved);
        }
        dto.setAvailableQuantity(availableQuantity);
        
        dto.setReorderLevel(inventory.getReorderLevel() != null ? inventory.getReorderLevel() : 10);
        dto.setMaxStockLevel(inventory.getMaxStockLevel() != null ? inventory.getMaxStockLevel() : 1000);
        dto.setStockStatus(inventory.getStockStatus() != null ? inventory.getStockStatus() : "UNKNOWN");
        dto.setUpdatedAt(inventory.getUpdatedAt());
        return dto;
    }
}