package com.estore.cart;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = {
        "product.service.retry.period-millis=10",
        "product.service.retry.max-period-millis=20",
        "product.service.retry.max-attempts=3"
})
@Import(TestcontainersConfiguration.class)
class ProductCatalogAdapterRetryTest {

    private static final UUID FLAKY_PRODUCT_ID = UUID.randomUUID();
    private static final UUID UNAVAILABLE_PRODUCT_ID = UUID.randomUUID();
    private static final UUID MISSING_PRODUCT_ID = UUID.randomUUID();
    private static final Map<String, AtomicInteger> CALLS_PER_PATH = new ConcurrentHashMap<>();
    private static final HttpServer PRODUCT_SERVICE_STUB = startProductServiceStub();

    @Autowired
    private ProductCatalogPort productCatalogPort;

    @DynamicPropertySource
    static void productServiceUrl(DynamicPropertyRegistry registry) {
        registry.add("product.service.url",
                () -> "http://localhost:" + PRODUCT_SERVICE_STUB.getAddress().getPort());
    }

    @AfterAll
    static void stopStub() {
        PRODUCT_SERVICE_STUB.stop(0);
    }

    private static HttpServer startProductServiceStub() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/products/", ProductCatalogAdapterRetryTest::handle);
            server.start();
            return server;
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        int callNumber = CALLS_PER_PATH.computeIfAbsent(path, key -> new AtomicInteger()).incrementAndGet();
        String productId = path.substring("/products/".length());

        if (productId.equals(MISSING_PRODUCT_ID.toString())) {
            respond(exchange, 404, "{\"message\":\"not found\"}");
        } else if (productId.equals(UNAVAILABLE_PRODUCT_ID.toString())) {
            respond(exchange, 503, "{\"message\":\"down\"}");
        } else if (productId.equals(FLAKY_PRODUCT_ID.toString()) && callNumber < 3) {
            respond(exchange, 503, "{\"message\":\"temporarily down\"}");
        } else {
            respond(exchange, 200, """
                    {"id":"%s","name":"ProBook X15","variants":[],"addOns":[]}
                    """.formatted(productId));
        }
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }

    private static int callsFor(UUID productId) {
        AtomicInteger calls = CALLS_PER_PATH.get("/products/" + productId);
        return calls == null ? 0 : calls.get();
    }

    @Test
    void getProduct_ServerErrorTwiceThenSuccess_RetriesAndReturnsProduct() {
        ProductDetails result = productCatalogPort.getProduct(FLAKY_PRODUCT_ID);

        assertAll(
                () -> assertEquals("ProBook X15", result.getName()),
                () -> assertEquals(3, callsFor(FLAKY_PRODUCT_ID))
        );
    }

    @Test
    void getProduct_ServerKeepsFailing_ThrowsUnavailableAfterMaxAttempts() {
        ProductCatalogUnavailableException exception = Assertions.assertThrows(
                ProductCatalogUnavailableException.class,
                () -> productCatalogPort.getProduct(UNAVAILABLE_PRODUCT_ID));

        assertAll(
                () -> assertEquals(503, exception.getResponseCode()),
                () -> assertEquals(3, callsFor(UNAVAILABLE_PRODUCT_ID))
        );
    }

    @Test
    void getProduct_NotFound_DoesNotRetry() {
        Assertions.assertThrows(ProductNotFoundException.class,
                () -> productCatalogPort.getProduct(MISSING_PRODUCT_ID));

        assertEquals(1, callsFor(MISSING_PRODUCT_ID));
    }
}
