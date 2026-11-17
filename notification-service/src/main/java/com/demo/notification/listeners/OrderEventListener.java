package com.demo.notification.listeners;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.demo.notification.config.RabbitConfig;
import com.demo.notification.events.OrderPlaced;
import com.demo.notification.services.NotificationStore;

@Component
public class OrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    private final NotificationStore notificationStore;

    public OrderEventListener(NotificationStore notificationStore) {
        this.notificationStore = notificationStore;
    }

    @RabbitListener(queues = RabbitConfig.QUEUE)
    public void onOrderPlaced(OrderPlaced event) {
        log.info("Order received: orderId={}, bookId={}", event.orderId(), event.bookId());
        notificationStore.save(event.orderId(), event.bookId());
    }
}
