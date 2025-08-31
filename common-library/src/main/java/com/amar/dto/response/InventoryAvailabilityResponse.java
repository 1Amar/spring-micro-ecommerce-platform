package com.amar.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class InventoryAvailabilityResponse {
    
    @JsonProperty("productId")
    private Long productId;
    
    @JsonProperty("available")
    private Boolean available;
    
    @JsonProperty("availableQuantity")
    private Integer availableQuantity;
    
    @JsonProperty("requestedQuantity")
    private Integer requestedQuantity;
    
    @JsonProperty("totalQuantity")
    private Integer totalQuantity;
    
    @JsonProperty("reservedQuantity")
    private Integer reservedQuantity;
    
    @JsonProperty("stockStatus")
    private String stockStatus; // IN_STOCK, LOW_STOCK, OUT_OF_STOCK
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("suggestedQuantity")
    private Integer suggestedQuantity; // Maximum quantity that can be ordered
    
    @JsonProperty("estimatedRestockDate")
    private String estimatedRestockDate; // If known
    
    // For bulk availability checks
    @JsonProperty("bulkAvailability")
    private Map<Long, Boolean> bulkAvailability;
    
    @JsonProperty("bulkDetails")
    private Map<Long, ProductAvailabilityDetails> bulkDetails;

    // Default constructor
    public InventoryAvailabilityResponse() {
    }

    // Full constructor
    public InventoryAvailabilityResponse(Long productId, Boolean available, Integer availableQuantity, 
                                       Integer requestedQuantity, Integer totalQuantity, Integer reservedQuantity,
                                       String stockStatus, String message, Integer suggestedQuantity, 
                                       String estimatedRestockDate, Map<Long, Boolean> bulkAvailability,
                                       Map<Long, ProductAvailabilityDetails> bulkDetails) {
        this.productId = productId;
        this.available = available;
        this.availableQuantity = availableQuantity;
        this.requestedQuantity = requestedQuantity;
        this.totalQuantity = totalQuantity;
        this.reservedQuantity = reservedQuantity;
        this.stockStatus = stockStatus;
        this.message = message;
        this.suggestedQuantity = suggestedQuantity;
        this.estimatedRestockDate = estimatedRestockDate;
        this.bulkAvailability = bulkAvailability;
        this.bulkDetails = bulkDetails;
    }

    // Getters and setters
    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Boolean getAvailable() {
        return available;
    }

    public void setAvailable(Boolean available) {
        this.available = available;
    }

    public Integer getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(Integer availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    public Integer getRequestedQuantity() {
        return requestedQuantity;
    }

    public void setRequestedQuantity(Integer requestedQuantity) {
        this.requestedQuantity = requestedQuantity;
    }

    public Integer getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(Integer totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public Integer getReservedQuantity() {
        return reservedQuantity;
    }

    public void setReservedQuantity(Integer reservedQuantity) {
        this.reservedQuantity = reservedQuantity;
    }

    public String getStockStatus() {
        return stockStatus;
    }

    public void setStockStatus(String stockStatus) {
        this.stockStatus = stockStatus;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getSuggestedQuantity() {
        return suggestedQuantity;
    }

    public void setSuggestedQuantity(Integer suggestedQuantity) {
        this.suggestedQuantity = suggestedQuantity;
    }

    public String getEstimatedRestockDate() {
        return estimatedRestockDate;
    }

    public void setEstimatedRestockDate(String estimatedRestockDate) {
        this.estimatedRestockDate = estimatedRestockDate;
    }

    public Map<Long, Boolean> getBulkAvailability() {
        return bulkAvailability;
    }

    public void setBulkAvailability(Map<Long, Boolean> bulkAvailability) {
        this.bulkAvailability = bulkAvailability;
    }

    public Map<Long, ProductAvailabilityDetails> getBulkDetails() {
        return bulkDetails;
    }

    public void setBulkDetails(Map<Long, ProductAvailabilityDetails> bulkDetails) {
        this.bulkDetails = bulkDetails;
    }
    
    public static class ProductAvailabilityDetails {
        @JsonProperty("available")
        private Boolean available;
        
        @JsonProperty("availableQuantity")
        private Integer availableQuantity;
        
        @JsonProperty("requestedQuantity")
        private Integer requestedQuantity;
        
        @JsonProperty("stockStatus")
        private String stockStatus;
        
        @JsonProperty("message")
        private String message;

        // Default constructor
        public ProductAvailabilityDetails() {
        }

        // Full constructor
        public ProductAvailabilityDetails(Boolean available, Integer availableQuantity, Integer requestedQuantity,
                                        String stockStatus, String message) {
            this.available = available;
            this.availableQuantity = availableQuantity;
            this.requestedQuantity = requestedQuantity;
            this.stockStatus = stockStatus;
            this.message = message;
        }

        // Getters and setters
        public Boolean getAvailable() {
            return available;
        }

        public void setAvailable(Boolean available) {
            this.available = available;
        }

        public Integer getAvailableQuantity() {
            return availableQuantity;
        }

        public void setAvailableQuantity(Integer availableQuantity) {
            this.availableQuantity = availableQuantity;
        }

        public Integer getRequestedQuantity() {
            return requestedQuantity;
        }

        public void setRequestedQuantity(Integer requestedQuantity) {
            this.requestedQuantity = requestedQuantity;
        }

        public String getStockStatus() {
            return stockStatus;
        }

        public void setStockStatus(String stockStatus) {
            this.stockStatus = stockStatus;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}