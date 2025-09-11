package com.amar.controller;

import com.amar.dto.InventoryReservationDto;
import com.amar.dto.request.StockReservationRequest;
import com.amar.dto.response.StockReservationResponse;
import com.amar.service.StockReservationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventory/reservations")
@CrossOrigin(origins = "*")
public class StockReservationController {

    private static final Logger logger = LoggerFactory.getLogger(StockReservationController.class);

    private final StockReservationService stockReservationService;

    @Autowired
    public StockReservationController(StockReservationService stockReservationService) {
        this.stockReservationService = stockReservationService;
    }

    // Create new stock reservation
    @PostMapping("/reserve")
    public ResponseEntity<StockReservationResponse> reserveStock(@RequestBody StockReservationRequest request) {
        logger.info("Creating stock reservation for order: {}", request.getOrderId());
        
        try {
            StockReservationResponse response = stockReservationService.reserveStock(request);
            
            if (response.getSuccess()) {
                logger.info("Stock reservation successful for order: {}", request.getOrderId());
                return ResponseEntity.ok(response);
            } else {
                logger.warn("Stock reservation failed for order: {}", request.getOrderId());
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            }

        } catch (IllegalArgumentException ex) {
            logger.error("Invalid reservation request for order: {}", request.getOrderId(), ex);
            
            StockReservationResponse errorResponse = new StockReservationResponse();
            errorResponse.setSuccess(false);
            errorResponse.setOrderId(request.getOrderId());
            errorResponse.setMessage("Invalid request: " + ex.getMessage());
            
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception ex) {
            logger.error("Error creating reservation for order: {}", request.getOrderId(), ex);
            
            StockReservationResponse errorResponse = new StockReservationResponse();
            errorResponse.setSuccess(false);
            errorResponse.setOrderId(request.getOrderId());
            errorResponse.setMessage("Failed to create reservation: " + ex.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // Commit reservation (finalize order)
    @PostMapping("/{orderId}/commit")
    public ResponseEntity<Map<String, Object>> commitReservation(@PathVariable UUID orderId) {
        logger.info("Committing reservation for order: {}", orderId);
        
        try {
            stockReservationService.commitReservation(orderId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Reservation committed successfully");
            response.put("orderId", orderId != null ? orderId.toString() : null);
            response.put("committedAt", LocalDateTime.now().toString());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException ex) {
            logger.error("No reservation found for order: {}", orderId, ex);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Reservation not found: " + ex.getMessage());
            return ResponseEntity.notFound().build();

        } catch (Exception ex) {
            logger.error("Error committing reservation for order: {}", orderId, ex);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to commit reservation: " + (ex.getMessage() != null ? ex.getMessage() : "Unknown error"));
            errorResponse.put("orderId", orderId != null ? orderId.toString() : null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // Release reservation (cancel order)
    @PostMapping("/{orderId}/release")
    public ResponseEntity<Map<String, Object>> releaseReservation(@PathVariable UUID orderId) {
        logger.info("Releasing reservation for order: {}", orderId);
        
        try {
            stockReservationService.releaseReservation(orderId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Reservation released successfully");
            response.put("orderId", orderId);
            response.put("releasedAt", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException ex) {
            logger.error("No reservation found for order: {}", orderId, ex);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Reservation not found: " + ex.getMessage());
            return ResponseEntity.notFound().build();

        } catch (Exception ex) {
            logger.error("Error releasing reservation for order: {}", orderId, ex);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to release reservation: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // Get reservations for specific order
    @GetMapping("/order/{orderId}")
    public ResponseEntity<Map<String, Object>> getReservationsForOrder(@PathVariable UUID orderId) {
        logger.info("Getting reservations for order: {}", orderId);
        
        try {
            List<InventoryReservationDto> reservations = stockReservationService.getReservationsForOrder(orderId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", reservations);
            response.put("orderId", orderId);
            response.put("totalReservations", reservations.size());

            return ResponseEntity.ok(response);

        } catch (Exception ex) {
            logger.error("Error getting reservations for order: {}", orderId, ex);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to get reservations: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // Get active reservations for user/session
    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> getActiveReservationsForUser(
            @PathVariable String userId,
            @RequestParam(required = false) String sessionId) {
        
        logger.info("Getting active reservations for user: {} with session: {}", userId, sessionId);
        
        try {
            List<InventoryReservationDto> reservations = stockReservationService.getActiveReservationsForUser(userId, sessionId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", reservations);
            response.put("userId", userId);
            response.put("sessionId", sessionId);
            response.put("totalReservations", reservations.size());

            return ResponseEntity.ok(response);

        } catch (Exception ex) {
            logger.error("Error getting active reservations for user: {}", userId, ex);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to get reservations: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // Extend reservation TTL
    @PostMapping("/{reservationId}/extend")
    public ResponseEntity<Map<String, Object>> extendReservation(
            @PathVariable UUID reservationId,
            @RequestParam Integer additionalMinutes) {
        
        logger.info("Extending reservation: {} by {} minutes", reservationId, additionalMinutes);
        
        try {
            boolean extended = stockReservationService.extendReservation(reservationId, additionalMinutes);

            Map<String, Object> response = new HashMap<>();
            response.put("success", extended);
            response.put("message", extended ? "Reservation extended successfully" : "Reservation not found or already expired");
            response.put("reservationId", reservationId);
            response.put("additionalMinutes", additionalMinutes);
            response.put("extendedAt", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception ex) {
            logger.error("Error extending reservation: {}", reservationId, ex);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to extend reservation: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // Get reservation statistics
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getReservationStatistics() {
        logger.info("Getting reservation statistics");
        
        try {
            Map<String, Object> statistics = stockReservationService.getReservationStatistics();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", statistics);
            response.put("generatedAt", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception ex) {
            logger.error("Error getting reservation statistics", ex);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to get statistics: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // Cleanup expired reservations (admin function)
    @PostMapping("/cleanup-expired")
    public ResponseEntity<Map<String, Object>> cleanupExpiredReservations() {
        logger.info("Manually triggering cleanup of expired reservations");
        
        try {
            stockReservationService.cleanupExpiredReservations();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Expired reservations cleanup completed");
            response.put("cleanedAt", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception ex) {
            logger.error("Error during cleanup of expired reservations", ex);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to cleanup expired reservations: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // Health check
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "StockReservationController");
        response.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(response);
    }
}