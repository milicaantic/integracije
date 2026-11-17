package com.demo.order.clients;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import com.demo.order.clients.BookClient;
import com.demo.order.dto.BookDto;
import com.demo.order.exceptions.BookNotFoundException;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class BookClientTest {

    private BookClient clientReturning(HttpStatus status, String body) {
        WebClient.Builder builder = WebClient.builder()
                .exchangeFunction(request -> Mono.just(ClientResponse.create(status)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body(body)
                        .build()));
        return new BookClient(builder, "http://book-service");
    }

    @Test
    void getBookMapsJsonFromBookService() {
        BookClient client = clientReturning(HttpStatus.OK,
                "{\"bookId\":1,\"bookName\":\"Clean Code\",\"bookPrice\":50.0,\"authorName\":\"Robert C. Martin\"}");

        StepVerifier.create(client.getBook(1L))
                .expectNext(new BookDto(1L, "Clean Code", 50.0))
                .verifyComplete();
    }

    @Test
    void getBookReturnsBookNotFoundOn404() {
        BookClient client = clientReturning(HttpStatus.NOT_FOUND, "Cannot find book with this ID!");

        StepVerifier.create(client.getBook(99L))
                .expectError(BookNotFoundException.class)
                .verify();
    }
}