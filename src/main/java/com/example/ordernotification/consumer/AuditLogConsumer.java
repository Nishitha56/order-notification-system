package com.example.ordernotification.consumer;

import com.example.ordernotification.dto.OrderEvent;
import com.example.ordernotification.entity.AuditLog;
import com.example.ordernotification.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * CONSUMER 3 — Audit Log Service
 *
 * Listens to orders placed and persists every event to the H2 database.
 * Uses groupId "audit-group" — its own independent offset.
 *
 * This demonstrates:
 * - Database persistence from a Kafka consumer
 * - Capturing Kafka metadata (partition, offset) alongside business data
 * - Full audit trail for compliance / debugging
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogConsumer {

    private final AuditLogRepository auditLogRepository;

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            dltTopicSuffix = ".DLT"
    )
    @KafkaListener(
            topics = "${app.kafka.topics.orders-placed}",
            groupId = "audit-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleOrderPlaced(ConsumerRecord<String, OrderEvent> record) {
        OrderEvent event = record.value();

        log.info("[AUDIT] Received order | orderId={} | partition={} | offset={}",
                event.getOrderId(), record.partition(), record.offset());

        AuditLog auditLog = AuditLog.builder()
                .orderId(event.getOrderId())
                .customerId(event.getCustomerId())
                .productName(event.getProductName())
                .quantity(event.getQuantity())
                .status(event.getStatus().name())
                .eventTime(event.getOrderTime())
                .receivedAt(LocalDateTime.now())
                .consumerGroup("audit-group")
                .kafkaPartition(record.partition())
                .kafkaOffset(record.offset())
                .build();

        AuditLog saved = auditLogRepository.save(auditLog);

        log.info("[AUDIT] Event persisted to database | auditId={} | orderId={}",
                saved.getId(), saved.getOrderId());
    }
}
