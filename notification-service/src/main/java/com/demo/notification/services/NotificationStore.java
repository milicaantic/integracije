package com.demo.notification.services;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Service;

import com.demo.notification.dto.Notification;

@Service
public class NotificationStore {

    private final List<Notification> notifications = new CopyOnWriteArrayList<>();

    public Notification save(Long orderId, Long bookId) {
        Notification notification = new Notification(orderId, bookId, Instant.now());
        notifications.add(notification);
        return notification;
    }

    public List<Notification> findAll() {
        return List.copyOf(notifications);
    }
}
