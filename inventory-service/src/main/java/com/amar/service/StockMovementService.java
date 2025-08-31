package com.amar.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.amar.dto.StockMovementDto;
import com.amar.entity.StockMovement;
import com.amar.mapper.StockMovementMapper;
import com.amar.repository.StockMovementRepository;

@Service
@Transactional
public class StockMovementService {

    private static final Logger logger = LoggerFactory.getLogger(StockMovementService.class);

    private final StockMovementRepository stockMovementRepository;
    private final StockMovementMapper stockMovementMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    public StockMovementService(StockMovementRepository stockMovementRepository,
                               StockMovementMapper stockMovementMapper,
                               KafkaTemplate<String, Object> kafkaTemplate) {
        this.stockMovementRepository = stockMovementRepository;
        this.stockMovementMapper = stockMovementMapper;
        this.kafkaTemplate = kafkaTemplate;
    }

    // =====================================================
    // Stock Movement Recording Methods
    // =====================================================

    public void recordInbound(Long productId, Integer quantity, UUID referenceId, 
                             String referenceType, String reason, String performedBy) {
        logger.debug("Recording inbound movement for product ID: {} quantity: {}", productId, quantity);
        
        StockMovement movement = createMovement(
            productId, quantity, StockMovement.MovementType.INBOUND,
            referenceId, referenceType, reason, performedBy
        );
        
        stockMovementRepository.save(movement);
        publishMovementEvent("stock.movement.inbound", movement);
    }

    public void recordOutbound(Long productId, Integer quantity, UUID referenceId, 
                              String referenceType, String reason, String performedBy) {
        logger.debug("Recording outbound movement for product ID: {} quantity: {}", productId, quantity);
        
        StockMovement movement = createMovement(
            productId, -Math.abs(quantity), StockMovement.MovementType.OUTBOUND,
            referenceId, referenceType, reason, performedBy
        );
        
        stockMovementRepository.save(movement);
        publishMovementEvent("stock.movement.outbound", movement);
    }

    public void recordAdjustment(Long productId, Integer quantityChange, String reason, 
                                String performedBy, String notes) {
        logger.debug("Recording adjustment for product ID: {} change: {}", productId, quantityChange);
        
        StockMovement movement = createMovement(
            productId, quantityChange, StockMovement.MovementType.ADJUSTMENT,
            null, "ADJUSTMENT", reason, performedBy
        );
        movement.setNotes(notes);
        
        stockMovementRepository.save(movement);
        publishMovementEvent("stock.movement.adjustment", movement);
    }

    public void recordReservation(Long productId, Integer quantity, UUID orderId, String performedBy) {
        logger.debug("Recording reservation movement for product ID: {} quantity: {}", productId, quantity);
        
        StockMovement movement = createMovement(
            productId, quantity, StockMovement.MovementType.RESERVED,
            orderId, "ORDER", "Stock reserved for order", performedBy
        );
        movement.setNotes("Reserved " + quantity + " units for order: " + orderId);
        
        stockMovementRepository.save(movement);
        publishMovementEvent("stock.movement.reservation", movement);
    }

    public void recordReservationRelease(Long productId, Integer quantity, UUID orderId, String performedBy) {
        logger.debug("Recording reservation release for product ID: {} quantity: {}", productId, quantity);
        
        StockMovement movement = createMovement(
            productId, quantity, StockMovement.MovementType.RESERVATION_RELEASED,
            orderId, "ORDER", "Stock reservation released", performedBy
        );
        movement.setNotes("Released " + quantity + " units from order: " + orderId);
        
        stockMovementRepository.save(movement);
        publishMovementEvent("stock.movement.reservation.released", movement);
    }

    public void recordTransfer(Long productId, Integer quantity, String reason, String performedBy, String notes) {
        logger.debug("Recording transfer for product ID: {} quantity: {}", productId, quantity);
        
        StockMovement movement = createMovement(
            productId, quantity, StockMovement.MovementType.TRANSFER,
            null, "TRANSFER", reason, performedBy
        );
        movement.setNotes(notes);
        
        stockMovementRepository.save(movement);
        publishMovementEvent("stock.movement.transfer", movement);
    }

    // =====================================================
    // Stock Movement Query Methods
    // =====================================================

    @Transactional(readOnly = true)
    public List<StockMovementDto> getMovementsForProduct(Long productId) {
        List<StockMovement> movements = stockMovementRepository.findByProductIdOrderByCreatedAtDesc(productId);
        return stockMovementMapper.toDtoList(movements);
    }

