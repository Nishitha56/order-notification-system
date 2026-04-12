package com.example.ordernotification.consumer;

import com.example.ordernotification.dto.OrderEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;

/**
 * CONSUMER 1 — Inventory Service
 *
 * Listens to orders.placed and deducts stock for the ordered product.
 *
 * Key concepts demonstrated:
 * - @KafkaListener with a dedicated groupId ("inventory-group")
 *   This means this consumer gets its own independent offset pointer —
 *   it does NOT share consumption with the email or audit consumers.
 * - @RetryableTopic for automatic retry with backoff before hitting the DLQ.
 */
@Slf4j
@Service
public class InventoryConsumer {

    // In-memory inventory — simulates a real stock database
    private final java.util.concurrent.ConcurrentHashMap<String, Integer> inventory =
            new java.util.concurrent.ConcurrentHashMap<>() {{
                put("PROD-001", 100);
                put("PROD-002", 50);
                put("PROD-003", 200);
            }};

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            dltTopicSuffix = ".DLT"
    )
    @KafkaListener(
            topics = "${app.kafka.topics.orders-placed}",
            groupId = "inventory-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleOrderPlaced(ConsumerRecord<String, OrderEvent> record) {
        OrderEvent event = record.value();

        log.info("[INVENTORY] Received order | orderId={} | product={} | qty={} | partition={} | offset={}",
                event.getOrderId(), event.getProductId(), event.getQuantity(),
                record.partition(), record.offset());

        // Simulate inventory deduction
        String productId = event.getProductId();
        int requested = event.getQuantity();

        inventory.compute(productId, (key, currentStock) -> {
            if (currentStock == null) {
                log.warn("[INVENTORY] Product not found in inventory | productId={}", productId);
                // Add new product to inventory
                return 0;
            }
            int newStock = currentStock - requested;
            if (newStock < 0) {
                log.warn("[INVENTORY] Insufficient stock | productId={} | available={} | requested={}",
                        productId, currentStock, requested);
                return 0; // clamp to 0 for demo purposes
            }
            log.info("[INVENTORY] Stock updated | productId={} | before={} | after={}",
                    productId, currentStock, newStock);
            return newStock;
        });

        log.info("[INVENTORY] Current stock levels: {}", inventory);
    }

    // Expose inventory for the REST status endpoint
    public java.util.Map<String, Integer> getInventory() {
        return java.util.Collections.unmodifiableMap(inventory);
    }
}
