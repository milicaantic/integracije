package com.demo.order;

public record OrderResponse(Long orderId, Long bookId, String bookName,
        Integer quantity, Double totalPrice, String status) {}