package com.amar.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for real-time order status updates sent via WebSocket
 */
public class OrderStatusUpdate {
    
    private UUID orderId;
    private String orderNumber;
    private String userId;
    private String previousStatus;
    private String newStatus;
    private String reason;
    private String changedBy;
    private LocalDateTime timestamp;
    private String paymentStatus;
    private String trackingNumber;
    private String carrier;
    private OrderProgressInfo progressInfo;

    // Constructors
    public OrderStatusUpdate() {
        this.timestamp = LocalDateTime.now();
    }

    public OrderStatusUpdate(UUID orderId, String orderNumber, String userId, 
                           String previousStatus, String newStatus, String reason) {
        this();
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.userId = userId;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.reason = reason;
        this.changedBy = "SYSTEM";
    }

    // Getters and Setters
    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPreviousStatus() {
        return previousStatus;
    }

    public void setPreviousStatus(String previousStatus) {
        this.previousStatus = previousStatus;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(String newStatus) {
        this.newStatus = newStatus;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(String changedBy) {
        this.changedBy = changedBy;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
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

    public OrderProgressInfo getProgressInfo() {
        return progressInfo;
    }

    public void setProgressInfo(OrderProgressInfo progressInfo) {
        this.progressInfo = progressInfo;
    }

    /**
     * Nested class for order progress information
     */
    public static class OrderProgressInfo {
        private int currentStep;
        private int totalSteps;
        private String currentStepName;
        private String nextStepName;
        private LocalDateTime estimatedCompletion;

        // Constructors
        public OrderProgressInfo() {}

        public OrderProgressInfo(int currentStep, int totalSteps, String currentStepName) {
            this.currentStep = currentStep;
            this.totalSteps = totalSteps;
            this.currentStepName = currentStepName;
        }

        // Getters and Setters
        public int getCurrentStep() {
            return currentStep;
        }

        public void setCurrentStep(int currentStep) {
            this.currentStep = currentStep;
        }

        public int getTotalSteps() {
            return totalSteps;
        }

        public void setTotalSteps(int totalSteps) {
            this.totalSteps = totalSteps;
        }

        public String getCurrentStepName() {
            return currentStepName;
        }

        public void setCurrentStepName(String currentStepName) {
            this.currentStepName = currentStepName;
        }

        public String getNextStepName() {
            return nextStepName;
        }

        public void setNextStepName(String nextStepName) {
            this.nextStepName = nextStepName;
        }

        public LocalDateTime getEstimatedCompletion() {
            return estimatedCompletion;
        }

        public void setEstimatedCompletion(LocalDateTime estimatedCompletion) {
            this.estimatedCompletion = estimatedCompletion;
        }
    }

    @Override
    public String toString() {
        return "OrderStatusUpdate{" +
                "orderId=" + orderId +
                ", orderNumber='" + orderNumber + '\'' +
                ", userId='" + userId + '\'' +
                ", previousStatus='" + previousStatus + '\'' +
                ", newStatus='" + newStatus + '\'' +
                ", reason='" + reason + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}