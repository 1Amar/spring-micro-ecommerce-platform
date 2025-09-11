package com.amar.service;

import com.amar.entity.order.EventOutbox;
import com.amar.repository.EventOutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@Transactional
public class EventOutboxService {
    
    private static final Logger logger = LoggerFactory.getLogger(EventOutboxService.class);
    private static final int MAX_RETRY_COUNT = 5;
    
    private final EventOutboxRepository outboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public EventOutboxService(EventOutboxRepository outboxRepository, 
                             KafkaTemplate<String, Object> kafkaTemplate,
                             ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Save event to outbox for reliable publishing
     */
    public void saveEvent(String aggregateId, String aggregateType, String eventType, 
                         Object eventData, String topic, String kafkaKey) {
        try {
            String eventDataJson = objectMapper.writeValueAsString(eventData);
            
            EventOutbox outboxEvent = new EventOutbox(
                aggregateId, 
                aggregateType, 
                eventType, 
                eventDataJson, 
                topic, 
                kafkaKey
            );
            
            outboxRepository.save(outboxEvent);
            logger.debug("Saved event to outbox: {} for aggregate: {}", eventType, aggregateId);
            
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize event data for outbox: {} - {}", eventType, e.getMessage());
            throw new RuntimeException("Failed to save event to outbox", e);
        }
    }
    
    /**
     * Process unprocessed events from outbox - scheduled task
     */
    @Scheduled(fixedDelay = 5000) // Every 5 seconds
    @Transactional
    public void processOutboxEvents() {
        List<EventOutbox> unprocessedEvents = outboxRepository.findUnprocessedEvents();
        
        if (unprocessedEvents.isEmpty()) {
            return;
        }
        
        logger.debug("Processing {} unprocessed outbox events", unprocessedEvents.size());
        
        for (EventOutbox event : unprocessedEvents) {
            try {
                publishEventToKafka(event);
            } catch (Exception e) {
                logger.error("Failed to publish event {} to Kafka: {}", event.getId(), e.getMessage());
                handleFailedEvent(event, e.getMessage());
            }
        }
    }
    
    /**
     * Process retry-able failed events
     */
    @Scheduled(fixedDelay = 30000) // Every 30 seconds
    @Transactional
    public void processFailedEvents() {
        List<EventOutbox> retryableEvents = outboxRepository.findEventsForRetry(MAX_RETRY_COUNT);
        
        if (retryableEvents.isEmpty()) {
            return;
        }
        
        logger.debug("Retrying {} failed outbox events", retryableEvents.size());
        
        for (EventOutbox event : retryableEvents) {
            try {
                publishEventToKafka(event);
            } catch (Exception e) {
                logger.warn("Retry failed for event {}: {}", event.getId(), e.getMessage());
                handleFailedEvent(event, e.getMessage());
            }
        }
    }
    
    /**
     * Cleanup old processed events
     */
    @Scheduled(fixedDelay = 3600000) // Every hour
    @Transactional
    public void cleanupOldEvents() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(7); // Keep events for 7 days
        List<EventOutbox> oldEvents = outboxRepository.findOldProcessedEvents(cutoffDate);
        
        if (!oldEvents.isEmpty()) {
            outboxRepository.deleteAll(oldEvents);
            logger.info("Cleaned up {} old processed outbox events", oldEvents.size());
        }
    }
    
    private void publishEventToKafka(EventOutbox event) throws JsonProcessingException {
        // Deserialize the event data
        @SuppressWarnings("unchecked")
        Map<String, Object> eventData = objectMapper.readValue(event.getEventData(), Map.class);
        
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(
            event.getTopic(), 
            event.getKafkaKey(), 
            eventData
        );
        
        future.whenComplete((result, exception) -> {
            if (exception == null) {
                // Mark as processed
                event.markAsProcessed();
                outboxRepository.save(event);
                
                logger.debug("Successfully published event {} to Kafka: {}", 
                           event.getId(), event.getEventType());
            } else {
                logger.error("Failed to publish event {} to Kafka: {}", 
                           event.getId(), exception.getMessage());
                handleFailedEvent(event, exception.getMessage());
            }
        });
        
        // Wait for completion to handle the result synchronously
        try {
            future.get(); // This will throw exception if publishing failed
        } catch (Exception e) {
            throw new RuntimeException("Failed to publish event to Kafka", e);
        }
    }
    
    private void handleFailedEvent(EventOutbox event, String errorMessage) {
        event.incrementRetryCount(errorMessage);
        outboxRepository.save(event);
        
        if (event.getRetryCount() >= MAX_RETRY_COUNT) {
            logger.error("Event {} exceeded max retry count ({}). Manual intervention required.", 
                       event.getId(), MAX_RETRY_COUNT);
        }
    }
    
    /**
     * Get outbox statistics
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getOutboxStats() {
        Long unprocessedCount = outboxRepository.countUnprocessedEvents();
        return Map.of(
            "unprocessedEvents", unprocessedCount,
            "totalEvents", outboxRepository.count()
        );
    }
}