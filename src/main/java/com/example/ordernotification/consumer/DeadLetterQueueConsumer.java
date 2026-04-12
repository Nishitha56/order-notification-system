package com.example.ordernotification.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Dead Letter Queue (DLQ) Handler
 *
 * When @RetryableTopic exhausts all retry attempts (3 tries with backoff),
 * the failed message is automatically routed to the DLT (Dead Letter Topic).
 *
 * This handler consumes those failed messages so they don't silently disappear.
 * In production: alert ops team, store in a failed-events table, trigger manual review.
 */
@Slf4j
@Service
public class DeadLetterQueueConsumer {

    @KafkaListener(
            topics = "${app.kafka.topics.orders-dlq}",
            groupId = "dlq-handler-group"
    )
    public void handleFailedMessage(ConsumerRecord<String, Object> record) {
        log.error("=== DEAD LETTER QUEUE MESSAGE RECEIVED ===");
        log.error("[DLQ] Topic   : {}", record.topic());
        log.error("[DLQ] Key     : {}", record.key());
        log.error("[DLQ] Value   : {}", record.value());
        log.error("[DLQ] Partition: {} | Offset: {}", record.partition(), record.offset());

        // In production you would:
        // 1. Persist to a failed_events table for manual review
        // 2. Send an alert to Slack / PagerDuty
        // 3. Trigger a manual reprocessing workflow
        log.error("[DLQ] Action needed: Investigate failed message for key={}", record.key());
    }
}
