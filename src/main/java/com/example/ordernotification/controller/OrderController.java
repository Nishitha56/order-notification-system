package com.example.ordernotification.controller;

import com.example.ordernotification.consumer.EmailNotificationConsumer;
import com.example.ordernotification.consumer.InventoryConsumer;
import com.example.ordernotification.dto.OrderEvent;
import com.example.ordernotification.dto.OrderRequest;
import com.example.ordernotification.entity.AuditLog;
import com.example.ordernotification.repository.AuditLogRepository;
import com.example.ordernotification.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller exposing endpoints to:
 * 1. Place an order  (POST /api/orders)
 * 2. View audit logs (GET  /api/orders/audit)
 * 3. View inventory  (GET  /api/orders/inventory)
 * 4. View sent emails(GET  /api/orders/emails)
 */
@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final AuditLogRepository auditLogRepository;
    private final InventoryConsumer inventoryConsumer;
    private final EmailNotificationConsumer emailNotificationConsumer;

    /**
     * POST /api/orders
     * Places an order — publishes an event to Kafka.
     *
     * Example request body:
     * {
     *   "customerId": "CUST-001",
     *   "customerEmail": "john@example.com",
     *   "productId": "PROD-001",
     *   "productName": "Wireless Headphones",
     *   "quantity": 2,
     *   "totalAmount": 199.99
     * }
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> placeOrder(@RequestBody OrderRequest request) {
        log.info("REST: Received order request for customer={}", request.getCustomerId());

        OrderEvent event = orderService.placeOrder(request);

        return ResponseEntity.ok(Map.of(
                "message", "Order placed successfully! Kafka consumers are processing it.",
                "orderId", event.getOrderId(),
                "status", event.getStatus(),
                "orderTime", event.getOrderTime().toString(),
                "note", "Check /api/orders/audit, /api/orders/inventory, /api/orders/emails after 1-2 seconds"
        ));
    }

    /**
     * GET /api/orders/audit
     * Returns all audit log entries from the database.
     */
    @GetMapping("/audit")
    public ResponseEntity<List<AuditLog>> getAuditLogs() {
        List<AuditLog> logs = auditLogRepository.findAllByOrderByReceivedAtDesc();
        log.info("REST: Returning {} audit log entries", logs.size());
        return ResponseEntity.ok(logs);
    }

    /**
     * GET /api/orders/audit/{orderId}
     * Returns audit logs for a specific order.
     */
    @GetMapping("/audit/{orderId}")
    public ResponseEntity<List<AuditLog>> getAuditLogByOrder(@PathVariable String orderId) {
        List<AuditLog> logs = auditLogRepository.findByOrderIdOrderByReceivedAtDesc(orderId);
        return ResponseEntity.ok(logs);
    }

    /**
     * GET /api/orders/inventory
     * Returns current in-memory stock levels.
     */
    @GetMapping("/inventory")
    public ResponseEntity<Map<String, Object>> getInventory() {
        Map<String, Integer> stock = inventoryConsumer.getInventory();
        return ResponseEntity.ok(Map.of(
                "inventory", stock,
                "note", "Stock is deducted each time an order is consumed by the Inventory consumer"
        ));
    }

    /**
     * GET /api/orders/emails
     * Returns list of simulated emails that were "sent".
     */
    @GetMapping("/emails")
    public ResponseEntity<Map<String, Object>> getSentEmails() {
        List<String> emails = emailNotificationConsumer.getSentEmails();
        return ResponseEntity.ok(Map.of(
                "totalSent", emails.size(),
                "emails", emails
        ));
    }

    /**
     * GET /api/orders/health
     * Simple health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "Order Notification System",
                "kafka", "connected"
        ));
    }
}
