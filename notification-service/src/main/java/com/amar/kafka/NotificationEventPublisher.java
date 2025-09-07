package com.amar.kafka;

import com.amar.config.KafkaConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
public class NotificationEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(NotificationEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public NotificationEventPublisher(KafkaTemplate<String, Object> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    // =====================================================
    // Notification Lifecycle Events
    // =====================================================

    public void publishNotificationInitiated(String notificationId, String userId, String orderId, 
                                           String notificationType, String message, String channel) {
        Map<String, Object> eventData = createBaseNotificationEvent("notification.initiated", notificationId, userId, orderId);
        eventData.put("notificationType", notificationType);
        eventData.put("message", message);
        eventData.put("channel", channel);
        eventData.put("status", "INITIATED");
        
        publishEvent(KafkaConfig.NOTIFICATION_EVENTS_TOPIC, notificationId, eventData);
        
        logger.debug("Published notification initiated event - ID: {}, User: {}, Channel: {}", 
            notificationId, userId, channel);
    }

    public void publishNotificationSent(String notificationId, String userId, String orderId, 
                                       String notificationType, String channel) {
        Map<String, Object> eventData = createBaseNotificationEvent("notification.sent", notificationId, userId, orderId);
        eventData.put("notificationType", notificationType);
        eventData.put("channel", channel);
        eventData.put("status", "SENT");
        eventData.put("sentAt", new Date().toString());
        
        publishEvent(KafkaConfig.NOTIFICATION_EVENTS_TOPIC, notificationId, eventData);
        
        logger.debug("Published notification sent event - ID: {}, User: {}, Channel: {}", 
            notificationId, userId, channel);
    }

    public void publishNotificationFailed(String notificationId, String userId, String orderId, 
                                        String notificationType, String channel, String reason, String errorCode) {
        Map<String, Object> eventData = createBaseNotificationEvent("notification.failed", notificationId, userId, orderId);
        eventData.put("notificationType", notificationType);
        eventData.put("channel", channel);
        eventData.put("status", "FAILED");
        eventData.put("failureReason", reason);
        eventData.put("errorCode", errorCode);
        eventData.put("failedAt", new Date().toString());
        
        publishEvent(KafkaConfig.NOTIFICATION_EVENTS_TOPIC, notificationId, eventData);
        
        logger.warn("Published notification failed event - ID: {}, User: {}, Channel: {}, Reason: {}", 
            notificationId, userId, channel, reason);
    }

    public void publishNotificationRetried(String notificationId, String userId, String orderId, 
                                         String channel, int retryCount, String previousError) {
        Map<String, Object> eventData = createBaseNotificationEvent("notification.retried", notificationId, userId, orderId);
        eventData.put("channel", channel);
        eventData.put("retryCount", retryCount);
        eventData.put("previousError", previousError);
        eventData.put("retriedAt", new Date().toString());
        
        publishEvent(KafkaConfig.NOTIFICATION_EVENTS_TOPIC, notificationId, eventData);
        
        logger.info("Published notification retry event - ID: {}, Attempt: {}", notificationId, retryCount);
    }

    // =====================================================
    // User Engagement Events
    // =====================================================

    public void publishUserNotified(String userId, String notificationId, String notificationType, 
                                   String channel, boolean successful) {
        Map<String, Object> eventData = createBaseUserEvent("user.notified", userId);
        eventData.put("notificationId", notificationId);
        eventData.put("notificationType", notificationType);
        eventData.put("channel", channel);
        eventData.put("successful", successful);
        eventData.put("notifiedAt", new Date().toString());
        
        publishEvent(KafkaConfig.USER_EVENTS_TOPIC, userId, eventData);
        
        logger.debug("Published user notified event - User: {}, Notification: {}, Channel: {}", 
            userId, notificationId, channel);
    }

    public void publishUserEngaged(String userId, String notificationId, String engagementType, 
                                 Map<String, Object> engagementData) {
        Map<String, Object> eventData = createBaseUserEvent("user.engaged", userId);
        eventData.put("notificationId", notificationId);
        eventData.put("engagementType", engagementType); // OPENED, CLICKED, REPLIED, etc.
        eventData.put("engagementData", engagementData);
        eventData.put("engagedAt", new Date().toString());
        
        publishEvent(KafkaConfig.USER_EVENTS_TOPIC, userId, eventData);
        
        logger.info("Published user engagement event - User: {}, Type: {}", userId, engagementType);
    }

    public void publishUserPreferencesUpdated(String userId, Map<String, Object> preferences) {
        Map<String, Object> eventData = createBaseUserEvent("user.preferences.updated", userId);
        eventData.put("preferences", preferences);
        eventData.put("updatedAt", new Date().toString());
        
        publishEvent(KafkaConfig.USER_EVENTS_TOPIC, userId, eventData);
        
        logger.info("Published user preferences updated event - User: {}", userId);
    }

    // =====================================================
    // Channel-Specific Events
    // =====================================================

    public void publishEmailBounced(String notificationId, String userId, String emailAddress, 
                                   String bounceType, String bounceReason) {
        Map<String, Object> eventData = createBaseNotificationEvent("notification.email.bounced", notificationId, userId, null);
        eventData.put("channel", "EMAIL");
        eventData.put("emailAddress", emailAddress);
        eventData.put("bounceType", bounceType); // HARD, SOFT, COMPLAINT
        eventData.put("bounceReason", bounceReason);
        eventData.put("bouncedAt", new Date().toString());
        
        publishEvent(KafkaConfig.NOTIFICATION_EVENTS_TOPIC, notificationId, eventData);
        
        logger.warn("Published email bounce event - User: {}, Type: {}, Reason: {}", 
            userId, bounceType, bounceReason);
    }

    public void publishSmsDeliveryStatus(String notificationId, String userId, String phoneNumber, 
                                       String deliveryStatus, String providerId) {
        Map<String, Object> eventData = createBaseNotificationEvent("notification.sms.delivery", notificationId, userId, null);
        eventData.put("channel", "SMS");
        eventData.put("phoneNumber", phoneNumber);
        eventData.put("deliveryStatus", deliveryStatus); // DELIVERED, FAILED, PENDING
        eventData.put("providerId", providerId);
        eventData.put("statusReceivedAt", new Date().toString());
        
        publishEvent(KafkaConfig.NOTIFICATION_EVENTS_TOPIC, notificationId, eventData);
        
        logger.info("Published SMS delivery status - User: {}, Status: {}", userId, deliveryStatus);
    }

    public void publishPushNotificationStatus(String notificationId, String userId, String deviceToken, 
                                            String deliveryStatus, String errorDetails) {
        Map<String, Object> eventData = createBaseNotificationEvent("notification.push.status", notificationId, userId, null);
        eventData.put("channel", "PUSH");
        eventData.put("deviceToken", deviceToken != null ? deviceToken.substring(0, 10) + "..." : null); // Truncate for privacy
        eventData.put("deliveryStatus", deliveryStatus);
        if (errorDetails != null) {
            eventData.put("errorDetails", errorDetails);
        }
        eventData.put("statusReceivedAt", new Date().toString());
        
        publishEvent(KafkaConfig.NOTIFICATION_EVENTS_TOPIC, notificationId, eventData);
        
        logger.info("Published push notification status - User: {}, Status: {}", userId, deliveryStatus);
    }

    // =====================================================
    // Notification Analytics Events
    // =====================================================

    public void publishNotificationAnalytics(String notificationType, String channel, 
                                           int totalSent, int successful, int failed, 
                                           double avgDeliveryTime, Date periodStart, Date periodEnd) {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("eventType", "notification.analytics");
        eventData.put("notificationType", notificationType);
        eventData.put("channel", channel);
        eventData.put("totalSent", totalSent);
        eventData.put("successful", successful);
        eventData.put("failed", failed);
        eventData.put("successRate", totalSent > 0 ? (double) successful / totalSent : 0.0);
        eventData.put("avgDeliveryTimeMs", avgDeliveryTime);
        eventData.put("periodStart", periodStart.toString());
        eventData.put("periodEnd", periodEnd.toString());
        eventData.put("generatedAt", new Date().toString());
        eventData.put("serviceName", "notification-service");
        
        publishEvent(KafkaConfig.NOTIFICATION_EVENTS_TOPIC, "analytics-" + System.currentTimeMillis(), eventData);
        
        logger.info("Published notification analytics - Type: {}, Channel: {}, Success Rate: {}", 
            notificationType, channel, String.format("%.1f%%", (double) successful / totalSent * 100));
    }

    // =====================================================
    // Helper Methods
    // =====================================================

    private Map<String, Object> createBaseNotificationEvent(String eventType, String notificationId, 
                                                           String userId, String orderId) {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("eventType", eventType);
        eventData.put("notificationId", notificationId);
        eventData.put("userId", userId);
        if (orderId != null) {
            eventData.put("orderId", orderId);
        }
        eventData.put("timestamp", new Date().toString());
        eventData.put("serviceName", "notification-service");
        eventData.put("version", "1.0");
        
        return eventData;
    }

    private Map<String, Object> createBaseUserEvent(String eventType, String userId) {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("eventType", eventType);
        eventData.put("userId", userId);
        eventData.put("timestamp", new Date().toString());
        eventData.put("serviceName", "notification-service");
        eventData.put("version", "1.0");
        
        return eventData;
    }

    private void publishEvent(String topic, String key, Map<String, Object> eventData) {
        try {
            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, eventData);
            
            future.whenComplete((result, exception) -> {
                if (exception == null) {
                    logger.debug("Successfully sent event to topic: {} with key: {} at offset: {}", 
                        topic, key, result.getRecordMetadata().offset());
                } else {
                    logger.error("Failed to send event to topic: {} with key: {}", topic, key, exception);
                }
            });
            
        } catch (Exception ex) {
            logger.error("Error publishing event to topic: {} with key: {}", topic, key, ex);
        }
    }
}