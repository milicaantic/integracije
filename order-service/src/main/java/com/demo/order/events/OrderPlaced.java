package com.demo.order.events;

public record OrderPlaced(Long orderId, Long bookId) {}