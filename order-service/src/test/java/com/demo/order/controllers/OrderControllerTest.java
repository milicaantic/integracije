package com.demo.order.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.demo.order.controllers.OrderController;
import com.demo.order.dto.CreateOrderRequest;
import com.demo.order.dto.OrderResponse;
import com.demo.order.exceptions.BookNotFoundException;
import com.demo.order.services.OrderService;

import reactor.core.publisher.Mono;

@WebFluxTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private OrderService orderService;

    @Test
    void placeOrderReturnsCreated() {
        when(orderService.placeOrder(any(CreateOrderRequest.class)))
                .thenReturn(Mono.just(new OrderResponse(1L, 5L, "Clean Code", 2, 100.0, "PLACED")));

        webTestClient.post().uri("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"bookId\":5,\"quantity\":2}")
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.orderId").isEqualTo(1)
                .jsonPath("$.totalPrice").isEqualTo(100.0)
                .jsonPath("$.status").isEqualTo("PLACED");
    }

    @Test
    void placeOrderWithoutBookIdReturnsBadRequest() {
        webTestClient.post().uri("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"quantity\":2}")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void placeOrderForMissingBookReturnsNotFound() {
        when(orderService.placeOrder(any(CreateOrderRequest.class)))
                .thenReturn(Mono.error(new BookNotFoundException(99L)));

        webTestClient.post().uri("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"bookId\":99,\"quantity\":1}")
                .exchange()
                .expectStatus().isNotFound();
    }
}