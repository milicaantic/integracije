package com.demo.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.jayway.jsonpath.JsonPath;

class BookstoreEndToEndTest {

    private static final String BASE_URL = System.getProperty("e2e.baseUrl", "http://localhost:8080");

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private HttpResponse<String> get(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(BASE_URL + path))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, String json) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(BASE_URL + path))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private long createBook(String name, double price) throws Exception {
        String json = """
                {"bookName":"%s","bookPrice":%s,"authorName":"E2E Author","bookGenre":"E2E","publishingYear":2024}
                """.formatted(name, price);
        HttpResponse<String> created = post("/api/books", json);
        assertThat(created.statusCode()).isEqualTo(201);

        HttpResponse<String> found = get("/api/books/search?title=" + URLEncoder.encode(name, StandardCharsets.UTF_8));
        assertThat(found.statusCode()).isEqualTo(200);
        List<Number> ids = JsonPath.read(found.body(), "$[*].bookId");
        assertThat(ids).hasSize(1);
        return ids.get(0).longValue();
    }

    @Test
    void authorsAreAvailableThroughGateway() throws Exception {
        HttpResponse<String> response = get("/api/authors");

        assertThat(response.statusCode()).isEqualTo(200);
        List<String> names = JsonPath.read(response.body(), "$[*].name");
        assertThat(names).contains("Robert C. Martin");
    }

    @Test
    void createdBookCanBeReadThroughGateway() throws Exception {
        String name = "E2E Book " + UUID.randomUUID();
        long bookId = createBook(name, 30.0);

        HttpResponse<String> response = get("/api/books/" + bookId);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat((String) JsonPath.read(response.body(), "$.bookName")).isEqualTo(name);
    }

    @Test
    void placedOrderUsesBookServiceAndReachesNotificationService() throws Exception {
        String name = "E2E Order Book " + UUID.randomUUID();
        long bookId = createBook(name, 50.0);

        HttpResponse<String> order = post("/api/orders", "{\"bookId\":" + bookId + ",\"quantity\":2}");

        assertThat(order.statusCode()).isEqualTo(201);
        assertThat((String) JsonPath.read(order.body(), "$.bookName")).isEqualTo(name);
        assertThat(((Number) JsonPath.read(order.body(), "$.totalPrice")).doubleValue()).isEqualTo(100.0);
        long orderId = ((Number) JsonPath.read(order.body(), "$.orderId")).longValue();

        await().atMost(Duration.ofSeconds(20))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    HttpResponse<String> notifications = get("/api/notifications");
                    assertThat(notifications.statusCode()).isEqualTo(200);
                    List<Object> matching = JsonPath.read(notifications.body(),
                            "$[?(@.orderId == " + orderId + " && @.bookId == " + bookId + ")]");
                    assertThat(matching).hasSize(1);
                });
    }

    @Test
    void orderForMissingBookReturnsNotFound() throws Exception {
        HttpResponse<String> response = post("/api/orders", "{\"bookId\":999999999,\"quantity\":1}");

        assertThat(response.statusCode()).isEqualTo(404);
    }
}
