package com.example.ordernotification.producer;

import com.example.ordernotification.dto.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka Producer — publishes OrderEvent messages to the orders.placed topic.
 *
 * Key concept: we use the orderId as the Kafka message key.
 * This guarantees all events for the SAME order always land on the SAME partition,
 * preserving order for that customer's events.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderProducer {

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    @Value("${app.kafka.topics.orders-placed}")
    private String ordersTopic;

    public void publishOrder(OrderEvent event) {
        log.info("Publishing OrderEvent to Kafka | orderId={} | product={} | customer={}",
                event.getOrderId(), event.getProductName(), event.getCustomerId());

        // send(topic, key, value) — key=orderId ensures partition affinity
        CompletableFuture<SendResult<String, OrderEvent>> future =
                kafkaTemplate.send(ordersTopic, event.getOrderId(), event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Order published successfully | orderId={} | partition={} | offset={}",
                        event.getOrderId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("Failed to publish order | orderId={} | error={}",
                        event.getOrderId(), ex.getMessage(), ex);
            }
        });
    }
}
