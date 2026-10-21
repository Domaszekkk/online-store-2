package com.estore.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OrderIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private CartCheckoutPort cartCheckoutPort;
    @Autowired
    private OrderRepositoryPort orderRepositoryPort;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static Money pln(String amount) {
        return Money.of(new BigDecimal(amount), "PLN");
    }

    private UUID checkoutReturnsTwoLaptopsAndOneCable() {
        UUID cartId = UUID.randomUUID();
        when(cartCheckoutPort.checkout(cartId)).thenReturn(CartDetails.builder()
                .id(cartId)
                .items(List.of(CartItemDetails.builder()
                        .productId(UUID.randomUUID()).variantId(UUID.randomUUID())
                        .productName("ProBook X15").stockKeepingUnit("PBX15-I7-16")
                        .unitPrice(pln("4599.00")).vatRate(VatRate.VAT_23).quantity(2)
                        .addOns(List.of(CartItemAddOnDetails.builder()
                                .productId(UUID.randomUUID()).name("Kabel HDMI 2m").price(pln("39.00")).vatRate(VatRate.VAT_8).quantity(1)
                                .build()))
                        .build()))
                .build());
        return cartId;
    }

    private String orderBody(UUID cartId, String email, String deliveryMethod) {
        return """
                {"cartId":"%s",
                 "customer":{"firstName":"Jan","lastName":"Kowalski","email":"%s","phone":"+48 600 100 200"},
                 "shippingAddress":{"street":"Marszałkowska 1","city":"Warszawa","postalCode":"00-001","country":"Polska"},
                 "paymentMethod":"BLIK","deliveryMethod":"%s"}
                """.formatted(cartId, email, deliveryMethod);
    }

    private String placeOrder(UUID cartId, String email) throws Exception {
        String response = mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderBody(cartId, email, "COURIER")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    @Test
    void placeOrder_thenGetById_returnsPersistedSnapshot() throws Exception {
        UUID cartId = checkoutReturnsTwoLaptopsAndOneCable();

        String id = placeOrder(cartId, "jan.kowalski@example.com");

        mockMvc.perform(get("/orders/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.orderNumber").value(matchesPattern("ORD-\\d{4}-\\d{6}")))
                .andExpect(jsonPath("$.cartId").value(cartId.toString()))
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.customer.lastName").value("Kowalski"))
                .andExpect(jsonPath("$.shippingAddress.street").value("Marszałkowska 1"))
                .andExpect(jsonPath("$.billingAddress").value(nullValue()))
                .andExpect(jsonPath("$.lines.length()").value(1))
                .andExpect(jsonPath("$.lines[0].quantity").value(2))
                .andExpect(jsonPath("$.lines[0].addOns[0].name").value("Kabel HDMI 2m"))
                .andExpect(jsonPath("$.lines[0].addOns[0].quantity").value(1))
                .andExpect(jsonPath("$.lines[0].vatRate").value("VAT_23"))
                .andExpect(jsonPath("$.lines[0].addOns[0].vatRate").value("VAT_8"))
                .andExpect(jsonPath("$.lines[0].lineTotal.amount").value(9237.00))
                .andExpect(jsonPath("$.itemsTotal.amount").value(9237.00))
                .andExpect(jsonPath("$.deliveryCost.amount").value(19.99))
                .andExpect(jsonPath("$.grandTotal.amount").value(9256.99));
    }

    @Test
    void placeOrder_TwoOrders_GetConsecutiveOrderNumbers() throws Exception {
        String firstNumber = objectMapper.readTree(mockMvc.perform(get("/orders/{id}",
                        placeOrder(checkoutReturnsTwoLaptopsAndOneCable(), "a@example.com")))
                .andReturn().getResponse().getContentAsString()).get("orderNumber").asText();
        String secondNumber = objectMapper.readTree(mockMvc.perform(get("/orders/{id}",
                        placeOrder(checkoutReturnsTwoLaptopsAndOneCable(), "b@example.com")))
                .andReturn().getResponse().getContentAsString()).get("orderNumber").asText();

        long first = Long.parseLong(firstNumber.substring(firstNumber.lastIndexOf('-') + 1));
        long second = Long.parseLong(secondNumber.substring(secondNumber.lastIndexOf('-') + 1));
        assertEquals(first + 1, second);
    }

    @Test
    void placeOrder_CartEmpty_Returns409AndSavesNothing() throws Exception {
        UUID cartId = UUID.randomUUID();
        String email = "conflict-" + UUID.randomUUID() + "@example.com";
        when(cartCheckoutPort.checkout(cartId))
                .thenThrow(new CartNotOrderableException(cartId, "Cart with id " + cartId + " is empty and cannot be checked out"));

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderBody(cartId, email, "COURIER")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("is empty")));

        mockMvc.perform(get("/orders").param("email", email))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void placeOrder_SameCartTwice_SecondReturns409WithFirstOrderNumberWithoutCheckout() throws Exception {
        UUID cartId = checkoutReturnsTwoLaptopsAndOneCable();
        String email = "twice-" + UUID.randomUUID() + "@example.com";
        String firstOrderNumber = objectMapper.readTree(mockMvc.perform(get("/orders/{id}", placeOrder(cartId, email)))
                .andReturn().getResponse().getContentAsString()).get("orderNumber").asText();

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderBody(cartId, email, "COURIER")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "Order " + firstOrderNumber + " has already been placed from cart " + cartId));

        verify(cartCheckoutPort, times(1)).checkout(cartId);
        mockMvc.perform(get("/orders").param("email", email))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void saveOrder_SecondOrderForSameCartRacingPastTheCheck_IsRejectedByUniqueCartId() throws Exception {
        UUID cartId = checkoutReturnsTwoLaptopsAndOneCable();
        placeOrder(cartId, "race-" + UUID.randomUUID() + "@example.com");
        Order racingOrder = orderRepositoryPort.findByCartId(cartId).orElseThrow();
        racingOrder.setId(UUID.randomUUID());
        racingOrder.setOrderNumber("ORD-2026-999999");
        racingOrder.getLines().forEach(line -> line.setId(UUID.randomUUID()));

        OrderAlreadyPlacedException exception = Assertions.assertThrows(OrderAlreadyPlacedException.class,
                () -> orderRepositoryPort.save(racingOrder));

        assertEquals("An order has already been placed from cart " + cartId, exception.getMessage());
    }

    @Test
    void getOrders_ByEmail_ReturnsOnlyThatCustomerNewestFirst() throws Exception {
        String email = "history-" + UUID.randomUUID() + "@example.com";
        String olderId = placeOrder(checkoutReturnsTwoLaptopsAndOneCable(), email);
        String newerId = placeOrder(checkoutReturnsTwoLaptopsAndOneCable(), email);
        placeOrder(checkoutReturnsTwoLaptopsAndOneCable(), "someone-else-" + UUID.randomUUID() + "@example.com");

        mockMvc.perform(get("/orders").param("email", email.toUpperCase()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(newerId))
                .andExpect(jsonPath("$[1].id").value(olderId));
    }

    @Test
    void getOrders_NoEmail_ReturnsAllOrders() throws Exception {
        String id = placeOrder(checkoutReturnsTwoLaptopsAndOneCable(), "all-" + UUID.randomUUID() + "@example.com");

        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + id + "')]").exists());
    }

    @Test
    void getOrderById_OrderNotFound_Returns404() throws Exception {
        mockMvc.perform(get("/orders/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void placeOrder_WithBillingAddress_PersistsBillingAddress() throws Exception {
        UUID cartId = checkoutReturnsTwoLaptopsAndOneCable();
        String body = orderBody(cartId, "invoice-" + UUID.randomUUID() + "@example.com", "COURIER")
                .replace("\"paymentMethod\"",
                        "\"billingAddress\":{\"street\":\"Firmowa 5\",\"city\":\"Kraków\",\"postalCode\":\"30-001\",\"country\":\"Polska\"},\"paymentMethod\"");
        String response = mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(response).get("id").asText();

        mockMvc.perform(get("/orders/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.billingAddress.street").value("Firmowa 5"))
                .andExpect(jsonPath("$.billingAddress.city").value("Kraków"))
                .andExpect(jsonPath("$.billingAddress.companyName").value(nullValue()))
                .andExpect(jsonPath("$.shippingAddress.street").value("Marszałkowska 1"));
    }

    @Test
    void placeOrder_WithCompanyBillingAddress_PersistsCompanyData() throws Exception {
        UUID cartId = checkoutReturnsTwoLaptopsAndOneCable();
        String body = orderBody(cartId, "company-" + UUID.randomUUID() + "@example.com", "COURIER")
                .replace("\"paymentMethod\"",
                        "\"billingAddress\":{\"companyName\":\"Kowalski Sp. z o.o.\",\"taxIdentificationNumber\":\"5260250274\","
                                + "\"street\":\"Firmowa 5\",\"city\":\"Kraków\",\"postalCode\":\"30-001\",\"country\":\"Polska\"},"
                                + "\"paymentMethod\"");
        String response = mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(response).get("id").asText();

        mockMvc.perform(get("/orders/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.billingAddress.companyName").value("Kowalski Sp. z o.o."))
                .andExpect(jsonPath("$.billingAddress.taxIdentificationNumber").value("5260250274"))
                .andExpect(jsonPath("$.billingAddress.street").value("Firmowa 5"));
    }
}
