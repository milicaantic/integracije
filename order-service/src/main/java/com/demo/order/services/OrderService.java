package com.demo.order.services;

import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import com.demo.order.clients.BookClient;
import com.demo.order.config.RabbitConfig;
import com.demo.order.dto.CreateOrderRequest;
import com.demo.order.dto.OrderResponse;
import com.demo.order.events.OrderPlaced;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final BookClient bookClient;
    private final RabbitTemplate rabbitTemplate;
    private final AtomicLong orderIds = new AtomicLong();

    public OrderService(BookClient bookClient, RabbitTemplate rabbitTemplate) {
        this.bookClient = bookClient;
        this.rabbitTemplate = rabbitTemplate;
    }

    public Mono<OrderResponse> placeOrder(CreateOrderRequest request) {
        return bookClient.getBook(request.bookId())
                .flatMap(book -> {
                    long orderId = orderIds.incrementAndGet();
                    double price = book.bookPrice() == null ? 0.0 : book.bookPrice();
                    OrderResponse response = new OrderResponse(orderId, book.bookId(), book.bookName(),
                            request.quantity(), price * request.quantity(), "PLACED");

                    return Mono.fromRunnable(() -> rabbitTemplate.convertAndSend(
                                    RabbitConfig.EXCHANGE, RabbitConfig.ROUTING_KEY,
                                    new OrderPlaced(orderId, book.bookId())))
                            .subscribeOn(Schedulers.boundedElastic())
                            .doOnSuccess(v -> log.info("Order placed: orderId={}, bookId={}", orderId, book.bookId()))
                            .thenReturn(response);
                });
    }
}