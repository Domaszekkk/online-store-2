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
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderServiceTest {

    private OrderRepositoryPort orderRepositoryPort;
    private CartPort cartPort;
    private OrderEventPort orderEventPort;
    private TransactionPort transactionPort;
    private OrderService orderService;

    @BeforeEach
    void setup() {
        this.orderRepositoryPort = Mockito.mock(OrderRepositoryPort.class);
        this.cartPort = Mockito.mock(CartPort.class);
        this.orderEventPort = Mockito.mock(OrderEventPort.class);
        this.transactionPort = Mockito.mock(TransactionPort.class);
        this.orderService = new OrderService(orderRepositoryPort, cartPort, orderEventPort, transactionPort);
        when(transactionPort.inTransaction(any())).thenAnswer(invocation -> invocation.<Supplier<?>>getArgument(0).get());
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
                        .vatRate(VatRate.VAT_23)
                        .quantity(2)
                        .addOns(List.of(CartItemAddOnDetails.builder()
                                .productId(UUID.randomUUID())
                                .name("Kabel HDMI 2m")
                                .price(pln("39.00"))
                                .vatRate(VatRate.VAT_8)
                                .quantity(1)
                                .build()))
                        .build()))
                .build();
    }

    private Address address(String street) {
        return Address.builder().street(street).city("Warszawa").postalCode("00-001").country("Polska").build();
    }

    private BillingAddress billingAddress(String street, String companyName, String taxIdentificationNumber) {
        return BillingAddress.builder().companyName(companyName).taxIdentificationNumber(taxIdentificationNumber)
                .street(street).city("Warszawa").postalCode("00-001").country("Polska").build();
    }

    private PlaceOrderCommand command(UUID cartId, DeliveryMethod deliveryMethod, BillingAddress billingAddress) {
        return PlaceOrderCommand.builder()
                .cartId(cartId)
                .cartVersion(1L)
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
        when(cartPort.checkout(cartId, 1L)).thenReturn(cartWithTwoLaptopsAndOneCable(cartId));

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
                () -> assertEquals("PLN", result.getGrandTotal().currency()),
                () -> assertEquals(VatRate.VAT_23, result.getLines().get(0).getVatRate()),
                () -> assertEquals(VatRate.VAT_8, result.getLines().get(0).getAddOns().get(0).getVatRate())
        );
        verify(orderRepositoryPort).save(result);
        verify(orderEventPort).orderPlaced(result);
    }

    @Test
    void placeOrder_NoBillingAddress_OrderHasNoBillingAddress() {
        UUID cartId = UUID.randomUUID();
        when(cartPort.checkout(cartId, 1L)).thenReturn(cartWithTwoLaptopsAndOneCable(cartId));

        Order result = orderService.placeOrder(command(cartId, DeliveryMethod.COURIER, null));

        assertNull(result.getBillingAddress());
    }

    @Test
    void placeOrder_SeparateBillingAddress_KeepsBillingAddress() {
        UUID cartId = UUID.randomUUID();
        when(cartPort.checkout(cartId, 1L)).thenReturn(cartWithTwoLaptopsAndOneCable(cartId));

        Order result = orderService.placeOrder(command(cartId, DeliveryMethod.COURIER, billingAddress("Firmowa 5", null, null)));

        assertAll(
                () -> assertEquals("Marszałkowska 1", result.getShippingAddress().getStreet()),
                () -> assertEquals("Firmowa 5", result.getBillingAddress().getStreet()),
                () -> assertEquals(false, result.getBillingAddress().isCompany())
        );
    }

    @Test
    void placeOrder_CompanyBillingAddress_KeepsCompanyData() {
        UUID cartId = UUID.randomUUID();
        when(cartPort.checkout(cartId, 1L)).thenReturn(cartWithTwoLaptopsAndOneCable(cartId));

        Order result = orderService.placeOrder(command(cartId, DeliveryMethod.COURIER,
                billingAddress("Firmowa 5", "Kowalski Sp. z o.o.", "5260250274")));

        assertAll(
                () -> assertEquals("Kowalski Sp. z o.o.", result.getBillingAddress().getCompanyName()),
                () -> assertEquals("5260250274", result.getBillingAddress().getTaxIdentificationNumber()),
                () -> assertEquals(true, result.getBillingAddress().isCompany())
        );
    }

    @Test
    void placeOrder_CompanyNameWithoutTaxIdentificationNumber_ThrowsAndDoesNotCheckoutCart() {
        UUID cartId = UUID.randomUUID();

        InvalidBillingAddressException exception = Assertions.assertThrows(InvalidBillingAddressException.class,
                () -> orderService.placeOrder(command(cartId, DeliveryMethod.COURIER,
                        billingAddress("Firmowa 5", "Kowalski Sp. z o.o.", null))));

        assertEquals(400, exception.getResponseCode());
        verify(cartPort, never()).checkout(any(), anyLong());
        verify(orderRepositoryPort, never()).save(any());
    }

    @Test
    void placeOrder_TaxIdentificationNumberWithWrongChecksum_ThrowsAndDoesNotCheckoutCart() {
        UUID cartId = UUID.randomUUID();

        InvalidBillingAddressException exception = Assertions.assertThrows(InvalidBillingAddressException.class,
                () -> orderService.placeOrder(command(cartId, DeliveryMethod.COURIER,
                        billingAddress("Firmowa 5", "Kowalski Sp. z o.o.", "5260250275"))));

        assertEquals(400, exception.getResponseCode());
        verify(cartPort, never()).checkout(any(), anyLong());
    }

    @Test
    void placeOrder_Pickup_DeliveryIsFree() {
        UUID cartId = UUID.randomUUID();
        when(cartPort.checkout(cartId, 1L)).thenReturn(cartWithTwoLaptopsAndOneCable(cartId));

        Order result = orderService.placeOrder(command(cartId, DeliveryMethod.PICKUP, null));

        assertAll(
                () -> assertAmount("0.00", result.getDeliveryCost()),
                () -> assertAmount("9237.00", result.getGrandTotal())
        );
    }

    @Test
    void placeOrder_CartEmpty_ThrowsCartNotOrderableExceptionAndSavesNothing() {
        UUID cartId = UUID.randomUUID();
        when(cartPort.checkout(cartId, 1L))
                .thenReturn(CartDetails.builder().id(cartId).items(new ArrayList<>()).build());

        CartNotOrderableException exception = Assertions.assertThrows(CartNotOrderableException.class,
                () -> orderService.placeOrder(command(cartId, DeliveryMethod.COURIER, null)));

        assertEquals(409, exception.getResponseCode());
        verify(orderRepositoryPort, never()).save(any());
    }

    @Test
    void placeOrder_CartServiceRejectsCheckout_PropagatesAndSavesNothing() {
        UUID cartId = UUID.randomUUID();
        when(cartPort.checkout(cartId, 1L))
                .thenThrow(new CartNotOrderableException(cartId, "cart is already checked out"));

        CartNotOrderableException exception = Assertions.assertThrows(CartNotOrderableException.class,
                () -> orderService.placeOrder(command(cartId, DeliveryMethod.COURIER, null)));

        assertEquals(409, exception.getResponseCode());
        verify(orderRepositoryPort, never()).save(any());
        verify(orderRepositoryPort, never()).nextOrderSequenceNumber();
        verify(orderEventPort, never()).orderPlaced(any());
    }

    @Test
    void placeOrder_OrderAlreadyPlacedFromCart_ThrowsConflictWithoutCheckout() {
        UUID cartId = UUID.randomUUID();
        when(orderRepositoryPort.findByCartId(cartId))
                .thenReturn(Optional.of(Order.builder().cartId(cartId).orderNumber("ORD-2026-000007").build()));

        OrderAlreadyPlacedException exception = Assertions.assertThrows(OrderAlreadyPlacedException.class,
                () -> orderService.placeOrder(command(cartId, DeliveryMethod.COURIER, null)));

        assertAll(
                () -> assertEquals(409, exception.getResponseCode()),
                () -> assertEquals("Order ORD-2026-000007 has already been placed from cart " + cartId, exception.getMessage())
        );
        verify(cartPort, never()).checkout(any(), anyLong());
        verify(orderRepositoryPort, never()).save(any());
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
    void getOrders_NoEmail_ReturnsPageOfAllOrders() {
        PageQuery pageQuery = new PageQuery(0, 20);
        ResultPage<Order> orders = new ResultPage<>(List.of(new Order(), new Order()), 0, 20, 2);
        when(orderRepositoryPort.findAll(pageQuery)).thenReturn(orders);

        assertEquals(orders, orderService.getOrders(null, pageQuery));
        assertEquals(orders, orderService.getOrders("  ", pageQuery));
        verify(orderRepositoryPort, never()).findAllByCustomerEmail(any(), any());
    }

    @Test
    void getOrders_Email_ReturnsPageOfOrdersOfThatCustomer() {
        PageQuery pageQuery = new PageQuery(2, 10);
        ResultPage<Order> orders = new ResultPage<>(List.of(new Order()), 2, 10, 21);
        when(orderRepositoryPort.findAllByCustomerEmail("jan.kowalski@example.com", pageQuery)).thenReturn(orders);

        ResultPage<Order> result = orderService.getOrders(" jan.kowalski@example.com ", pageQuery);

        assertAll(
                () -> assertEquals(orders, result),
                () -> assertEquals(3, result.totalPages())
        );
    }

    @Test
    void getDeliveryMethods_ReturnsAllMethodsWithCosts() {
        List<DeliveryMethod> deliveryMethods = orderService.getDeliveryMethods();

        assertAll(
                () -> assertEquals(List.of(DeliveryMethod.COURIER, DeliveryMethod.PARCEL_LOCKER, DeliveryMethod.PICKUP),
                        deliveryMethods),
                () -> assertAmount("19.99", deliveryMethods.get(0).cost()),
                () -> assertAmount("12.99", deliveryMethods.get(1).cost()),
                () -> assertAmount("0.00", deliveryMethods.get(2).cost())
        );
    }

    @Test
    void getPaymentMethods_ReturnsAllMethods() {
        assertEquals(List.of(PaymentMethod.CARD, PaymentMethod.BLIK, PaymentMethod.TRANSFER, PaymentMethod.CASH_ON_DELIVERY),
                orderService.getPaymentMethods());
    }

    private Order placedOrder(UUID orderId, OrderStatus status) {
        return Order.builder().id(orderId).orderNumber("ORD-2026-000042").status(status).build();
    }

    @Test
    void previewOrder_DataCorrect_ReturnsSummaryWithoutPlacingOrder() {
        UUID cartId = UUID.randomUUID();
        when(orderRepositoryPort.findByCartId(cartId)).thenReturn(Optional.empty());
        when(cartPort.refreshCart(cartId)).thenReturn(cartWithTwoLaptopsAndOneCable(cartId));

        Order result = orderService.previewOrder(command(cartId, DeliveryMethod.COURIER, null));

        assertAll(
                () -> assertNull(result.getId()),
                () -> assertNull(result.getOrderNumber()),
                () -> assertNull(result.getStatus()),
                () -> assertEquals("Jan", result.getCustomer().getFirstName()),
                () -> assertEquals(1, result.getLines().size()),
                () -> assertAmount("9237.00", result.getItemsTotal()),
                () -> assertAmount("19.99", result.getDeliveryCost()),
                () -> assertAmount("9256.99", result.getGrandTotal())
        );
        verify(cartPort, never()).checkout(any(), anyLong());
        verify(orderRepositoryPort, never()).save(any());
        verify(orderEventPort, never()).orderPlaced(any());
    }

    @Test
    void previewOrder_OrderAlreadyPlaced_ThrowsOrderAlreadyPlacedException() {
        UUID cartId = UUID.randomUUID();
        when(orderRepositoryPort.findByCartId(cartId))
                .thenReturn(Optional.of(placedOrder(UUID.randomUUID(), OrderStatus.NEW)));

        OrderAlreadyPlacedException exception = Assertions.assertThrows(OrderAlreadyPlacedException.class,
                () -> orderService.previewOrder(command(cartId, DeliveryMethod.COURIER, null)));

        assertEquals(409, exception.getResponseCode());
        verify(cartPort, never()).refreshCart(any());
    }

    @Test
    void previewOrder_InvalidTaxIdentificationNumber_ThrowsInvalidBillingAddressException() {
        UUID cartId = UUID.randomUUID();
        PlaceOrderCommand command = command(cartId, DeliveryMethod.COURIER,
                billingAddress("Firmowa 5", "Kowalski Sp. z o.o.", "5260250275"));

        Assertions.assertThrows(InvalidBillingAddressException.class, () -> orderService.previewOrder(command));

        verify(cartPort, never()).refreshCart(any());
    }

    @Test
    void previewOrder_EmptyCart_ThrowsCartNotOrderableException() {
        UUID cartId = UUID.randomUUID();
        when(orderRepositoryPort.findByCartId(cartId)).thenReturn(Optional.empty());
        when(cartPort.refreshCart(cartId)).thenReturn(CartDetails.builder().id(cartId).items(new ArrayList<>()).build());

        CartNotOrderableException exception = Assertions.assertThrows(CartNotOrderableException.class,
                () -> orderService.previewOrder(command(cartId, DeliveryMethod.COURIER, null)));

        assertEquals(409, exception.getResponseCode());
    }

    @Test
    void confirmOrder_NewOrder_SetsConfirmedStatusAndDocumentNumber() {
        UUID orderId = UUID.randomUUID();
        when(orderRepositoryPort.findByIdForUpdate(orderId)).thenReturn(Optional.of(placedOrder(orderId, OrderStatus.NEW)));

        Order result = orderService.confirmOrder(orderId, "FV/2026/10/000001");

        assertAll(
                () -> assertEquals(OrderStatus.CONFIRMED, result.getStatus()),
                () -> assertEquals("FV/2026/10/000001", result.getDocumentNumber())
        );
        verify(orderRepositoryPort).save(result);
    }

    @Test
    void confirmOrder_CancelledOrder_KeepsStatusAndStoresDocumentNumber() {
        UUID orderId = UUID.randomUUID();
        when(orderRepositoryPort.findByIdForUpdate(orderId))
                .thenReturn(Optional.of(placedOrder(orderId, OrderStatus.CANCELLED)));

        Order result = orderService.confirmOrder(orderId, "PZ/2026/10/000001");

        assertAll(
                () -> assertEquals(OrderStatus.CANCELLED, result.getStatus()),
                () -> assertEquals("PZ/2026/10/000001", result.getDocumentNumber())
        );
    }

    @Test
    void confirmOrder_OrderNotFound_ThrowsOrderNotFoundException() {
        UUID orderId = UUID.randomUUID();
        when(orderRepositoryPort.findByIdForUpdate(orderId)).thenReturn(Optional.empty());

        Assertions.assertThrows(OrderNotFoundException.class, () -> orderService.confirmOrder(orderId, "FV/2026/10/000001"));

        verify(orderRepositoryPort, never()).save(any());
    }

    @Test
    void changeOrderStatus_ConfirmedToShipped_ChangesStatus() {
        UUID orderId = UUID.randomUUID();
        when(orderRepositoryPort.findByIdForUpdate(orderId))
                .thenReturn(Optional.of(placedOrder(orderId, OrderStatus.CONFIRMED)));

        Order result = orderService.changeOrderStatus(orderId, OrderStatus.SHIPPED);

        assertEquals(OrderStatus.SHIPPED, result.getStatus());
    }

    @Test
    void changeOrderStatus_NewToShipped_ThrowsInvalidStatusChangeException() {
        UUID orderId = UUID.randomUUID();
        when(orderRepositoryPort.findByIdForUpdate(orderId)).thenReturn(Optional.of(placedOrder(orderId, OrderStatus.NEW)));

        InvalidStatusChangeException exception = Assertions.assertThrows(InvalidStatusChangeException.class,
                () -> orderService.changeOrderStatus(orderId, OrderStatus.SHIPPED));

        assertAll(
                () -> assertEquals(409, exception.getResponseCode()),
                () -> assertEquals("Order " + orderId + " cannot change status from NEW to SHIPPED", exception.getMessage())
        );
        verify(orderRepositoryPort, never()).save(any());
    }

    @Test
    void changeOrderStatus_ToConfirmedByHand_ThrowsInvalidStatusChangeException() {
        UUID orderId = UUID.randomUUID();
        when(orderRepositoryPort.findByIdForUpdate(orderId)).thenReturn(Optional.of(placedOrder(orderId, OrderStatus.NEW)));

        Assertions.assertThrows(InvalidStatusChangeException.class,
                () -> orderService.changeOrderStatus(orderId, OrderStatus.CONFIRMED));
    }

    @Test
    void changeOrderStatus_CompletedOrderCancelled_ThrowsInvalidStatusChangeException() {
        UUID orderId = UUID.randomUUID();
        when(orderRepositoryPort.findByIdForUpdate(orderId))
                .thenReturn(Optional.of(placedOrder(orderId, OrderStatus.COMPLETED)));

        Assertions.assertThrows(InvalidStatusChangeException.class,
                () -> orderService.changeOrderStatus(orderId, OrderStatus.CANCELLED));
    }
}
