package com.amar.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class StockAdjustmentRequest {

    @JsonProperty("adjustments")
    @Valid
    @NotEmpty(message = "Adjustments list cannot be empty")
    private List<AdjustmentItem> adjustments;

    @JsonProperty("reason")
    @NotNull(message = "Reason is required")
    private String reason;

    @JsonProperty("performedBy")
    @NotNull(message = "Performed by is required")
    private String performedBy;

    @JsonProperty("notes")
    private String notes;

    // Constructors
    public StockAdjustmentRequest() {}

    public StockAdjustmentRequest(List<AdjustmentItem> adjustments, String reason, String performedBy, String notes) {
        this.adjustments = adjustments;
        this.reason = reason;
        this.performedBy = performedBy;
        this.notes = notes;
    }

    // Getters and setters
    public List<AdjustmentItem> getAdjustments() {
        return adjustments;
    }

    public void setAdjustments(List<AdjustmentItem> adjustments) {
        this.adjustments = adjustments;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getPerformedBy() {
        return performedBy;
    }

    public void setPerformedBy(String performedBy) {
        this.performedBy = performedBy;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public static class AdjustmentItem {
        @JsonProperty("productId")
        @NotNull(message = "Product ID is required")
        private Long productId;

        @JsonProperty("quantityChange")
        @NotNull(message = "Quantity change is required")
        private Integer quantityChange;

        @JsonProperty("newReorderLevel")
        private Integer newReorderLevel;

        @JsonProperty("newMaxStockLevel")
        private Integer newMaxStockLevel;

        @JsonProperty("reason")
        private String reason;

        // Constructors
        public AdjustmentItem() {}

        public AdjustmentItem(Long productId, Integer quantityChange) {
            this.productId = productId;
            this.quantityChange = quantityChange;
        }

        public AdjustmentItem(Long productId, Integer quantityChange, Integer newReorderLevel, Integer newMaxStockLevel, String reason) {
            this.productId = productId;
            this.quantityChange = quantityChange;
            this.newReorderLevel = newReorderLevel;
            this.newMaxStockLevel = newMaxStockLevel;
            this.reason = reason;
        }

        // Getters and setters
        public Long getProductId() {
            return productId;
        }

        public void setProductId(Long productId) {
            this.productId = productId;
        }

        public Integer getQuantityChange() {
            return quantityChange;
        }

        public void setQuantityChange(Integer quantityChange) {
            this.quantityChange = quantityChange;
        }

        public Integer getNewReorderLevel() {
            return newReorderLevel;
        }

        public void setNewReorderLevel(Integer newReorderLevel) {
            this.newReorderLevel = newReorderLevel;
        }

        public Integer getNewMaxStockLevel() {
            return newMaxStockLevel;
        }

        public void setNewMaxStockLevel(Integer newMaxStockLevel) {
            this.newMaxStockLevel = newMaxStockLevel;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }
}