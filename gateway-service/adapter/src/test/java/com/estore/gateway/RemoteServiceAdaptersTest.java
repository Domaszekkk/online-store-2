package com.estore.gateway;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "gateway.client.retry.period-millis=10",
        "gateway.client.retry.max-period-millis=20",
        "gateway.client.retry.max-attempts=3",
        "spring.cloud.openfeign.client.config.default.read-timeout=500",
        "spring.cloud.openfeign.client.config.order-service.read-timeout=500"
})
class RemoteServiceAdaptersTest {

    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final UUID MISSING_PRODUCT_ID = UUID.randomUUID();
    private static final UUID FLAKY_PRODUCT_ID = UUID.randomUUID();
    private static final UUID FAILING_PRODUCT_ID = UUID.randomUUID();
    private static final UUID SLOW_PRODUCT_ID = UUID.randomUUID();
    private static final UUID CART_ID = UUID.randomUUID();
    private static final UUID FAILING_CART_ID = UUID.randomUUID();
    private static final UUID CHECKED_OUT_CART_ID = UUID.randomUUID();
    private static final UUID SLOW_ORDER_CART_ID = UUID.randomUUID();
    private static final UUID ORDER_WITH_DOCUMENT_ID = UUID.randomUUID();
    private static final UUID ORDER_WITHOUT_DOCUMENT_ID = UUID.randomUUID();
    private static final byte[] PDF_CONTENT = "%PDF-1.4 test".getBytes(StandardCharsets.US_ASCII);
    private static final Map<String, AtomicInteger> CALLS = new ConcurrentHashMap<>();
    private static final AtomicReference<String> LAST_PRODUCTS_QUERY = new AtomicReference<>();
    private static final HttpServer SERVICES_STUB = startServicesStub();

    @Autowired
    private ProductCatalogPort productCatalogPort;
    @Autowired
    private CartPort cartPort;
    @Autowired
    private OrderPort orderPort;
    @Autowired
    private DocumentPort documentPort;

    @DynamicPropertySource
    static void serviceUrls(DynamicPropertyRegistry registry) {
        String url = "http://localhost:" + SERVICES_STUB.getAddress().getPort();
        registry.add("product.service.url", () -> url);
        registry.add("cart.service.url", () -> url);
        registry.add("order.service.url", () -> url);
        registry.add("invoice.service.url", () -> url);
    }

    @AfterAll
    static void stopStub() {
        SERVICES_STUB.stop(0);
    }

    private static HttpServer startServicesStub() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/", RemoteServiceAdaptersTest::handle);
            server.setExecutor(Executors.newCachedThreadPool());
            server.start();
            return server;
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        int callNumber = CALLS.computeIfAbsent(method + " " + path, key -> new AtomicInteger()).incrementAndGet();

