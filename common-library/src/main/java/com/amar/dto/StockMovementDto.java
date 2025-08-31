package com.amar.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.UUID;

public class StockMovementDto {

    @JsonProperty("id")
    private UUID id;

    @JsonProperty("productId")
    private Long productId;

    @JsonProperty("quantityChange")
    private Integer quantityChange;

    @JsonProperty("movementType")
    private String movementType;

    @JsonProperty("referenceId")
    private UUID referenceId;

    @JsonProperty("referenceType")
    private String referenceType;

    @JsonProperty("reason")
    private String reason;

    @JsonProperty("notes")
    private String notes;

    @JsonProperty("performedBy")
    private String performedBy;

    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    // Constructors
    public StockMovementDto() {}

    public StockMovementDto(UUID id, Long productId, Integer quantityChange, String movementType,
                           UUID referenceId, String referenceType, String reason, String notes,
                           String performedBy, LocalDateTime createdAt) {
        this.id = id;
        this.productId = productId;
        this.quantityChange = quantityChange;
        this.movementType = movementType;
        this.referenceId = referenceId;
        this.referenceType = referenceType;
        this.reason = reason;
        this.notes = notes;
        this.performedBy = performedBy;
        this.createdAt = createdAt;
    }

    // Getters and setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

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

    public String getMovementType() {
        return movementType;
    }

    public void setMovementType(String movementType) {
        this.movementType = movementType;
    }

    public UUID getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(UUID referenceId) {
        this.referenceId = referenceId;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getPerformedBy() {
        return performedBy;
    }

    public void setPerformedBy(String performedBy) {
        this.performedBy = performedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}