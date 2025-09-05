package com.amar.dto;

import jakarta.validation.constraints.*;

public class UpdateOrderStatusRequest {
    
    @NotBlank(message = "Status is required")
    private String status;
    
    private String reason;
    private String adminNotes;
    private String trackingNumber;
    private String carrier;
    
    // Constructors
    public UpdateOrderStatusRequest() {}
    
    public UpdateOrderStatusRequest(String status) {
        this.status = status;
    }
    
    public UpdateOrderStatusRequest(String status, String reason) {
        this.status = status;
        this.reason = reason;
    }
    
    // Getters and Setters
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
    
    public String getAdminNotes() {
        return adminNotes;
    }
    
    public void setAdminNotes(String adminNotes) {
        this.adminNotes = adminNotes;
    }
    
    public String getTrackingNumber() {
        return trackingNumber;
    }
    
    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }
    
    public String getCarrier() {
        return carrier;
    }
    
    public void setCarrier(String carrier) {
        this.carrier = carrier;
    }
}