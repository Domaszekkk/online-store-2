package com.estore.gateway;

import com.estore.gateway.order.CheckoutOptionsController;
import com.estore.gateway.order.OrderController;
import com.estore.gateway.order.OrderMapperImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({OrderController.class, CheckoutOptionsController.class})
@Import(OrderMapperImpl.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private ShopService shopService;

    private static final String ORDER_WITHOUT_INVOICE = """
            {"cartId":"%s",
             "customer":{"firstName":"Jan","lastName":"Kowalski","email":"jan.kowalski@example.com","phone":"+48 600 100 200"},
             "shippingAddress":{"street":"Marszałkowska 1","city":"Warszawa","postalCode":"00-001","country":"Polska"},
             "paymentMethod":"BLIK","deliveryMethod":"COURIER"}
            """;

    private static Money pln(String amount) {
        return Money.of(new BigDecimal(amount), "PLN");
    }

    private static Order sampleOrder(UUID orderId) {
        return Order.builder()
                .id(orderId)
                .orderNumber("ORD-2026-000001")
                .status("NEW")
                .customer(Customer.builder()
                        .firstName("Jan").lastName("Kowalski").email("jan.kowalski@example.com").phone("+48 600 100 200").build())
                .shippingAddress(Address.builder()
                        .street("Marszałkowska 1").city("Warszawa").postalCode("00-001").country("Polska").build())
                .billingAddress(BillingAddress.builder()
                        .companyName("Kowalski Sp. z o.o.").taxIdentificationNumber("5260250274")
                        .street("Firmowa 5").city("Kraków").postalCode("30-001").country("Polska").build())
                .paymentMethod("BLIK")
                .deliveryMethod("COURIER")
                .lines(List.of(OrderLine.builder()
                        .productName("ProBook X15").unitPrice(pln("4599.00")).quantity(2)
                        .addOns(List.of(OrderLineAddOn.builder().name("Kabel HDMI 2m").price(pln("39.00")).quantity(1).build()))
                        .lineTotal(pln("9237.00"))
                        .build()))
                .itemsTotal(pln("9237.00"))
                .deliveryCost(pln("19.99"))
                .grandTotal(pln("9256.99"))
                .createdAt(Instant.parse("2026-10-21T10:00:00Z"))
                .build();
    }

    @Test
    void placeOrder_WithoutInvoice_Returns201AndSendsNoBillingAddress() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(shopService.placeOrder(any(PlaceOrderCommand.class))).thenReturn(sampleOrder(orderId));

        mockMvc.perform(post("/shop/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ORDER_WITHOUT_INVOICE.formatted(UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.orderNumber").value("ORD-2026-000001"))
                .andExpect(jsonPath("$.lines[0].addOns[0].name").value("Kabel HDMI 2m"))
                .andExpect(jsonPath("$.lines[0].vatRate").doesNotExist())
                .andExpect(jsonPath("$.grandTotal.amount").value(9256.99));

        ArgumentCaptor<PlaceOrderCommand> command = ArgumentCaptor.forClass(PlaceOrderCommand.class);
        verify(shopService).placeOrder(command.capture());
        assertAll(
                () -> assertNull(command.getValue().getBillingAddress()),
                () -> assertEquals("BLIK", command.getValue().getPaymentMethod()),
                () -> assertEquals("Warszawa", command.getValue().getShippingAddress().getCity())
        );
    }

    @Test
    void placeOrder_CompanyInvoice_PassesCompanyData() throws Exception {
        when(shopService.placeOrder(any(PlaceOrderCommand.class))).thenReturn(sampleOrder(UUID.randomUUID()));
        String body = ORDER_WITHOUT_INVOICE.formatted(UUID.randomUUID()).replace("\"paymentMethod\"",
                "\"billingAddress\":{\"companyName\":\"Kowalski Sp. z o.o.\",\"taxIdentificationNumber\":\"5260250274\","
                        + "\"street\":\"Firmowa 5\",\"city\":\"Kraków\",\"postalCode\":\"30-001\",\"country\":\"Polska\"},"
                        + "\"paymentMethod\"");

        mockMvc.perform(post("/shop/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.billingAddress.taxIdentificationNumber").value("5260250274"));

        ArgumentCaptor<PlaceOrderCommand> command = ArgumentCaptor.forClass(PlaceOrderCommand.class);
        verify(shopService).placeOrder(command.capture());
        assertAll(
                () -> assertEquals("Kowalski Sp. z o.o.", command.getValue().getBillingAddress().getCompanyName()),
                () -> assertEquals("5260250274", command.getValue().getBillingAddress().getTaxIdentificationNumber())
        );
    }

    @Test
    void placeOrder_EmptyBody_Returns400WithAllMissingFields() throws Exception {
        mockMvc.perform(post("/shop/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Validation failed: cartId: must not be null; customer: must not be null; "
                                + "deliveryMethod: must not be null; paymentMethod: must not be null; "
                                + "shippingAddress: must not be null"));
        verifyNoInteractions(shopService);
    }

    @Test
    void placeOrder_InvalidTaxIdentificationNumber_Returns400FromOrderService() throws Exception {
        String message = "Validation failed: billingAddress.taxIdentificationNumber: must be 10 digits with a correct checksum";
        when(shopService.placeOrder(any(PlaceOrderCommand.class))).thenThrow(new ServiceRejectedRequestException(message, 400));

        mockMvc.perform(post("/shop/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ORDER_WITHOUT_INVOICE.formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(message));
    }

    @Test
    void placeOrder_OrderAlreadyPlacedFromCart_Returns409() throws Exception {
        UUID cartId = UUID.randomUUID();
        String message = "Order ORD-2026-000001 has already been placed from cart " + cartId;
        when(shopService.placeOrder(any(PlaceOrderCommand.class))).thenThrow(new ServiceRejectedRequestException(message, 409));

        mockMvc.perform(post("/shop/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ORDER_WITHOUT_INVOICE.formatted(cartId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(message));
    }

    @Test
    void getOrder_DataCorrect_Returns200() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(shopService.getOrder(orderId)).thenReturn(sampleOrder(orderId));

        mockMvc.perform(get("/shop/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customer.email").value("jan.kowalski@example.com"))
                .andExpect(jsonPath("$.deliveryCost.amount").value(19.99));
    }

    @Test
    void getOrderDocument_DocumentIssued_Returns200Pdf() throws Exception {
        UUID orderId = UUID.randomUUID();
        byte[] pdf = "%PDF-1.4 test".getBytes();
        when(shopService.findOrderDocument(orderId)).thenReturn(Optional.of(OrderDocument.builder()
                .fileName("faktura-FV-2026-10-000001.pdf").content(pdf).build()));

        mockMvc.perform(get("/shop/orders/{id}/document", orderId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("faktura-FV-2026-10-000001.pdf")))
                .andExpect(content().bytes(pdf));
    }

    @Test
    void getOrderDocument_DocumentNotIssuedYet_Returns202WithRetryAfter() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(shopService.findOrderDocument(orderId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/shop/orders/{id}/document", orderId))
                .andExpect(status().isAccepted())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "2"))
                .andExpect(jsonPath("$.message")
                        .value("Document of order " + orderId + " is being prepared, try again in a few seconds"));
    }

    @Test
    void getOrderDocument_OrderNotFound_Returns404() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(shopService.findOrderDocument(orderId))
                .thenThrow(new ServiceRejectedRequestException("Order with id " + orderId + " not found", 404));

        mockMvc.perform(get("/shop/orders/{id}/document", orderId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Order with id " + orderId + " not found"));
    }

    @Test
    void getCheckoutOptions_Returns200WithPaymentAndDeliveryMethods() throws Exception {
        when(shopService.getCheckoutOptions()).thenReturn(CheckoutOptions.builder()
                .paymentMethods(List.of("CARD", "BLIK"))
                .deliveryMethods(List.of(
                        DeliveryOption.builder().method("COURIER").cost(pln("19.99")).build(),
                        DeliveryOption.builder().method("PICKUP").cost(pln("0.00")).build()))
                .build());

        mockMvc.perform(get("/shop/checkout-options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentMethods[1]").value("BLIK"))
                .andExpect(jsonPath("$.deliveryMethods[0].method").value("COURIER"))
                .andExpect(jsonPath("$.deliveryMethods[0].cost.amount").value(19.99))
                .andExpect(jsonPath("$.deliveryMethods[1].cost.amount").value(0.00));
    }
}
