package com.estore.order;

import com.estore.order.order.OrderController;
import com.estore.order.order.OrderMapperImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@Import(OrderMapperImpl.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private OrderService orderService;

    private static final String VALID_BODY = """
            {"cartId":"%s",
             "customer":{"firstName":"Jan","lastName":"Kowalski","email":"jan.kowalski@example.com","phone":"+48 600 100 200"},
             "shippingAddress":{"street":"Marszałkowska 1","city":"Warszawa","postalCode":"00-001","country":"Polska"},
             "paymentMethod":"BLIK","deliveryMethod":"COURIER"}
            """;

    private static Money pln(String amount) {
        return Money.of(new BigDecimal(amount), "PLN");
    }

    private Order sampleOrder(UUID id) {
        Address address = Address.builder()
                .street("Marszałkowska 1").city("Warszawa").postalCode("00-001").country("Polska").build();
        return Order.builder()
                .id(id)
                .orderNumber("ORD-2026-000001")
                .cartId(UUID.randomUUID())
                .status(OrderStatus.NEW)
                .customer(Customer.builder()
                        .firstName("Jan").lastName("Kowalski").email("jan.kowalski@example.com").phone("+48 600 100 200")
                        .build())
                .shippingAddress(address)
                .billingAddress(BillingAddress.builder()
                        .companyName("Kowalski Sp. z o.o.").taxIdentificationNumber("5260250274")
                        .street("Firmowa 5").city("Kraków").postalCode("30-001").country("Polska").build())
                .paymentMethod(PaymentMethod.BLIK)
                .deliveryMethod(DeliveryMethod.COURIER)
                .lines(List.of(OrderLine.builder()
                        .id(UUID.randomUUID()).productId(UUID.randomUUID()).variantId(UUID.randomUUID())
                        .productName("ProBook X15").stockKeepingUnit("PBX15-I7-16")
                        .unitPrice(pln("4599.00")).vatRate(VatRate.VAT_23).quantity(2)
                        .addOns(List.of(OrderLineAddOn.builder()
                                .productId(UUID.randomUUID()).name("Kabel HDMI 2m").price(pln("39.00")).vatRate(VatRate.VAT_8).quantity(1)
                                .build()))
                        .lineTotal(pln("9237.00"))
                        .build()))
                .itemsTotal(pln("9237.00"))
                .deliveryCost(pln("19.99"))
                .grandTotal(pln("9256.99"))
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void placeOrder_DataCorrect_Returns201() throws Exception {
        UUID id = UUID.randomUUID();
        when(orderService.placeOrder(any(PlaceOrderCommand.class))).thenReturn(sampleOrder(id));

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY.formatted(UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.orderNumber").value("ORD-2026-000001"))
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.paymentMethod").value("BLIK"))
                .andExpect(jsonPath("$.deliveryMethod").value("COURIER"))
                .andExpect(jsonPath("$.customer.email").value("jan.kowalski@example.com"))
                .andExpect(jsonPath("$.billingAddress.city").value("Kraków"))
                .andExpect(jsonPath("$.billingAddress.companyName").value("Kowalski Sp. z o.o."))
                .andExpect(jsonPath("$.billingAddress.taxIdentificationNumber").value("5260250274"))
                .andExpect(jsonPath("$.lines[0].vatRate").value("VAT_23"))
                .andExpect(jsonPath("$.lines[0].addOns[0].vatRate").value("VAT_8"))
                .andExpect(jsonPath("$.lines[0].addOns[0].quantity").value(1))
                .andExpect(jsonPath("$.grandTotal.amount").value(9256.99));
    }

    @Test
    void placeOrder_EmptyBody_Returns400WithAllMissingFields() throws Exception {
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Validation failed: cartId: must not be null; customer: must not be null; "
                                + "deliveryMethod: must not be null; paymentMethod: must not be null; "
                                + "shippingAddress: must not be null"));
        verifyNoInteractions(orderService);
    }

    @Test
    void placeOrder_InvalidNestedCustomerAndAddress_Returns400() throws Exception {
        String body = """
                {"cartId":"%s",
                 "customer":{"firstName":"","lastName":"Kowalski","email":"not-an-email","phone":"abc"},
                 "shippingAddress":{"street":"Marszałkowska 1","city":"","postalCode":"00-001","country":"Polska"},
                 "billingAddress":{"street":"","city":"Kraków","postalCode":"30-001","country":"Polska"},
                 "paymentMethod":"BLIK","deliveryMethod":"COURIER"}
                """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(allOf(
                        containsString("customer.firstName"),
                        containsString("customer.email"),
                        containsString("customer.phone"),
                        containsString("shippingAddress.city"),
                        containsString("billingAddress.street"))));
        verifyNoInteractions(orderService);
    }

    @Test
    void placeOrder_CompanyNameWithoutTaxIdentificationNumber_Returns400() throws Exception {
        String body = VALID_BODY.formatted(UUID.randomUUID()).replace("\"paymentMethod\"",
                "\"billingAddress\":{\"companyName\":\"Kowalski Sp. z o.o.\",\"street\":\"Firmowa 5\","
                        + "\"city\":\"Kraków\",\"postalCode\":\"30-001\",\"country\":\"Polska\"},\"paymentMethod\"");

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Validation failed: billingAddress.taxIdentificationNumber: must be provided together with companyName"));
        verifyNoInteractions(orderService);
    }

    @Test
    void placeOrder_TaxIdentificationNumberWithWrongChecksum_Returns400() throws Exception {
        String body = VALID_BODY.formatted(UUID.randomUUID()).replace("\"paymentMethod\"",
                "\"billingAddress\":{\"companyName\":\"Kowalski Sp. z o.o.\",\"taxIdentificationNumber\":\"5260250275\","
                        + "\"street\":\"Firmowa 5\",\"city\":\"Kraków\",\"postalCode\":\"30-001\",\"country\":\"Polska\"},"
                        + "\"paymentMethod\"");

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Validation failed: billingAddress.taxIdentificationNumber: must be 10 digits with a correct checksum"));
        verifyNoInteractions(orderService);
    }

    @Test
    void placeOrder_UnknownDeliveryMethod_Returns400() throws Exception {
        String body = VALID_BODY.formatted(UUID.randomUUID()).replace("COURIER", "DRONE");

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(startsWith("Malformed request body")));
        verifyNoInteractions(orderService);
    }

    @Test
    void placeOrder_OrderAlreadyPlacedFromCart_Returns409() throws Exception {
        UUID cartId = UUID.randomUUID();
        when(orderService.placeOrder(any(PlaceOrderCommand.class)))
                .thenThrow(new OrderAlreadyPlacedException(cartId, "ORD-2026-000001"));

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY.formatted(cartId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Order ORD-2026-000001 has already been placed from cart " + cartId));
    }

    @Test
    void placeOrder_CartNotFound_Returns404() throws Exception {
        UUID cartId = UUID.randomUUID();
        when(orderService.placeOrder(any(PlaceOrderCommand.class))).thenThrow(new CartNotFoundException(cartId));

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY.formatted(cartId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Cart with id " + cartId + " not found"));
    }

    @Test
    void placeOrder_CartServiceUnavailable_Returns503() throws Exception {
        when(orderService.placeOrder(any(PlaceOrderCommand.class))).thenThrow(new CartServiceUnavailableException());

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY.formatted(UUID.randomUUID())))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void getOrderById_DataCorrect_Returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(orderService.getOrderById(id)).thenReturn(sampleOrder(id));

        mockMvc.perform(get("/orders/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.lines[0].stockKeepingUnit").value("PBX15-I7-16"));
    }

    @Test
    void getOrderById_OrderNotFound_Returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(orderService.getOrderById(id)).thenThrow(new OrderNotFoundException(id));

        mockMvc.perform(get("/orders/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Order with id " + id + " not found"));
    }

    @Test
    void getOrders_ByEmail_Returns200() throws Exception {
        when(orderService.getOrders("jan.kowalski@example.com")).thenReturn(List.of(sampleOrder(UUID.randomUUID())));

        mockMvc.perform(get("/orders").param("email", "jan.kowalski@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].customer.email").value("jan.kowalski@example.com"));
        verify(orderService).getOrders("jan.kowalski@example.com");
    }

    @Test
    void getOrders_NoEmail_Returns200() throws Exception {
        when(orderService.getOrders(null)).thenReturn(List.of());

        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getDeliveryMethods_Returns200WithCosts() throws Exception {
        when(orderService.getDeliveryMethods())
                .thenReturn(List.of(DeliveryMethod.COURIER, DeliveryMethod.PARCEL_LOCKER, DeliveryMethod.PICKUP));

        mockMvc.perform(get("/orders/delivery-methods"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].method").value("COURIER"))
                .andExpect(jsonPath("$[0].cost.amount").value(19.99))
                .andExpect(jsonPath("$[0].cost.currency").value("PLN"))
                .andExpect(jsonPath("$[1].method").value("PARCEL_LOCKER"))
                .andExpect(jsonPath("$[1].cost.amount").value(12.99))
                .andExpect(jsonPath("$[2].method").value("PICKUP"))
                .andExpect(jsonPath("$[2].cost.amount").value(0.00));
    }

    @Test
    void getPaymentMethods_Returns200WithNames() throws Exception {
        when(orderService.getPaymentMethods()).thenReturn(List.of(PaymentMethod.CARD, PaymentMethod.BLIK));

        mockMvc.perform(get("/orders/payment-methods"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0]").value("CARD"))
                .andExpect(jsonPath("$[1]").value("BLIK"));
    }

    @Test
    void previewOrder_DataCorrect_Returns200WithSummaryWithoutOrderNumber() throws Exception {
        Order preview = sampleOrder(null);
        preview.setOrderNumber(null);
        preview.setStatus(null);
        when(orderService.previewOrder(any(PlaceOrderCommand.class))).thenReturn(preview);

        mockMvc.perform(post("/orders/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY.formatted(UUID.randomUUID())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").doesNotExist())
                .andExpect(jsonPath("$.customer.firstName").value("Jan"))
                .andExpect(jsonPath("$.deliveryMethod").value("COURIER"))
                .andExpect(jsonPath("$.lines[0].quantity").value(2))
                .andExpect(jsonPath("$.deliveryCost.amount").value(19.99))
                .andExpect(jsonPath("$.grandTotal.amount").value(9256.99));
    }

    @Test
    void previewOrder_EmptyBody_Returns400() throws Exception {
        mockMvc.perform(post("/orders/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(startsWith("Validation failed")));
        verifyNoInteractions(orderService);
    }

    @Test
    void changeOrderStatus_AllowedChange_Returns200() throws Exception {
        UUID id = UUID.randomUUID();
        Order order = sampleOrder(id);
        order.setStatus(OrderStatus.SHIPPED);
        order.setDocumentNumber("FV/2026/10/000001");
        when(orderService.changeOrderStatus(id, OrderStatus.SHIPPED)).thenReturn(order);

        mockMvc.perform(patch("/orders/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SHIPPED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPED"))
                .andExpect(jsonPath("$.documentNumber").value("FV/2026/10/000001"));
    }

    @Test
    void changeOrderStatus_NotAllowedChange_Returns409() throws Exception {
        UUID id = UUID.randomUUID();
        when(orderService.changeOrderStatus(id, OrderStatus.SHIPPED))
                .thenThrow(new InvalidStatusChangeException(id, OrderStatus.NEW, OrderStatus.SHIPPED));

        mockMvc.perform(patch("/orders/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SHIPPED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Order " + id + " cannot change status from NEW to SHIPPED"));
    }

    @Test
    void changeOrderStatus_MissingStatus_Returns400() throws Exception {
        mockMvc.perform(patch("/orders/{id}/status", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed: status: must not be null"));
        verifyNoInteractions(orderService);
    }
}
