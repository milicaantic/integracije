package com.demo.notification;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

class OrderEventListenerTest {

    private final OrderEventListener listener = new OrderEventListener();

    @Test
    void onOrderPlacedHandlesEventWithoutErrors() {
        assertDoesNotThrow(() -> listener.onOrderPlaced(new OrderPlaced(1L, 2L)));
    }
}