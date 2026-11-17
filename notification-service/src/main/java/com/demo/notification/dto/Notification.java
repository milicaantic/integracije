package com.demo.notification.dto;

import java.time.Instant;

public record Notification(Long orderId, Long bookId, Instant receivedAt) {}
