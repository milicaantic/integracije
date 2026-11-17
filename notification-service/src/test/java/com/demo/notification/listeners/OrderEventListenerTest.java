package com.demo.notification.listeners;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.demo.notification.dto.Notification;
import com.demo.notification.events.OrderPlaced;
import com.demo.notification.services.NotificationStore;

class OrderEventListenerTest {

    private final NotificationStore notificationStore = new NotificationStore();
    private final OrderEventListener listener = new OrderEventListener(notificationStore);

    @Test
    void onOrderPlacedStoresNotification() {
        listener.onOrderPlaced(new OrderPlaced(1L, 2L));

        List<Notification> all = notificationStore.findAll();
        assertEquals(1, all.size());
        assertEquals(1L, all.get(0).orderId());
        assertEquals(2L, all.get(0).bookId());
        assertNotNull(all.get(0).receivedAt());
    }
}