    @Transactional(readOnly = true)
    public Page<StockMovementDto> getMovementsForProduct(Long productId, Pageable pageable) {
        Page<StockMovement> movements = stockMovementRepository.findByProductIdOrderByCreatedAtDesc(productId, pageable);
        return movements.map(stockMovementMapper::toDto);
    }

    @Transactional(readOnly = true)
    public List<StockMovementDto> getMovementsByType(StockMovement.MovementType movementType) {
        List<StockMovement> movements = stockMovementRepository.findByMovementTypeOrderByCreatedAtDesc(movementType);
        return stockMovementMapper.toDtoList(movements);
    }

    @Transactional(readOnly = true)
    public Page<StockMovementDto> getMovementsByType(StockMovement.MovementType movementType, Pageable pageable) {
        Page<StockMovement> movements = stockMovementRepository.findByMovementTypeOrderByCreatedAtDesc(movementType, pageable);
        return movements.map(stockMovementMapper::toDto);
    }

    @Transactional(readOnly = true)
    public List<StockMovementDto> getMovementsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        List<StockMovement> movements = stockMovementRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(startDate, endDate);
        return stockMovementMapper.toDtoList(movements);
    }

    @Transactional(readOnly = true)
    public Page<StockMovementDto> getMovementsByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        Page<StockMovement> movements = stockMovementRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(startDate, endDate, pageable);
        return movements.map(stockMovementMapper::toDto);
    }

    @Transactional(readOnly = true)
    public List<StockMovementDto> getMovementsForOrder(UUID orderId) {
        List<StockMovement> movements = stockMovementRepository.findByReferenceId(orderId);
        return stockMovementMapper.toDtoList(movements);
    }

    @Transactional(readOnly = true)
    public List<StockMovementDto> getMovementsByUser(String performedBy) {
        List<StockMovement> movements = stockMovementRepository.findByPerformedByOrderByCreatedAtDesc(performedBy);
        return stockMovementMapper.toDtoList(movements);
    }

    @Transactional(readOnly = true)
    public List<StockMovementDto> getRecentMovements(Pageable pageable) {
        List<StockMovement> movements = stockMovementRepository.findRecentMovements(pageable);
        return stockMovementMapper.toDtoList(movements);
    }

    // =====================================================
    // Movement Analytics Methods
    // =====================================================

    @Transactional(readOnly = true)
    public Map<String, Object> getMovementStatisticsForProduct(Long productId, LocalDateTime startDate, LocalDateTime endDate) {
        Object[] stats = stockMovementRepository.getMovementStatisticsForProduct(productId, startDate, endDate);
        
        return Map.of(
            "totalMovements", stats[0],
            "totalInbound", stats[1],
            "totalOutbound", stats[2],
            "netChange", stats[3],
            "productId", productId,
            "periodStart", startDate,
            "periodEnd", endDate
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getGlobalMovementStatistics(LocalDateTime startDate, LocalDateTime endDate) {
        Object[] stats = stockMovementRepository.getGlobalMovementStatistics(startDate, endDate);
        
        return Map.of(
            "totalMovements", stats[0],
            "uniqueProducts", stats[1],
            "totalInbound", stats[2],
            "totalOutbound", stats[3],
            "periodStart", startDate,
            "periodEnd", endDate
        );
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMostActiveProducts(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        List<Object[]> results = stockMovementRepository.findMostActiveProducts(startDate, endDate, pageable);
        
        return results.stream()
            .map(row -> Map.of(
                "productId", row[0],
                "movementCount", row[1]
            ))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMovementCountByType(LocalDateTime startDate, LocalDateTime endDate) {
        List<Object[]> results = stockMovementRepository.getMovementCountByType(startDate, endDate);
        
        return results.stream()
            .map(row -> Map.of(
                "movementType", row[0].toString(),
                "count", row[1]
            ))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getDailyMovementSummary(LocalDateTime startDate, LocalDateTime endDate) {
        List<Object[]> results = stockMovementRepository.getDailyMovementSummary(startDate, endDate);
        
        return results.stream()
            .map(row -> Map.of(
                "date", row[0],
                "totalMovements", row[1],
                "totalInbound", row[2],
                "totalOutbound", row[3]
            ))
            .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getSystemVsUserMovements(LocalDateTime startDate, LocalDateTime endDate) {
        Object[] stats = stockMovementRepository.getSystemVsUserMovements(startDate, endDate);
        
        return Map.of(
            "systemMovements", stats[0],
            "userMovements", stats[1],
            "totalMovements", ((Number) stats[0]).longValue() + ((Number) stats[1]).longValue(),
            "periodStart", startDate,
            "periodEnd", endDate
        );
    }

    @Transactional(readOnly = true)
    public List<StockMovementDto> searchMovements(String searchText) {
        List<StockMovement> movements = stockMovementRepository.findMovementsByNotesOrReason(searchText);
        return stockMovementMapper.toDtoList(movements);
    }

    @Transactional(readOnly = true)
    public List<StockMovementDto> getLargeMovements(Integer threshold) {
        List<StockMovement> movements = stockMovementRepository.findLargeMovements(threshold);
        return stockMovementMapper.toDtoList(movements);
    }

    // =====================================================
    // Helper Methods
    // =====================================================

    private StockMovement createMovement(Long productId, Integer quantity, StockMovement.MovementType movementType,
                                        UUID referenceId, String referenceType, String reason, String performedBy) {
        StockMovement movement = new StockMovement();
        movement.setProductId(productId);
        movement.setQuantityChange(quantity);
        movement.setMovementType(movementType);
        movement.setReferenceId(referenceId);
        movement.setReferenceType(referenceType);
        movement.setReason(reason);
        movement.setPerformedBy(performedBy);
        return movement;
    }

    private void publishMovementEvent(String eventType, StockMovement movement) {
        try {
            Map<String, Object> event = Map.of(
                "eventType", eventType,
                "productId", movement.getProductId(),
                "quantityChange", movement.getQuantityChange(),
                "movementType", movement.getMovementType().toString(),
                "referenceId", movement.getReferenceId(),
                "referenceType", movement.getReferenceType(),
                "reason", movement.getReason(),
                "performedBy", movement.getPerformedBy(),
                "timestamp", movement.getCreatedAt() != null ? movement.getCreatedAt() : LocalDateTime.now()
            );
            
            kafkaTemplate.send("inventory-movement-events", movement.getProductId().toString(), event);
            logger.debug("Published movement event: {} for product ID: {}", eventType, movement.getProductId());
        } catch (Exception ex) {
            logger.error("Failed to publish movement event for product ID: {}", movement.getProductId(), ex);
        }
    }
    
    // Additional methods needed by controllers - using existing methods with different signatures
    public List<StockMovementDto> getMovementsForProductPaged(Long productId, Pageable pageable) {
        logger.debug("Getting movements for product: {} with pagination", productId);
        List<StockMovement> movements = stockMovementRepository.findByProductIdOrderByCreatedAtDesc(productId);
        return movements.stream()
            .skip(pageable.getOffset())
            .limit(pageable.getPageSize())
            .map(stockMovementMapper::toDto)
            .collect(Collectors.toList());
    }
    
    public List<StockMovementDto> getMovementsByDateRangePaged(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        logger.debug("Getting movements between {} and {} with pagination", startDate, endDate);
        List<StockMovement> movements = stockMovementRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(startDate, endDate);
        return movements.stream()
            .skip(pageable.getOffset())
            .limit(pageable.getPageSize())
            .map(stockMovementMapper::toDto)
            .collect(Collectors.toList());
    }
    
    public List<StockMovementDto> getMovementsByReferenceId(UUID referenceId) {
        logger.debug("Getting movements for reference ID: {}", referenceId);
        List<StockMovement> movements = stockMovementRepository.findByReferenceIdOrderByCreatedAtDesc(referenceId);
        return movements.stream()
            .map(stockMovementMapper::toDto)
            .collect(Collectors.toList());
    }
    
    public Map<String, Object> getMovementStatistics(Long productId, LocalDateTime startDate, LocalDateTime endDate) {
        logger.debug("Getting movement statistics for product: {}, date range: {} to {}", productId, startDate, endDate);
        
        Map<String, Object> statistics = new HashMap<>();
        
        if (productId != null && startDate != null && endDate != null) {
            // Product-specific statistics for date range
            Object[] stats = stockMovementRepository.getMovementStatisticsForProduct(productId, startDate, endDate);
            if (stats != null && stats.length >= 4) {
                statistics.put("totalMovements", stats[0]);
                statistics.put("totalInbound", stats[1]);
                statistics.put("totalOutbound", stats[2]);
                statistics.put("netChange", stats[3]);
                statistics.put("productId", productId);
            }
        } else if (startDate != null && endDate != null) {
            // Global statistics for date range
            Object[] stats = stockMovementRepository.getGlobalMovementStatistics(startDate, endDate);
            if (stats != null && stats.length >= 4) {
                statistics.put("totalMovements", stats[0]);
                statistics.put("uniqueProducts", stats[1]);
                statistics.put("totalInbound", stats[2]);
                statistics.put("totalOutbound", stats[3]);
            }
        } else {
            // Basic counts
            long totalMovements = stockMovementRepository.count();
            statistics.put("totalMovements", totalMovements);
        }
        
        statistics.put("generatedAt", LocalDateTime.now());
        return statistics;
    }
}