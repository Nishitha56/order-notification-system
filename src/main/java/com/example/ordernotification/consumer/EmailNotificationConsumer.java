package com.example.ordernotification.consumer;

import com.example.ordernotification.dto.OrderEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * CONSUMER 2 — Email Notification Service
 *
 * Listens to orders.placed and simulates sending a confirmation email.
 * Uses groupId "email-group" — completely independent from the inventory consumer.
 *
 * In a real system, this would call SendGrid / SES / SMTP.
 * Here we log the email and keep an in-memory sent list for the demo endpoint.
 */
@Slf4j
@Service
public class EmailNotificationConsumer {

    // In-memory log of all "sent" emails — visible via REST endpoint
    private final List<String> sentEmails = new CopyOnWriteArrayList<>();

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            dltTopicSuffix = ".DLT"
    )
    @KafkaListener(
            topics = "${app.kafka.topics.orders-placed}",
            groupId = "email-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleOrderPlaced(ConsumerRecord<String, OrderEvent> record) {
        OrderEvent event = record.value();

        log.info("[EMAIL] Received order | orderId={} | customer={} | partition={} | offset={}",
                event.getOrderId(), event.getCustomerEmail(),
                record.partition(), record.offset());

        // Simulate email construction and sending
        String emailBody = buildEmailBody(event);
        sendEmail(event.getCustomerEmail(), emailBody, event.getOrderId());
    }

    private String buildEmailBody(OrderEvent event) {
        return String.format(
            """
            ============================================
            ORDER CONFIRMATION
            ============================================
            Dear Customer %s,
            
            Your order has been placed successfully!
            
            Order ID   : %s
            Product    : %s
            Quantity   : %d
            Total      : $%.2f
            Order Time : %s
            Status     : %s
            
            Thank you for shopping with us!
            ============================================
            """,
            event.getCustomerId(),
            event.getOrderId(),
            event.getProductName(),
            event.getQuantity(),
            event.getTotalAmount(),
            event.getOrderTime(),
            event.getStatus()
        );
    }

    private void sendEmail(String toEmail, String body, String orderId) {
        // Simulated email send — replace with real SMTP/SendGrid call
        log.info("[EMAIL] Sending confirmation email to: {}", toEmail);
        log.info("[EMAIL] Email body:\n{}", body);

        String sentRecord = String.format("[%s] Sent to %s for orderId=%s",
                java.time.LocalDateTime.now(), toEmail, orderId);
        sentEmails.add(sentRecord);

        log.info("[EMAIL] Email sent successfully to: {}", toEmail);
    }

    public List<String> getSentEmails() {
        return Collections.unmodifiableList(sentEmails);
    }
}
