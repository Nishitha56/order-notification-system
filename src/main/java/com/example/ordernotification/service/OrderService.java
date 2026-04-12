package com.example.ordernotification.service;

import com.example.ordernotification.dto.OrderEvent;
import com.example.ordernotification.dto.OrderRequest;
import com.example.ordernotification.producer.OrderProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderProducer orderProducer;

    /**
     * Accepts an order request, creates an OrderEvent, and publishes it to Kafka.
     * The three consumers then independently handle inventory, email, and audit.
     */
    public OrderEvent placeOrder(OrderRequest request) {
        log.info("Placing order for customer={} | product={}",
                request.getCustomerId(), request.getProductName());

        OrderEvent event = OrderEvent.create(
                request.getCustomerId(),
                request.getCustomerEmail(),
                request.getProductId(),
                request.getProductName(),
                request.getQuantity(),
                request.getTotalAmount()
        );

        orderProducer.publishOrder(event);

        return event;
    }
}
