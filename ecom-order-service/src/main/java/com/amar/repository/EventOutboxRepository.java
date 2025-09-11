package com.amar.repository;

import com.amar.entity.order.EventOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface EventOutboxRepository extends JpaRepository<EventOutbox, UUID> {
    
    @Query("SELECT e FROM EventOutbox e WHERE e.processed = false ORDER BY e.createdAt ASC")
    List<EventOutbox> findUnprocessedEvents();
    
    @Query("SELECT e FROM EventOutbox e WHERE e.processed = false AND e.retryCount < :maxRetries ORDER BY e.createdAt ASC")
    List<EventOutbox> findUnprocessedEventsWithRetryLimit(@Param("maxRetries") int maxRetries);
    
    @Query("SELECT e FROM EventOutbox e WHERE e.processed = true AND e.processedAt < :cutoffTime")
    List<EventOutbox> findProcessedEventsOlderThan(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    List<EventOutbox> findByAggregateIdAndAggregateType(String aggregateId, String aggregateType);
    
    @Query("SELECT e FROM EventOutbox e WHERE e.processed = false AND e.retryCount < :maxRetries ORDER BY e.createdAt ASC")
    List<EventOutbox> findEventsForRetry(@Param("maxRetries") int maxRetries);
    
    @Query("SELECT e FROM EventOutbox e WHERE e.processed = true AND e.processedAt < :cutoffTime")
    List<EventOutbox> findOldProcessedEvents(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    @Query("SELECT COUNT(e) FROM EventOutbox e WHERE e.processed = false")
    Long countUnprocessedEvents();
}