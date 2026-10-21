package com.estore.order;

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
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = {
        "cart.service.retry.period-millis=10",
        "cart.service.retry.max-period-millis=20",
        "cart.service.retry.max-attempts=3",
        "spring.cloud.openfeign.client.config.cart-service.read-timeout=500"
})
@Import(TestcontainersConfiguration.class)
class CartCheckoutAdapterTest {

    private static final UUID CART_ID = UUID.randomUUID();
    private static final UUID EMPTY_CART_ID = UUID.randomUUID();
    private static final UUID MISSING_CART_ID = UUID.randomUUID();
    private static final UUID FAILING_CART_ID = UUID.randomUUID();
    private static final UUID FLAKY_CART_ID = UUID.randomUUID();
    private static final UUID SLOW_CART_ID = UUID.randomUUID();
    private static final Map<String, AtomicInteger> CALLS_PER_PATH = new ConcurrentHashMap<>();
    private static final HttpServer CART_SERVICE_STUB = startCartServiceStub();

    @Autowired
    private CartCheckoutPort cartCheckoutPort;

    @DynamicPropertySource
    static void cartServiceUrl(DynamicPropertyRegistry registry) {
        registry.add("cart.service.url", () -> "http://localhost:" + CART_SERVICE_STUB.getAddress().getPort());
    }

    @AfterAll
    static void stopStub() {
        CART_SERVICE_STUB.stop(0);
    }

    private static HttpServer startCartServiceStub() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/carts/", CartCheckoutAdapterTest::handle);
            server.setExecutor(Executors.newCachedThreadPool());
            server.start();
            return server;
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        int callNumber = CALLS_PER_PATH.computeIfAbsent(path, key -> new AtomicInteger()).incrementAndGet();

        if (!"POST".equals(exchange.getRequestMethod())) {
            respond(exchange, 405, "{\"message\":\"method not allowed\"}");
        } else if (path.equals(checkoutPath(MISSING_CART_ID))) {
            respond(exchange, 404, "{\"message\":\"Cart with id " + MISSING_CART_ID + " not found\"}");
        } else if (path.equals(checkoutPath(EMPTY_CART_ID))) {
            respond(exchange, 409, "{\"message\":\"Cart with id " + EMPTY_CART_ID
                    + " is empty and cannot be checked out\"}");
        } else if (path.equals(checkoutPath(FAILING_CART_ID))) {
            respond(exchange, 503, "{\"message\":\"down\"}");
        } else if (path.equals(checkoutPath(FLAKY_CART_ID)) && callNumber == 1) {
            respond(exchange, 503, "{\"message\":\"temporarily down\"}");
        } else if (path.equals(checkoutPath(SLOW_CART_ID)) && callNumber == 1) {
            sleep(1500);
            respond(exchange, 200, cartBody(SLOW_CART_ID));
        } else {
            respond(exchange, 200, cartBody(UUID.fromString(path.split("/")[2])));
        }
    }

    private static String cartBody(UUID cartId) {
        return """
                {"id":"%s","status":"CHECKED_OUT","items":[
                  {"id":"%s","productId":"%s","variantId":"%s","productName":"ProBook X15",
                   "stockKeepingUnit":"PBX15-I7-16","unitPrice":{"amount":4599.00,"currency":"PLN"},"vatRate":"VAT_23","quantity":2,
                   "addOns":[{"productId":"%s","name":"Kabel HDMI 2m","price":{"amount":39.00,"currency":"PLN"},"vatRate":"VAT_8","quantity":1}],
                   "lineTotal":{"amount":9237.00,"currency":"PLN"}}],
                 "total":{"amount":9237.00,"currency":"PLN"}}
                """.formatted(cartId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private static String checkoutPath(UUID cartId) {
        return "/carts/" + cartId + "/checkout";
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        } catch (IOException ignored) {
        }
    }

    private static int callsFor(UUID cartId) {
        AtomicInteger calls = CALLS_PER_PATH.get(checkoutPath(cartId));
        return calls == null ? 0 : calls.get();
    }

    @Test
    void checkout_CartWithItems_MapsResponse() {
        CartDetails result = cartCheckoutPort.checkout(CART_ID);

        CartItemDetails item = result.getItems().get(0);
        assertAll(
                () -> assertEquals(CART_ID, result.getId()),
                () -> assertEquals("PBX15-I7-16", item.getStockKeepingUnit()),
                () -> assertEquals(2, item.getQuantity()),
                () -> assertEquals(0, new BigDecimal("4599.00").compareTo(item.getUnitPrice().amount())),
                () -> assertEquals("Kabel HDMI 2m", item.getAddOns().get(0).getName()),
                () -> assertEquals(1, item.getAddOns().get(0).getQuantity()),
                () -> assertEquals(VatRate.VAT_23, item.getVatRate()),
                () -> assertEquals(VatRate.VAT_8, item.getAddOns().get(0).getVatRate())
        );
    }

    @Test
    void checkout_EmptyCart_ThrowsConflictWithCartServiceReasonWithoutRetry() {
        CartNotOrderableException exception = Assertions.assertThrows(CartNotOrderableException.class,
                () -> cartCheckoutPort.checkout(EMPTY_CART_ID));

        assertAll(
                () -> assertEquals(409, exception.getResponseCode()),
                () -> assertEquals("Order cannot be placed from cart " + EMPTY_CART_ID + ": Cart with id "
                        + EMPTY_CART_ID + " is empty and cannot be checked out", exception.getMessage()),
                () -> assertEquals(1, callsFor(EMPTY_CART_ID))
        );
    }

    @Test
    void checkout_CartNotFound_ThrowsCartNotFoundExceptionWithoutRetry() {
        CartNotFoundException exception = Assertions.assertThrows(CartNotFoundException.class,
                () -> cartCheckoutPort.checkout(MISSING_CART_ID));

        assertAll(
                () -> assertEquals(404, exception.getResponseCode()),
                () -> assertEquals(1, callsFor(MISSING_CART_ID))
        );
    }

    @Test
    void checkout_ServerErrorOnce_IsRetriedAndReturnsCart() {
        CartDetails result = cartCheckoutPort.checkout(FLAKY_CART_ID);

        assertAll(
                () -> assertEquals(FLAKY_CART_ID, result.getId()),
                () -> assertEquals(2, callsFor(FLAKY_CART_ID))
        );
    }

    @Test
    void checkout_FirstResponseTooSlow_IsRetriedAndReturnsCart() {
        CartDetails result = cartCheckoutPort.checkout(SLOW_CART_ID);

        assertAll(
                () -> assertEquals(SLOW_CART_ID, result.getId()),
                () -> assertEquals(2, callsFor(SLOW_CART_ID))
        );
    }

    @Test
    void checkout_ServerKeepsFailing_ThrowsUnavailableAfterMaxAttempts() {
        CartServiceUnavailableException exception = Assertions.assertThrows(CartServiceUnavailableException.class,
                () -> cartCheckoutPort.checkout(FAILING_CART_ID));

        assertAll(
                () -> assertEquals(503, exception.getResponseCode()),
                () -> assertEquals(3, callsFor(FAILING_CART_ID))
        );
    }
}
