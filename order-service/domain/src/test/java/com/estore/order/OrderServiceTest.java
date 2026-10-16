package com.estore.order;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.Year;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderServiceTest {

    private OrderRepositoryPort orderRepositoryPort;
    private CartCheckoutPort cartCheckoutPort;
    private OrderService orderService;

    @BeforeEach
    void setup() {
        this.orderRepositoryPort = Mockito.mock(OrderRepositoryPort.class);
        this.cartCheckoutPort = Mockito.mock(CartCheckoutPort.class);
        this.orderService = new OrderService(orderRepositoryPort, cartCheckoutPort);
        when(orderRepositoryPort.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepositoryPort.nextOrderSequenceNumber()).thenReturn(42L);
    }

    private static Money pln(String amount) {
        return Money.of(new BigDecimal(amount), "PLN");
    }

    private static void assertAmount(String expected, Money actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual.amount()),
                () -> "expected " + expected + " but was " + actual.amount());
    }

    private CartDetails cartWithTwoLaptopsAndOneCable(UUID cartId) {
        return CartDetails.builder()
                .id(cartId)
                .items(List.of(CartItemDetails.builder()
                        .productId(UUID.randomUUID())
                        .variantId(UUID.randomUUID())
                        .productName("ProBook X15")
                        .stockKeepingUnit("PBX15-I7-16")
                        .unitPrice(pln("4599.00"))
                        .quantity(2)
                        .addOns(List.of(CartItemAddOnDetails.builder()
                                .productId(UUID.randomUUID())
                                .name("Kabel HDMI 2m")
                                .price(pln("39.00"))
                                .quantity(1)
                                .build()))
                        .build()))
                .build();
    }

    private Address address(String street) {
        return Address.builder().street(street).city("Warszawa").postalCode("00-001").country("Polska").build();
    }

    private PlaceOrderCommand command(UUID cartId, DeliveryMethod deliveryMethod, Address billingAddress) {
        return PlaceOrderCommand.builder()
                .cartId(cartId)
                .customer(Customer.builder()
                        .firstName("Jan").lastName("Kowalski").email("jan.kowalski@example.com").phone("+48 600 100 200")
                        .build())
                .shippingAddress(address("Marszałkowska 1"))
                .billingAddress(billingAddress)
                .paymentMethod(PaymentMethod.BLIK)
                .deliveryMethod(deliveryMethod)
                .build();
    }

    @Test
    void placeOrder_DataCorrect_SnapshotsCartAndComputesTotals() {
        UUID cartId = UUID.randomUUID();
        when(cartCheckoutPort.checkout(cartId)).thenReturn(cartWithTwoLaptopsAndOneCable(cartId));

        Order result = orderService.placeOrder(command(cartId, DeliveryMethod.COURIER, null));

        int year = Year.now(ZoneId.of("Europe/Warsaw")).getValue();
        assertAll(
                () -> assertNotNull(result.getId()),
                () -> assertEquals("ORD-" + year + "-000042", result.getOrderNumber()),
                () -> assertEquals(OrderStatus.NEW, result.getStatus()),
                () -> assertEquals(cartId, result.getCartId()),
                () -> assertEquals(1, result.getLines().size()),
                () -> assertEquals("PBX15-I7-16", result.getLines().get(0).getStockKeepingUnit()),
                () -> assertEquals(2, result.getLines().get(0).getQuantity()),
                () -> assertEquals(1, result.getLines().get(0).getAddOns().get(0).getQuantity()),
                () -> assertAmount("9237.00", result.getLines().get(0).getLineTotal()),
                () -> assertAmount("9237.00", result.getItemsTotal()),
                () -> assertAmount("19.99", result.getDeliveryCost()),
                () -> assertAmount("9256.99", result.getGrandTotal()),
                () -> assertEquals("PLN", result.getGrandTotal().currency())
        );
        verify(orderRepositoryPort).save(result);
    }

    @Test
    void placeOrder_NoBillingAddress_OrderHasNoBillingAddress() {
        UUID cartId = UUID.randomUUID();
        when(cartCheckoutPort.checkout(cartId)).thenReturn(cartWithTwoLaptopsAndOneCable(cartId));

        Order result = orderService.placeOrder(command(cartId, DeliveryMethod.COURIER, null));

        assertNull(result.getBillingAddress());
    }

    @Test
    void placeOrder_SeparateBillingAddress_KeepsBillingAddress() {
        UUID cartId = UUID.randomUUID();
        when(cartCheckoutPort.checkout(cartId)).thenReturn(cartWithTwoLaptopsAndOneCable(cartId));

        Order result = orderService.placeOrder(command(cartId, DeliveryMethod.COURIER, address("Firmowa 5")));

        assertAll(
                () -> assertEquals("Marszałkowska 1", result.getShippingAddress().getStreet()),
                () -> assertEquals("Firmowa 5", result.getBillingAddress().getStreet())
        );
    }

    @Test
    void placeOrder_Pickup_DeliveryIsFree() {
        UUID cartId = UUID.randomUUID();
        when(cartCheckoutPort.checkout(cartId)).thenReturn(cartWithTwoLaptopsAndOneCable(cartId));

        Order result = orderService.placeOrder(command(cartId, DeliveryMethod.PICKUP, null));

        assertAll(
                () -> assertAmount("0.00", result.getDeliveryCost()),
                () -> assertAmount("9237.00", result.getGrandTotal())
        );
    }

    @Test
    void placeOrder_CartEmpty_ThrowsCartNotOrderableExceptionAndSavesNothing() {
        UUID cartId = UUID.randomUUID();
        when(cartCheckoutPort.checkout(cartId))
                .thenReturn(CartDetails.builder().id(cartId).items(new ArrayList<>()).build());

        CartNotOrderableException exception = Assertions.assertThrows(CartNotOrderableException.class,
                () -> orderService.placeOrder(command(cartId, DeliveryMethod.COURIER, null)));

        assertEquals(409, exception.getResponseCode());
        verify(orderRepositoryPort, never()).save(any());
    }

    @Test
    void placeOrder_CartServiceRejectsCheckout_PropagatesAndSavesNothing() {
        UUID cartId = UUID.randomUUID();
        when(cartCheckoutPort.checkout(cartId))
                .thenThrow(new CartNotOrderableException(cartId, "cart is already checked out"));

        CartNotOrderableException exception = Assertions.assertThrows(CartNotOrderableException.class,
                () -> orderService.placeOrder(command(cartId, DeliveryMethod.COURIER, null)));

        assertEquals(409, exception.getResponseCode());
        verify(orderRepositoryPort, never()).save(any());
        verify(orderRepositoryPort, never()).nextOrderSequenceNumber();
    }

    @Test
    void getOrderById_OrderNotFound_ThrowsOrderNotFoundException() {
        UUID id = UUID.randomUUID();
        when(orderRepositoryPort.findById(id)).thenReturn(Optional.empty());

        OrderNotFoundException exception = Assertions.assertThrows(
                OrderNotFoundException.class, () -> orderService.getOrderById(id));

        assertEquals(404, exception.getResponseCode());
    }

    @Test
    void getOrders_NoEmail_ReturnsAllOrders() {
        List<Order> orders = List.of(new Order(), new Order());
        when(orderRepositoryPort.findAll()).thenReturn(orders);

        assertEquals(orders, orderService.getOrders(null));
        assertEquals(orders, orderService.getOrders("  "));
        verify(orderRepositoryPort, never()).findAllByCustomerEmail(any());
    }

    @Test
    void getOrders_Email_ReturnsOrdersOfThatCustomer() {
        List<Order> orders = List.of(new Order());
        when(orderRepositoryPort.findAllByCustomerEmail("jan.kowalski@example.com")).thenReturn(orders);

        assertEquals(orders, orderService.getOrders(" jan.kowalski@example.com "));
    }
}
