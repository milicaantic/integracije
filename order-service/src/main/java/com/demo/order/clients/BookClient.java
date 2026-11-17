package com.demo.order.clients;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.demo.order.dto.BookDto;
import com.demo.order.exceptions.BookNotFoundException;

import reactor.core.publisher.Mono;

@Component
public class BookClient {

    private final WebClient webClient;

    public BookClient(WebClient.Builder builder, @Value("${services.book.url}") String bookServiceUrl) {
        this.webClient = builder.baseUrl(bookServiceUrl).build();
    }

    public Mono<BookDto> getBook(Long bookId) {
        return webClient.get()
                .uri("/api/books/{id}", bookId)
                .retrieve()
                .onStatus(status -> status.value() == HttpStatus.NOT_FOUND.value(),
                        response -> Mono.error(new BookNotFoundException(bookId)))
                .bodyToMono(BookDto.class);
    }
}