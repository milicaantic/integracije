package com.demo.order.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import com.demo.order.clients.BookClient;
import com.demo.order.config.RabbitConfig;
import com.demo.order.dto.BookDto;
import com.demo.order.dto.CreateOrderRequest;
import com.demo.order.events.OrderPlaced;
import com.demo.order.exceptions.BookNotFoundException;
import com.demo.order.services.OrderService;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private BookClient bookClient;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private OrderService orderService;

    @Test
    void placeOrderCalculatesTotalAndSendsEvent() {
        when(bookClient.getBook(5L)).thenReturn(Mono.just(new BookDto(5L, "Clean Code", 50.0)));

        StepVerifier.create(orderService.placeOrder(new CreateOrderRequest(5L, 2)))
                .assertNext(order -> {
                    assertEquals(5L, order.bookId());
                    assertEquals("Clean Code", order.bookName());
                    assertEquals(100.0, order.totalPrice());
                    assertEquals("PLACED", order.status());
                })
                .verifyComplete();

        verify(rabbitTemplate).convertAndSend(eq(RabbitConfig.EXCHANGE), eq(RabbitConfig.ROUTING_KEY),
                any(OrderPlaced.class));
    }

    @Test
    void placeOrderFailsAndSendsNothingWhenBookDoesNotExist() {
        when(bookClient.getBook(99L)).thenReturn(Mono.error(new BookNotFoundException(99L)));

        StepVerifier.create(orderService.placeOrder(new CreateOrderRequest(99L, 1)))
                .expectError(BookNotFoundException.class)
                .verify();

        verifyNoInteractions(rabbitTemplate);
    }
}