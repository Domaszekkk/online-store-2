package com.estore.gateway;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "gateway.client.retry.period-millis=10",
        "gateway.client.retry.max-period-millis=20",
        "gateway.client.retry.max-attempts=3"
})
@AutoConfigureMockMvc
class GatewayIntegrationTest {

    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final UUID UNAVAILABLE_PRODUCT_ID = UUID.randomUUID();
    private static final UUID VARIANT_ID = UUID.randomUUID();
    private static final UUID ADD_ON_PRODUCT_ID = UUID.randomUUID();
    private static final UUID CART_ID = UUID.randomUUID();
    private static final UUID CLOSED_CART_ID = UUID.randomUUID();
    private static final UUID UNAVAILABLE_CART_ID = UUID.randomUUID();
    private static final UUID ITEM_ID = UUID.randomUUID();
    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID MISSING_ORDER_ID = UUID.randomUUID();
    private static final String INVALID_TAX_IDENTIFICATION_NUMBER = "5260250275";
    private static final byte[] PDF_CONTENT = "%PDF-1.4 invoice".getBytes(StandardCharsets.US_ASCII);
    private static final Map<String, AtomicInteger> CALLS = new ConcurrentHashMap<>();
    private static final HttpServer SERVICES_STUB = startServicesStub();

