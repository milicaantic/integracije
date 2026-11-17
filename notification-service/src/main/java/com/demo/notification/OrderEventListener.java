package com.demo.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    @RabbitListener(queues = RabbitConfig.QUEUE)
    public void onOrderPlaced(OrderPlaced event) {
        log.info("Order received: orderId={}, bookId={}", event.orderId(), event.bookId());
    }
}