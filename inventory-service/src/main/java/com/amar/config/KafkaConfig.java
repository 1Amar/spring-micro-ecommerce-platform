package com.amar.config;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConfig {

    private static final Logger logger = LoggerFactory.getLogger(KafkaConfig.class);

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String consumerGroupId;

    @Value("${inventory.kafka.topics.inventory-events:inventory-events}")
    private String inventoryEventsTopic;

    @Value("${inventory.kafka.topics.inventory-movement-events:inventory-movement-events}")
    private String inventoryMovementEventsTopic;

    @Value("${inventory.kafka.topics.inventory-reservation-events:inventory-reservation-events}")
    private String inventoryReservationEventsTopic;

    @Value("${inventory.kafka.topics.inventory-low-stock-alerts:inventory-low-stock-alerts}")
    private String inventoryLowStockAlertsTopic;

    @Value("${inventory.kafka.topics.inventory-alert-events:inventory-alert-events}")
    private String inventoryAlertEventsTopic;

    // =====================================================
    // Producer Configuration
    // =====================================================

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        
        // Producer reliability settings
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 1000);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        
        // Performance settings
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 5);
        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
        configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        
        // Timeout settings
        configProps.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000);
        configProps.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120000);
        
        logger.info("Configured Kafka producer with bootstrap servers: {}", bootstrapServers);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        KafkaTemplate<String, Object> template = new KafkaTemplate<>(producerFactory());
        
        // Enable producer listener for success/failure callbacks
        template.setProducerListener(new org.springframework.kafka.support.ProducerListener<String, Object>() {
            public void onSuccess(org.springframework.kafka.support.SendResult<String, Object> result) {
                logger.debug("Successfully sent message to topic: {} with key: {}", 
                    result.getRecordMetadata().topic(), result.getProducerRecord().key());
            }

            public void onError(org.springframework.kafka.support.SendResult<String, Object> result, RuntimeException exception) {
                logger.error("Failed to send message to topic: {} with key: {}", 
                    result.getProducerRecord().topic(), result.getProducerRecord().key(), exception);
            }
        });
        
        return template;
    }

    // =====================================================
    // Consumer Configuration
    // =====================================================

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        
        // Consumer reliability settings
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        configProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
        configProps.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000);
        configProps.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        configProps.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 3000);
        
        // JSON deserializer settings
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.amar.*");
        configProps.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        configProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "java.util.Map");
        
        logger.info("Configured Kafka consumer with group ID: {}", consumerGroupId);
        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        
        // Container configuration
        factory.setConcurrency(3); // 3 concurrent consumer threads
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.getContainerProperties().setSyncCommits(true);
        
        // Error handling
        factory.setCommonErrorHandler(new org.springframework.kafka.listener.DefaultErrorHandler(
            (record, exception) -> {
                logger.error("Error processing record from topic: {} partition: {} offset: {}", 
                    record.topic(), record.partition(), record.offset(), exception);
            },
            new org.springframework.util.backoff.FixedBackOff(1000L, 3L)
        ));
        
        return factory;
    }

    // =====================================================
    // Admin Configuration (Topic Creation)
    // =====================================================

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configs.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000);
        configs.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 60000);
        
        logger.info("Configured Kafka admin client");
        return new KafkaAdmin(configs);
    }

    // =====================================================
    // Topic Beans (Auto-created on startup)
    // =====================================================

    @Bean
    public NewTopic inventoryEventsTopic() {
        return TopicBuilder.name(inventoryEventsTopic)
            .partitions(3)
            .replicas(1)
            .config("retention.ms", "604800000") // 7 days
            .config("cleanup.policy", "delete")
            .config("compression.type", "snappy")
            .build();
    }

    @Bean
    public NewTopic inventoryMovementEventsTopic() {
        return TopicBuilder.name(inventoryMovementEventsTopic)
            .partitions(3)
            .replicas(1)
            .config("retention.ms", "2592000000") // 30 days
            .config("cleanup.policy", "delete")
            .config("compression.type", "snappy")
            .build();
    }

    @Bean
    public NewTopic inventoryReservationEventsTopic() {
        return TopicBuilder.name(inventoryReservationEventsTopic)
            .partitions(3)
            .replicas(1)
            .config("retention.ms", "604800000") // 7 days
            .config("cleanup.policy", "delete")
            .config("compression.type", "snappy")
            .build();
    }

    @Bean
    public NewTopic inventoryLowStockAlertsTopic() {
        return TopicBuilder.name(inventoryLowStockAlertsTopic)
            .partitions(2)
            .replicas(1)
            .config("retention.ms", "86400000") // 1 day
            .config("cleanup.policy", "delete")
            .config("compression.type", "snappy")
            .build();
    }

    @Bean
    public NewTopic inventoryAlertEventsTopic() {
        return TopicBuilder.name(inventoryAlertEventsTopic)
            .partitions(2)
            .replicas(1)
            .config("retention.ms", "604800000") // 7 days
            .config("cleanup.policy", "delete")
            .config("compression.type", "snappy")
            .build();
    }

    // =====================================================
    // Health Check Bean
    // =====================================================

    @Bean
    public String kafkaConfigInfo() {
        logger.info("Kafka configuration initialized successfully");
        logger.info("Topics configured: {}, {}, {}, {}, {}", 
            inventoryEventsTopic, inventoryMovementEventsTopic, inventoryReservationEventsTopic,
            inventoryLowStockAlertsTopic, inventoryAlertEventsTopic);
        return "Kafka configuration loaded";
    }
}