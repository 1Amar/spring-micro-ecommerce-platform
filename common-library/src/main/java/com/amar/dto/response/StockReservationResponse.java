package com.amar.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

public class StockReservationResponse {
    
    @JsonProperty("success")
    private Boolean success;
    
    @JsonProperty("orderId")
    private UUID orderId;
    
    @JsonProperty("reservations")
    private List<ReservationItem> reservations;
    
    @JsonProperty("expiresAt")
    private LocalDateTime expiresAt;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("errors")
    private List<ReservationError> errors;
    
    @JsonProperty("totalItemsReserved")
    private Integer totalItemsReserved;
    
    @JsonProperty("totalQuantityReserved")
    private Integer totalQuantityReserved;
    
    @JsonProperty("partialReservation")
    private Boolean partialReservation; // True if some items couldn't be reserved

    // Default constructor
    public StockReservationResponse() {
    }

    // Full constructor
    public StockReservationResponse(Boolean success, UUID orderId, List<ReservationItem> reservations, 
                                  LocalDateTime expiresAt, String message, List<ReservationError> errors, 
                                  Integer totalItemsReserved, Integer totalQuantityReserved, Boolean partialReservation) {
        this.success = success;
        this.orderId = orderId;
        this.reservations = reservations;
        this.expiresAt = expiresAt;
        this.message = message;
        this.errors = errors;
        this.totalItemsReserved = totalItemsReserved;
        this.totalQuantityReserved = totalQuantityReserved;
        this.partialReservation = partialReservation;
    }

    // Getters and setters
    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public List<ReservationItem> getReservations() {
        return reservations;
    }

    public void setReservations(List<ReservationItem> reservations) {
        this.reservations = reservations;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<ReservationError> getErrors() {
        return errors;
    }

    public void setErrors(List<ReservationError> errors) {
        this.errors = errors;
    }

    public Integer getTotalItemsReserved() {
        return totalItemsReserved;
    }

    public void setTotalItemsReserved(Integer totalItemsReserved) {
        this.totalItemsReserved = totalItemsReserved;
    }

    public Integer getTotalQuantityReserved() {
        return totalQuantityReserved;
    }

    public void setTotalQuantityReserved(Integer totalQuantityReserved) {
        this.totalQuantityReserved = totalQuantityReserved;
    }

    public Boolean getPartialReservation() {
        return partialReservation;
    }

    public void setPartialReservation(Boolean partialReservation) {
        this.partialReservation = partialReservation;
    }
    
    public static class ReservationError {
        @JsonProperty("productId")
        private Long productId;
        
        @JsonProperty("requestedQuantity")
        private Integer requestedQuantity;
        
        @JsonProperty("availableQuantity")
        private Integer availableQuantity;
        
        @JsonProperty("errorCode")
        private String errorCode; // INSUFFICIENT_STOCK, PRODUCT_NOT_FOUND, etc.
        
        @JsonProperty("errorMessage")
        private String errorMessage;

        // Default constructor
        public ReservationError() {
        }

        // Full constructor
        public ReservationError(Long productId, Integer requestedQuantity, Integer availableQuantity, 
                              String errorCode, String errorMessage) {
            this.productId = productId;
            this.requestedQuantity = requestedQuantity;
            this.availableQuantity = availableQuantity;
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
        }

        // Getters and setters
        public Long getProductId() {
            return productId;
        }

        public void setProductId(Long productId) {
            this.productId = productId;
        }

        public Integer getRequestedQuantity() {
            return requestedQuantity;
        }

        public void setRequestedQuantity(Integer requestedQuantity) {
            this.requestedQuantity = requestedQuantity;
        }

        public Integer getAvailableQuantity() {
            return availableQuantity;
        }

        public void setAvailableQuantity(Integer availableQuantity) {
            this.availableQuantity = availableQuantity;
        }

        public String getErrorCode() {
            return errorCode;
        }

        public void setErrorCode(String errorCode) {
            this.errorCode = errorCode;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }
    
    public static class ReservationItem {
        @JsonProperty("productId")
        private Long productId;
        
        @JsonProperty("quantity")
        private Integer quantity;
        
        @JsonProperty("reservedQuantity")
        private Integer reservedQuantity;
        
        @JsonProperty("status")
        private String status;
        
        public ReservationItem() {}
        
        public ReservationItem(Long productId, Integer quantity, Integer reservedQuantity, String status) {
            this.productId = productId;
            this.quantity = quantity;
            this.reservedQuantity = reservedQuantity;
            this.status = status;
        }
        
        public Long getProductId() {
            return productId;
        }
        
        public void setProductId(Long productId) {
            this.productId = productId;
        }
        
        public Integer getQuantity() {
            return quantity;
        }
        
        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
        
        public Integer getReservedQuantity() {
            return reservedQuantity;
        }
        
        public void setReservedQuantity(Integer reservedQuantity) {
            this.reservedQuantity = reservedQuantity;
        }
        
        public String getStatus() {
            return status;
        }
        
        public void setStatus(String status) {
            this.status = status;
        }
    }
}