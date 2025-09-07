package com.amar.service;

import com.amar.kafka.NotificationEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationEventPublisher eventPublisher;
    
    // In-memory storage for notification tracking (in real app, use database)
    private final Map<String, NotificationRecord> notifications = new ConcurrentHashMap<>();
    private final Map<String, List<String>> userNotifications = new ConcurrentHashMap<>();

    @Value("${notification.channels.email.enabled:true}")
    private boolean emailEnabled;

    @Value("${notification.channels.sms.enabled:true}")
    private boolean smsEnabled;

    @Value("${notification.channels.push.enabled:false}")
    private boolean pushEnabled;

    @Value("${notification.channels.email.simulation-mode:true}")
    private boolean emailSimulationMode;

    @Value("${notification.channels.sms.simulation-mode:true}")
    private boolean smsSimulationMode;

    @Value("${notification.channels.push.simulation-mode:true}")
    private boolean pushSimulationMode;

    @Autowired
    public NotificationService(NotificationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    // =====================================================
    // Main Notification Methods
    // =====================================================

    public NotificationResult sendNotification(String userId, String orderId, String notificationType, 
                                             String message, String channel) {
        String notificationId = UUID.randomUUID().toString();
        
        logger.info("Sending notification - ID: {}, User: {}, Order: {}, Type: {}, Channel: {}", 
            notificationId, userId, orderId, notificationType, channel);

        // Validate channel
        if (!isChannelEnabled(channel)) {
            logger.warn("Channel {} is disabled, skipping notification", channel);
            return new NotificationResult(false, "Channel disabled", notificationId, "SKIPPED");
        }

        // Create notification record
        NotificationRecord notification = new NotificationRecord(
            notificationId, userId, orderId, notificationType, message, channel);
        notifications.put(notificationId, notification);

        // Add to user notifications list
        userNotifications.computeIfAbsent(userId, k -> new ArrayList<>()).add(notificationId);

        // Publish notification initiated event
        eventPublisher.publishNotificationInitiated(notificationId, userId, orderId, notificationType, message, channel);

        try {
            // Send notification based on channel
            boolean success = sendByChannel(notification);
            
            if (success) {
                notification.setStatus("SENT");
                notification.setSentAt(new Date());
                
                eventPublisher.publishNotificationSent(notificationId, userId, orderId, notificationType, channel);
                
                logger.info("Notification sent successfully - ID: {}, Channel: {}", notificationId, channel);
                return new NotificationResult(true, "Notification sent successfully", notificationId, "SENT");
            } else {
                notification.setStatus("FAILED");
                notification.setFailureReason("Channel delivery failed");
                notification.setFailedAt(new Date());
                
                eventPublisher.publishNotificationFailed(notificationId, userId, orderId, notificationType, 
                    channel, "Channel delivery failed", "DELIVERY_FAILED");
                
                logger.error("Notification delivery failed - ID: {}, Channel: {}", notificationId, channel);
                return new NotificationResult(false, "Delivery failed", notificationId, "FAILED");
            }
            
        } catch (Exception e) {
            notification.setStatus("FAILED");
            notification.setFailureReason(e.getMessage());
            notification.setFailedAt(new Date());
            
            eventPublisher.publishNotificationFailed(notificationId, userId, orderId, notificationType, 
                channel, e.getMessage(), "PROCESSING_ERROR");
            
            logger.error("Notification processing error - ID: {}", notificationId, e);
            return new NotificationResult(false, "Processing error: " + e.getMessage(), notificationId, "FAILED");
        }
    }

    public NotificationResult sendMultiChannelNotification(String userId, String orderId, 
                                                         String notificationType, String message, 
                                                         List<String> channels) {
        logger.info("Sending multi-channel notification - User: {}, Order: {}, Type: {}, Channels: {}", 
            userId, orderId, notificationType, channels);

        List<NotificationResult> results = new ArrayList<>();
        boolean overallSuccess = true;
        
        for (String channel : channels) {
            NotificationResult result = sendNotification(userId, orderId, notificationType, message, channel);
            results.add(result);
            
            if (!result.isSuccess()) {
                overallSuccess = false;
            }
        }
        
        // Return consolidated result
        String consolidatedId = "MULTI-" + UUID.randomUUID().toString().substring(0, 8);
        String status = overallSuccess ? "SENT" : "PARTIAL";
        String resultMessage = String.format("Multi-channel notification: %d/%d successful", 
            (int) results.stream().mapToInt(r -> r.isSuccess() ? 1 : 0).sum(), channels.size());
        
        return new NotificationResult(overallSuccess, resultMessage, consolidatedId, status);
    }

    // =====================================================
    // Channel-Specific Methods
    // =====================================================

    private boolean sendByChannel(NotificationRecord notification) {
        String channel = notification.getChannel();
        
        switch (channel.toUpperCase()) {
            case "EMAIL":
                return sendEmail(notification);
            case "SMS":
                return sendSMS(notification);
            case "PUSH":
                return sendPushNotification(notification);
            default:
                logger.warn("Unknown notification channel: {}", channel);
                return false;
        }
    }

    private boolean sendEmail(NotificationRecord notification) {
        logger.info("Sending email notification - ID: {}, User: {}", 
            notification.getNotificationId(), notification.getUserId());
        
        if (emailSimulationMode) {
            // Simulate email sending
            try {
                Thread.sleep(50); // Simulate email service latency
                
                // Simulate 98% success rate
                boolean success = Math.random() < 0.98;
                
                if (success) {
                    logger.info("EMAIL SIMULATION: Email sent to user {} for order {} - Subject: {} - Message: {}", 
                        notification.getUserId(), notification.getOrderId(), 
                        notification.getNotificationType(), notification.getMessage());
                    return true;
                } else {
                    logger.warn("EMAIL SIMULATION: Email delivery failed for user {}", notification.getUserId());
                    return false;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        } else {
            // TODO: Implement real email sending (SendGrid, AWS SES, etc.)
            logger.info("Real email sending not implemented yet - using simulation");
            return sendEmail(notification); // Fallback to simulation
        }
    }

    private boolean sendSMS(NotificationRecord notification) {
        logger.info("Sending SMS notification - ID: {}, User: {}", 
            notification.getNotificationId(), notification.getUserId());
        
        if (smsSimulationMode) {
            // Simulate SMS sending
            try {
                Thread.sleep(30); // Simulate SMS service latency
                
                // Simulate 95% success rate
                boolean success = Math.random() < 0.95;
                
                if (success) {
                    logger.info("SMS SIMULATION: SMS sent to user {} for order {} - Message: {}", 
                        notification.getUserId(), notification.getOrderId(), notification.getMessage());
                    return true;
                } else {
                    logger.warn("SMS SIMULATION: SMS delivery failed for user {}", notification.getUserId());
                    return false;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        } else {
            // TODO: Implement real SMS sending (Twilio, AWS SNS, etc.)
            logger.info("Real SMS sending not implemented yet - using simulation");
            return sendSMS(notification); // Fallback to simulation
        }
    }

    private boolean sendPushNotification(NotificationRecord notification) {
        logger.info("Sending push notification - ID: {}, User: {}", 
            notification.getNotificationId(), notification.getUserId());
        
        if (pushSimulationMode) {
            // Simulate push notification
            try {
                Thread.sleep(20); // Simulate push service latency
                
                // Simulate 90% success rate (push notifications can be more unreliable)
                boolean success = Math.random() < 0.90;
                
                if (success) {
                    logger.info("PUSH SIMULATION: Push notification sent to user {} for order {} - Message: {}", 
                        notification.getUserId(), notification.getOrderId(), notification.getMessage());
                    return true;
                } else {
                    logger.warn("PUSH SIMULATION: Push notification delivery failed for user {}", notification.getUserId());
                    return false;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        } else {
            // TODO: Implement real push notification (Firebase, Apple Push, etc.)
            logger.info("Real push notification sending not implemented yet - using simulation");
            return sendPushNotification(notification); // Fallback to simulation
        }
    }

    // =====================================================
    // Utility Methods
    // =====================================================

    private boolean isChannelEnabled(String channel) {
        switch (channel.toUpperCase()) {
            case "EMAIL":
                return emailEnabled;
            case "SMS":
                return smsEnabled;
            case "PUSH":
                return pushEnabled;
            default:
                return false;
        }
    }

    public NotificationRecord getNotificationById(String notificationId) {
        return notifications.get(notificationId);
    }

    public List<NotificationRecord> getUserNotifications(String userId) {
        List<String> userNotificationIds = userNotifications.get(userId);
        if (userNotificationIds == null) {
            return new ArrayList<>();
        }
        
        return userNotificationIds.stream()
            .map(notifications::get)
            .filter(Objects::nonNull)
            .toList();
    }

    public Map<String, Integer> getNotificationStats() {
        Map<String, Integer> stats = new HashMap<>();
        
        int total = notifications.size();
        int sent = 0, failed = 0, pending = 0;
        
        for (NotificationRecord notification : notifications.values()) {
            switch (notification.getStatus()) {
                case "SENT":
                    sent++;
                    break;
                case "FAILED":
                    failed++;
                    break;
                default:
                    pending++;
            }
        }
        
        stats.put("total", total);
        stats.put("sent", sent);
        stats.put("failed", failed);
        stats.put("pending", pending);
        
        return stats;
    }

    // =====================================================
    // Data Classes
    // =====================================================

    public static class NotificationRecord {
        private String notificationId;
        private String userId;
        private String orderId;
        private String notificationType;
        private String message;
        private String channel;
        private String status;
        private String failureReason;
        private Date createdAt;
        private Date sentAt;
        private Date failedAt;

        public NotificationRecord(String notificationId, String userId, String orderId, 
                                String notificationType, String message, String channel) {
            this.notificationId = notificationId;
            this.userId = userId;
            this.orderId = orderId;
            this.notificationType = notificationType;
            this.message = message;
            this.channel = channel;
            this.status = "CREATED";
            this.createdAt = new Date();
        }

        // Getters and setters
        public String getNotificationId() { return notificationId; }
        public String getUserId() { return userId; }
        public String getOrderId() { return orderId; }
        public String getNotificationType() { return notificationType; }
        public String getMessage() { return message; }
        public String getChannel() { return channel; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getFailureReason() { return failureReason; }
        public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
        public Date getCreatedAt() { return createdAt; }
        public Date getSentAt() { return sentAt; }
        public void setSentAt(Date sentAt) { this.sentAt = sentAt; }
        public Date getFailedAt() { return failedAt; }
        public void setFailedAt(Date failedAt) { this.failedAt = failedAt; }
    }

    public static class NotificationResult {
        private boolean success;
        private String message;
        private String notificationId;
        private String status;

        public NotificationResult(boolean success, String message, String notificationId, String status) {
            this.success = success;
            this.message = message;
            this.notificationId = notificationId;
            this.status = status;
        }

        // Getters
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getNotificationId() { return notificationId; }
        public String getStatus() { return status; }
    }
}