        if (method.equals("GET") && path.equals("/products")) {
            LAST_PRODUCTS_QUERY.set(exchange.getRequestURI().getQuery());
            respond(exchange, 200, "{\"items\":[" + productBody(PRODUCT_ID) + "],\"page\":2,\"size\":5,\"totalItems\":11,\"totalPages\":3}");
        } else if (method.equals("GET") && path.equals(productPath(MISSING_PRODUCT_ID))) {
            respond(exchange, 404, "{\"message\":\"Product with id " + MISSING_PRODUCT_ID + " not found\"}");
        } else if (method.equals("GET") && path.equals(productPath(FLAKY_PRODUCT_ID)) && callNumber == 1) {
            respond(exchange, 503, "{\"message\":\"temporarily down\"}");
        } else if (method.equals("GET") && path.equals(productPath(FAILING_PRODUCT_ID))) {
            respond(exchange, 503, "{\"message\":\"down\"}");
        } else if (method.equals("GET") && path.equals(productPath(SLOW_PRODUCT_ID)) && callNumber == 1) {
            sleep(1500);
            respond(exchange, 200, productBody(SLOW_PRODUCT_ID));
        } else if (method.equals("GET") && path.startsWith("/products/")) {
            respond(exchange, 200, productBody(UUID.fromString(path.split("/")[2])));
        } else if (method.equals("POST") && path.equals(itemsPath(FAILING_CART_ID))) {
            respond(exchange, 503, "{\"message\":\"down\"}");
        } else if (method.equals("POST") && path.equals(itemsPath(CHECKED_OUT_CART_ID))) {
            respond(exchange, 409, "{\"message\":\"Cart with id " + CHECKED_OUT_CART_ID + " is already checked out\"}");
        } else if ((method.equals("POST") || method.equals("PATCH") || method.equals("DELETE")) && path.startsWith("/carts/")) {
            respond(exchange, 200, cartBody(UUID.fromString(path.split("/")[2])));
        } else if (method.equals("POST") && path.equals("/orders") && body.contains(SLOW_ORDER_CART_ID.toString())) {
            CALLS.computeIfAbsent("POST slow order", key -> new AtomicInteger()).incrementAndGet();
            sleep(1500);
            respond(exchange, 201, orderBody(UUID.randomUUID()));
        } else if (method.equals("GET") && path.equals("/orders/delivery-methods")) {
            respond(exchange, 200, """
                    [{"method":"COURIER","cost":{"amount":19.99,"currency":"PLN"}},
                     {"method":"PICKUP","cost":{"amount":0.00,"currency":"PLN"}}]
                    """);
        } else if (method.equals("GET") && path.equals("/orders/payment-methods")) {
            respond(exchange, 200, "[\"CARD\",\"BLIK\"]");
        } else if (method.equals("GET") && path.startsWith("/orders/")) {
            respond(exchange, 200, orderBody(UUID.fromString(path.split("/")[2])));
        } else if (method.equals("GET") && path.equals(documentPath(ORDER_WITH_DOCUMENT_ID))) {
            exchange.getResponseHeaders().add("Content-Disposition", "inline; filename=\"faktura-FV-2026-10-000001.pdf\"");
            respond(exchange, 200, "application/pdf", PDF_CONTENT);
        } else if (method.equals("GET") && path.startsWith("/documents/order/")) {
            respond(exchange, 404, "{\"message\":\"Document for order not found\"}");
        } else {
            respond(exchange, 404, "{\"message\":\"no stub for " + method + " " + path + "\"}");
        }
    }

    private static String productBody(UUID productId) {
        return """
                {"id":"%s","name":"ProBook X15","type":"COMPUTER","description":"Laptop",
                 "basePrice":{"amount":4599.00,"currency":"PLN"},"attributes":{"brand":"ProBook"},
                 "options":[{"name":"RAM","position":1,"values":["16 GB","32 GB"]}],
                 "variants":[{"id":"%s","stockKeepingUnit":"PBX15-16","optionValues":{"RAM":"16 GB"},
                              "price":{"amount":4599.00,"currency":"PLN"},"availability":"AVAILABLE","vatRate":"VAT_23"}],
                 "addOns":[{"productId":"%s","name":"Kabel HDMI 2m","price":{"amount":39.00,"currency":"PLN"},"vatRate":"VAT_23"}]}
                """.formatted(productId, UUID.randomUUID(), UUID.randomUUID());
    }

    private static String cartBody(UUID cartId) {
        return """
                {"id":"%s","status":"OPEN","items":[
                  {"id":"%s","productId":"%s","variantId":"%s","productName":"ProBook X15",
                   "stockKeepingUnit":"PBX15-16","unitPrice":{"amount":4599.00,"currency":"PLN"},"vatRate":"VAT_23","quantity":2,
                   "addOns":[{"productId":"%s","name":"Kabel HDMI 2m","price":{"amount":39.00,"currency":"PLN"},"vatRate":"VAT_23","quantity":1}],
                   "lineTotal":{"amount":9237.00,"currency":"PLN"},"available":true}],
                 "total":{"amount":9237.00,"currency":"PLN"}}
                """.formatted(cartId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
    }

    private static String orderBody(UUID orderId) {
        return """
                {"id":"%s","orderNumber":"ORD-2026-000001","cartId":"%s","status":"NEW",
                 "customer":{"firstName":"Jan","lastName":"Kowalski","email":"jan.kowalski@example.com","phone":"+48 600 100 200"},
                 "shippingAddress":{"street":"Marszalkowska 1","city":"Warszawa","postalCode":"00-001","country":"Polska"},
                 "billingAddress":null,"paymentMethod":"BLIK","deliveryMethod":"COURIER",
                 "lines":[{"productName":"ProBook X15","stockKeepingUnit":"PBX15-16","unitPrice":{"amount":4599.00,"currency":"PLN"},
                           "vatRate":"VAT_23","quantity":1,"addOns":[],"lineTotal":{"amount":4599.00,"currency":"PLN"}}],
                 "itemsTotal":{"amount":4599.00,"currency":"PLN"},"deliveryCost":{"amount":19.99,"currency":"PLN"},
                 "grandTotal":{"amount":4618.99,"currency":"PLN"},"createdAt":"2026-10-21T10:00:00Z"}
                """.formatted(orderId, UUID.randomUUID());
    }

    private static String productPath(UUID productId) {
        return "/products/" + productId;
    }

    private static String itemsPath(UUID cartId) {
        return "/carts/" + cartId + "/items";
    }

    private static String documentPath(UUID orderId) {
        return "/documents/order/" + orderId;
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        respond(exchange, status, "application/json", body.getBytes(StandardCharsets.UTF_8));
    }

    private static void respond(HttpExchange exchange, int status, String contentType, byte[] bytes) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        } catch (IOException ignored) {
        }
    }

    private static int callsFor(String methodAndPath) {
        AtomicInteger calls = CALLS.get(methodAndPath);
        return calls == null ? 0 : calls.get();
    }

    private static AddItemCommand addLaptopCommand() {
        return AddItemCommand.builder()
                .productId(UUID.randomUUID())
                .variantId(UUID.randomUUID())
                .quantity(1)
                .addOns(List.of(AddOnSelection.builder().productId(UUID.randomUUID()).quantity(1).build()))
                .build();
    }

    @Test
    void findProducts_DataCorrect_MapsSummary() {
        ResultPage<ProductSummary> result = productCatalogPort.findProducts("COMPUTER", 2, 5);

        assertAll(
                () -> assertEquals("type=COMPUTER&page=2&size=5", LAST_PRODUCTS_QUERY.get()),
                () -> assertEquals(1, result.items().size()),
                () -> assertEquals(3, result.totalPages()),
                () -> assertEquals(PRODUCT_ID, result.items().get(0).getId()),
                () -> assertEquals("COMPUTER", result.items().get(0).getType()),
                () -> assertEquals(0, new BigDecimal("4599.00").compareTo(result.items().get(0).getBasePrice().amount()))
        );
    }

    @Test
    void getProduct_DataCorrect_MapsOptionsVariantsAndAddOns() {
        ProductDetails result = productCatalogPort.getProduct(PRODUCT_ID);

        assertAll(
                () -> assertEquals(List.of("16 GB", "32 GB"), result.getOptions().get(0).getValues()),
                () -> assertEquals("16 GB", result.getVariants().get(0).getOptionValues().get("RAM")),
                () -> assertEquals("AVAILABLE", result.getVariants().get(0).getAvailability()),
                () -> assertEquals("Kabel HDMI 2m", result.getAddOns().get(0).getName()),
                () -> assertEquals("ProBook", result.getAttributes().get("brand"))
        );
    }

    @Test
    void getProduct_ProductNotFound_PassesStatusAndMessageWithoutRetry() {
        ServiceRejectedRequestException exception = Assertions.assertThrows(ServiceRejectedRequestException.class,
                () -> productCatalogPort.getProduct(MISSING_PRODUCT_ID));

        assertAll(
                () -> assertEquals(404, exception.getResponseCode()),
                () -> assertEquals("Product with id " + MISSING_PRODUCT_ID + " not found", exception.getMessage()),
                () -> assertEquals(1, callsFor("GET " + productPath(MISSING_PRODUCT_ID)))
        );
    }

    @Test
    void getProduct_ServerErrorOnce_IsRetriedAndReturnsProduct() {
        ProductDetails result = productCatalogPort.getProduct(FLAKY_PRODUCT_ID);

        assertAll(
                () -> assertEquals(FLAKY_PRODUCT_ID, result.getId()),
                () -> assertEquals(2, callsFor("GET " + productPath(FLAKY_PRODUCT_ID)))
        );
    }

    @Test
    void getProduct_FirstResponseTooSlow_IsRetriedAndReturnsProduct() {
        ProductDetails result = productCatalogPort.getProduct(SLOW_PRODUCT_ID);

        assertAll(
                () -> assertEquals(SLOW_PRODUCT_ID, result.getId()),
                () -> assertEquals(2, callsFor("GET " + productPath(SLOW_PRODUCT_ID)))
        );
    }

    @Test
    void getProduct_ServerKeepsFailing_ThrowsUnavailableAfterMaxAttempts() {
        ServiceUnavailableException exception = Assertions.assertThrows(ServiceUnavailableException.class,
                () -> productCatalogPort.getProduct(FAILING_PRODUCT_ID));

        assertAll(
                () -> assertEquals(503, exception.getResponseCode()),
                () -> assertEquals("Service product-service is currently unavailable, please try again later",
                        exception.getMessage()),
                () -> assertEquals(3, callsFor("GET " + productPath(FAILING_PRODUCT_ID)))
        );
    }

    @Test
    void addItem_DataCorrect_MapsCart() {
        Cart result = cartPort.addItem(CART_ID, addLaptopCommand());

        assertAll(
                () -> assertEquals(CART_ID, result.getId()),
                () -> assertEquals(2, result.getItems().get(0).getQuantity()),
                () -> assertEquals(1, result.getItems().get(0).getAddOns().get(0).getQuantity()),
                () -> assertEquals(0, new BigDecimal("9237.00").compareTo(result.getTotal().amount()))
        );
    }

    @Test
    void addItem_ServerError_IsNotRetried() {
        ServiceUnavailableException exception = Assertions.assertThrows(ServiceUnavailableException.class,
                () -> cartPort.addItem(FAILING_CART_ID, addLaptopCommand()));

        assertAll(
                () -> assertEquals(503, exception.getResponseCode()),
                () -> assertEquals(1, callsFor("POST " + itemsPath(FAILING_CART_ID)))
        );
    }

    @Test
    void addItem_CartCheckedOut_PassesConflictWithMessage() {
        ServiceRejectedRequestException exception = Assertions.assertThrows(ServiceRejectedRequestException.class,
                () -> cartPort.addItem(CHECKED_OUT_CART_ID, addLaptopCommand()));

        assertAll(
                () -> assertEquals(409, exception.getResponseCode()),
                () -> assertEquals("Cart with id " + CHECKED_OUT_CART_ID + " is already checked out", exception.getMessage())
        );
    }

    @Test
    void changeItemQuantity_DataCorrect_SendsPatch() {
        UUID itemId = UUID.randomUUID();

        Cart result = cartPort.changeItemQuantity(CART_ID, itemId, 3);

        assertAll(
                () -> assertEquals(CART_ID, result.getId()),
                () -> assertEquals(1, callsFor("PATCH /carts/" + CART_ID + "/items/" + itemId))
        );
    }

    @Test
    void placeOrder_ResponseTooSlow_IsNotRetried() {
        PlaceOrderCommand command = PlaceOrderCommand.builder()
                .cartId(SLOW_ORDER_CART_ID)
                .customer(Customer.builder().firstName("Jan").lastName("Kowalski")
                        .email("jan.kowalski@example.com").phone("+48 600 100 200").build())
                .shippingAddress(Address.builder().street("Marszalkowska 1").city("Warszawa")
                        .postalCode("00-001").country("Polska").build())
                .paymentMethod("BLIK")
                .deliveryMethod("COURIER")
                .build();

        ServiceUnavailableException exception = Assertions.assertThrows(ServiceUnavailableException.class,
                () -> orderPort.placeOrder(command));

        assertAll(
                () -> assertEquals("Service order-service is currently unavailable, please try again later",
                        exception.getMessage()),
                () -> assertEquals(1, callsFor("POST slow order"))
        );
    }

    @Test
    void getOrder_DataCorrect_MapsOrder() {
        UUID orderId = UUID.randomUUID();

        Order result = orderPort.getOrder(orderId);

        assertAll(
                () -> assertEquals(orderId, result.getId()),
                () -> assertEquals("ORD-2026-000001", result.getOrderNumber()),
                () -> assertEquals("Kowalski", result.getCustomer().getLastName()),
                () -> assertEquals("ProBook X15", result.getLines().get(0).getProductName()),
                () -> assertEquals(0, new BigDecimal("4618.99").compareTo(result.getGrandTotal().amount()))
        );
    }

    @Test
    void getDeliveryOptionsAndPaymentMethods_DataCorrect_MapsLists() {
        List<DeliveryOption> deliveryOptions = orderPort.getDeliveryOptions();

        assertAll(
                () -> assertEquals("COURIER", deliveryOptions.get(0).getMethod()),
                () -> assertEquals(0, new BigDecimal("19.99").compareTo(deliveryOptions.get(0).getCost().amount())),
                () -> assertEquals(List.of("CARD", "BLIK"), orderPort.getPaymentMethods())
        );
    }

    @Test
    void findDocument_DocumentIssued_ReturnsPdfWithFileName() {
        OrderDocument result = documentPort.findByOrderId(ORDER_WITH_DOCUMENT_ID).orElseThrow();

        assertAll(
                () -> assertEquals("faktura-FV-2026-10-000001.pdf", result.getFileName()),
                () -> assertArrayEquals(PDF_CONTENT, result.getContent())
        );
    }

    @Test
    void findDocument_DocumentNotIssuedYet_ReturnsEmpty() {
        Optional<OrderDocument> result = documentPort.findByOrderId(ORDER_WITHOUT_DOCUMENT_ID);

        assertTrue(result.isEmpty());
    }
}