    @Autowired
    private MockMvc mockMvc;

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
            server.createContext("/", GatewayIntegrationTest::handle);
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
            respond(exchange, 200, "[" + productBody() + "]");
        } else if (method.equals("GET") && path.equals("/products/" + PRODUCT_ID)) {
            respond(exchange, 200, productBody());
        } else if (method.equals("GET") && path.equals("/products/" + UNAVAILABLE_PRODUCT_ID)) {
            respond(exchange, 503, "{\"message\":\"down\"}");
        } else if (method.equals("POST") && path.equals("/carts")) {
            respond(exchange, 201, emptyCartBody());
        } else if (method.equals("POST") && path.equals("/carts/" + CART_ID + "/items")) {
            respond(exchange, 200, cartWithItemBody());
        } else if (method.equals("POST") && path.equals("/carts/" + CLOSED_CART_ID + "/items")) {
            respond(exchange, 409, "{\"message\":\"Cart with id " + CLOSED_CART_ID
                    + " is CHECKED_OUT and can no longer be modified\"}");
        } else if (method.equals("POST") && path.equals("/carts/" + UNAVAILABLE_CART_ID + "/items")) {
            respond(exchange, 503, "{\"message\":\"down\"}");
        } else if (method.equals("GET") && path.equals("/orders/delivery-methods")) {
            respond(exchange, 200, """
                    [{"method":"COURIER","cost":{"amount":19.99,"currency":"PLN"}},
                     {"method":"PARCEL_LOCKER","cost":{"amount":12.99,"currency":"PLN"}},
                     {"method":"PICKUP","cost":{"amount":0.00,"currency":"PLN"}}]
                    """);
        } else if (method.equals("GET") && path.equals("/orders/payment-methods")) {
            respond(exchange, 200, "[\"CARD\",\"BLIK\",\"TRANSFER\",\"CASH_ON_DELIVERY\"]");
        } else if (method.equals("POST") && path.equals("/orders/preview")) {
            respond(exchange, 200, previewBody());
        } else if (method.equals("POST") && path.equals("/orders") && body.contains(INVALID_TAX_IDENTIFICATION_NUMBER)) {
            respond(exchange, 400, "{\"message\":\"Validation failed: billingAddress.taxIdentificationNumber: "
                    + "must be 10 digits with a correct checksum\"}");
        } else if (method.equals("POST") && path.equals("/orders")) {
            respond(exchange, 201, orderBody());
        } else if (method.equals("GET") && path.equals("/orders/" + ORDER_ID)) {
            respond(exchange, 200, orderBody());
        } else if (method.equals("GET") && path.equals("/orders/" + MISSING_ORDER_ID)) {
            respond(exchange, 404, "{\"message\":\"Order with id " + MISSING_ORDER_ID + " not found\"}");
        } else if (method.equals("GET") && path.equals("/documents/order/" + ORDER_ID) && callNumber > 1) {
            exchange.getResponseHeaders().add("Content-Disposition", "inline; filename=\"faktura-FV-2026-10-000001.pdf\"");
            respond(exchange, 200, "application/pdf", PDF_CONTENT);
        } else if (method.equals("GET") && path.startsWith("/documents/order/")) {
            respond(exchange, 404, "{\"message\":\"Document for order not found\"}");
        } else {
            respond(exchange, 404, "{\"message\":\"no stub for " + method + " " + path + "\"}");
        }
    }

    private static String productBody() {
        return """
                {"id":"%s","name":"ProBook X15","type":"COMPUTER","description":"Laptop biznesowy",
                 "basePrice":{"amount":3999.00,"currency":"PLN"},"attributes":{"ekran":"15.6"},
                 "options":[{"name":"RAM","position":1,"values":["16GB","32GB"]}],
                 "variants":[{"id":"%s","stockKeepingUnit":"PBX15-16","optionValues":{"RAM":"16GB"},
                              "price":{"amount":4599.00,"currency":"PLN"},"availability":"AVAILABLE","vatRate":"VAT_23"}],
                 "addOns":[{"productId":"%s","name":"Kabel HDMI 2m","price":{"amount":39.00,"currency":"PLN"},"vatRate":"VAT_23"}]}
                """.formatted(PRODUCT_ID, VARIANT_ID, ADD_ON_PRODUCT_ID);
    }

    private static String emptyCartBody() {
        return """
                {"id":"%s","status":"ACTIVE","items":[],"total":{"amount":0,"currency":"PLN"},
                 "createdAt":"2026-10-22T10:00:00Z","updatedAt":"2026-10-22T10:00:00Z"}
                """.formatted(CART_ID);
    }

    private static String cartWithItemBody() {
        return """
                {"id":"%s","status":"ACTIVE","items":[
                  {"id":"%s","productId":"%s","variantId":"%s","productName":"ProBook X15",
                   "stockKeepingUnit":"PBX15-16","unitPrice":{"amount":4599.00,"currency":"PLN"},"vatRate":"VAT_23","quantity":1,
                   "addOns":[{"productId":"%s","name":"Kabel HDMI 2m","price":{"amount":39.00,"currency":"PLN"},"vatRate":"VAT_23","quantity":1}],
                   "lineTotal":{"amount":4638.00,"currency":"PLN"}}],
                 "total":{"amount":4638.00,"currency":"PLN"},
                 "createdAt":"2026-10-22T10:00:00Z","updatedAt":"2026-10-22T10:01:00Z"}
                """.formatted(CART_ID, ITEM_ID, PRODUCT_ID, VARIANT_ID, ADD_ON_PRODUCT_ID);
    }

    private static String previewBody() {
        return """
                {"customer":{"firstName":"Jan","lastName":"Kowalski","email":"jan.kowalski@example.com","phone":"+48 600 100 200"},
                 "shippingAddress":{"street":"Marszałkowska 1","city":"Warszawa","postalCode":"00-001","country":"Polska"},
                 "billingAddress":null,"paymentMethod":"BLIK","deliveryMethod":"PARCEL_LOCKER",
                 "lines":[{"id":null,"productId":"%s","variantId":"%s","productName":"ProBook X15","stockKeepingUnit":"PBX15-16",
                           "unitPrice":{"amount":4599.00,"currency":"PLN"},"vatRate":"VAT_23","quantity":1,"addOns":[],
                           "lineTotal":{"amount":4599.00,"currency":"PLN"}}],
                 "itemsTotal":{"amount":4599.00,"currency":"PLN"},"deliveryCost":{"amount":12.99,"currency":"PLN"},
                 "grandTotal":{"amount":4611.99,"currency":"PLN"}}
                """.formatted(PRODUCT_ID, VARIANT_ID);
    }

    private static String orderBody() {
        return """
                {"id":"%s","orderNumber":"ORD-2026-000001","cartId":"%s","status":"NEW",
                 "customer":{"firstName":"Jan","lastName":"Kowalski","email":"jan.kowalski@example.com","phone":"+48 600 100 200"},
                 "shippingAddress":{"street":"Marszałkowska 1","city":"Warszawa","postalCode":"00-001","country":"Polska"},
                 "billingAddress":{"companyName":"Kowalski Sp. z o.o.","taxIdentificationNumber":"5260250274",
                                   "street":"Firmowa 5","city":"Kraków","postalCode":"30-001","country":"Polska"},
                 "paymentMethod":"BLIK","deliveryMethod":"COURIER",
                 "lines":[{"id":"%s","productId":"%s","variantId":"%s","productName":"ProBook X15","stockKeepingUnit":"PBX15-16",
                           "unitPrice":{"amount":4599.00,"currency":"PLN"},"vatRate":"VAT_23","quantity":1,
                           "addOns":[{"productId":"%s","name":"Kabel HDMI 2m","price":{"amount":39.00,"currency":"PLN"},"vatRate":"VAT_23","quantity":1}],
                           "lineTotal":{"amount":4638.00,"currency":"PLN"}}],
                 "itemsTotal":{"amount":4638.00,"currency":"PLN"},"deliveryCost":{"amount":19.99,"currency":"PLN"},
                 "grandTotal":{"amount":4657.99,"currency":"PLN"},"documentNumber":"FV/2026/10/000001","createdAt":"2026-10-22T10:02:00Z"}
                """.formatted(ORDER_ID, CART_ID, UUID.randomUUID(), PRODUCT_ID, VARIANT_ID, ADD_ON_PRODUCT_ID);
    }

    private static String placeOrderBody(String taxIdentificationNumber) {
        return """
                {"cartId":"%s",
                 "customer":{"firstName":"Jan","lastName":"Kowalski","email":"jan.kowalski@example.com","phone":"+48 600 100 200"},
                 "shippingAddress":{"street":"Marszałkowska 1","city":"Warszawa","postalCode":"00-001","country":"Polska"},
                 "billingAddress":{"companyName":"Kowalski Sp. z o.o.","taxIdentificationNumber":"%s",
                                   "street":"Firmowa 5","city":"Kraków","postalCode":"30-001","country":"Polska"},
                 "paymentMethod":"BLIK","deliveryMethod":"COURIER"}
                """.formatted(CART_ID, taxIdentificationNumber);
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

    @Test
    void purchase_FromProductListToInvoicePdf_PassesThroughAllServices() throws Exception {
        mockMvc.perform(get("/shop/products").param("type", "COMPUTER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(PRODUCT_ID.toString()))
                .andExpect(jsonPath("$[0].basePrice.amount").value(3999.00));

        mockMvc.perform(get("/shop/products/{id}", PRODUCT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.options[0].values[1]").value("32GB"))
                .andExpect(jsonPath("$.variants[0].id").value(VARIANT_ID.toString()))
                .andExpect(jsonPath("$.addOns[0].productId").value(ADD_ON_PRODUCT_ID.toString()));

        mockMvc.perform(post("/shop/carts"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(CART_ID.toString()))
                .andExpect(jsonPath("$.items.length()").value(0));

        mockMvc.perform(post("/shop/carts/{id}/items", CART_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":"%s","variantId":"%s","quantity":1,"addOns":[{"productId":"%s","quantity":1}]}
                                """.formatted(PRODUCT_ID, VARIANT_ID, ADD_ON_PRODUCT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(ITEM_ID.toString()))
                .andExpect(jsonPath("$.items[0].addOns[0].name").value("Kabel HDMI 2m"))
                .andExpect(jsonPath("$.total.amount").value(4638.00));

        mockMvc.perform(get("/shop/checkout-options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentMethods.length()").value(4))
                .andExpect(jsonPath("$.deliveryMethods[1].method").value("PARCEL_LOCKER"))
                .andExpect(jsonPath("$.deliveryMethods[1].cost.amount").value(12.99));

        mockMvc.perform(post("/shop/orders/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(placeOrderBody("5260250274")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").doesNotExist())
                .andExpect(jsonPath("$.deliveryCost.amount").value(12.99))
                .andExpect(jsonPath("$.grandTotal.amount").value(4611.99))
                .andExpect(jsonPath("$.lines[0].stockKeepingUnit").doesNotExist())
                .andExpect(jsonPath("$.lines[0].vatRate").doesNotExist());

        mockMvc.perform(post("/shop/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(placeOrderBody("5260250274")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(ORDER_ID.toString()))
                .andExpect(jsonPath("$.orderNumber").value("ORD-2026-000001"))
                .andExpect(jsonPath("$.billingAddress.city").value("Kraków"))
                .andExpect(jsonPath("$.grandTotal.amount").value(4657.99));

        mockMvc.perform(get("/shop/orders/{id}/document", ORDER_ID))
                .andExpect(status().isAccepted())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "2"))
                .andExpect(jsonPath("$.message").value(containsString("is being prepared")));

        mockMvc.perform(get("/shop/orders/{id}/document", ORDER_ID))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("faktura-FV-2026-10-000001.pdf")))
                .andExpect(content().bytes(PDF_CONTENT));
    }

    @Test
    void responses_ServicesSendInternalFields_FrontendGetsThinData() throws Exception {
        mockMvc.perform(get("/shop/products/{id}", PRODUCT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.variants[0].stockKeepingUnit").doesNotExist())
                .andExpect(jsonPath("$.variants[0].vatRate").doesNotExist())
                .andExpect(jsonPath("$.addOns[0].vatRate").doesNotExist())
                .andExpect(jsonPath("$.options[0].position").doesNotExist());

        mockMvc.perform(post("/shop/carts/{id}/items", CART_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"%s\",\"variantId\":\"%s\",\"quantity\":1}".formatted(PRODUCT_ID, VARIANT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].stockKeepingUnit").doesNotExist())
                .andExpect(jsonPath("$.items[0].vatRate").doesNotExist())
                .andExpect(jsonPath("$.createdAt").doesNotExist());

        mockMvc.perform(get("/shop/orders/{id}", ORDER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentNumber").value("FV/2026/10/000001"))
                .andExpect(jsonPath("$.cartId").doesNotExist())
                .andExpect(jsonPath("$.lines[0].stockKeepingUnit").doesNotExist())
                .andExpect(jsonPath("$.lines[0].vatRate").doesNotExist())
                .andExpect(jsonPath("$.lines[0].addOns[0].productId").doesNotExist());
    }

    @Test
    void placeOrder_InvalidTaxIdentificationNumber_Returns400WithOrderServiceMessage() throws Exception {
        mockMvc.perform(post("/shop/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(placeOrderBody(INVALID_TAX_IDENTIFICATION_NUMBER)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Validation failed: billingAddress.taxIdentificationNumber: must be 10 digits with a correct checksum"));
    }

    @Test
    void addItem_CartCheckedOut_Returns409WithCartServiceMessage() throws Exception {
        mockMvc.perform(post("/shop/carts/{id}/items", CLOSED_CART_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"%s\",\"variantId\":\"%s\",\"quantity\":1}".formatted(PRODUCT_ID, VARIANT_ID)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Cart with id " + CLOSED_CART_ID + " is CHECKED_OUT and can no longer be modified"));
    }

    @Test
    void getProduct_ProductServiceKeepsFailing_Returns503() throws Exception {
        mockMvc.perform(get("/shop/products/{id}", UNAVAILABLE_PRODUCT_ID))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message")
                        .value("Service product-service is currently unavailable, please try again later"));

        assertEquals(3, callsFor("GET /products/" + UNAVAILABLE_PRODUCT_ID));
    }

    @Test
    void addItem_CartServiceFails_Returns503AfterSingleAttempt() throws Exception {
        mockMvc.perform(post("/shop/carts/{id}/items", UNAVAILABLE_CART_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"%s\",\"variantId\":\"%s\",\"quantity\":1}".formatted(PRODUCT_ID, VARIANT_ID)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message")
                        .value("Service cart-service is currently unavailable, please try again later"));

        assertEquals(1, callsFor("POST /carts/" + UNAVAILABLE_CART_ID + "/items"));
    }

    @Test
    void getOrderDocument_OrderNotFound_Returns404WithoutAskingInvoiceService() throws Exception {
        mockMvc.perform(get("/shop/orders/{id}/document", MISSING_ORDER_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Order with id " + MISSING_ORDER_ID + " not found"));

        assertEquals(0, callsFor("GET /documents/order/" + MISSING_ORDER_ID));
    }
}
