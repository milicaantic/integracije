package com.demo.gateway;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.sun.net.httpserver.HttpServer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayRoutingTest {

    private static final HttpServer FAKE_BOOK_SERVICE = startFakeBookService();

    private static HttpServer startFakeBookService() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/api/books", exchange -> {
                byte[] body = "[{\"bookId\":1,\"bookName\":\"Clean Code\"}]".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @DynamicPropertySource
    static void routeToFakeBookService(DynamicPropertyRegistry registry) {
        registry.add("BOOK_SERVICE_URL",
                () -> "http://127.0.0.1:" + FAKE_BOOK_SERVICE.getAddress().getPort());
    }

    @AfterAll
    static void stopFakeBookService() {
        FAKE_BOOK_SERVICE.stop(0);
    }

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void booksRequestIsForwardedToBookService() {
        webTestClient.get().uri("/api/books")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].bookName").isEqualTo("Clean Code");
    }

    @Test
    void unknownPathReturnsNotFound() {
        webTestClient.get().uri("/api/unknown")
                .exchange()
                .expectStatus().isNotFound();
    }
}