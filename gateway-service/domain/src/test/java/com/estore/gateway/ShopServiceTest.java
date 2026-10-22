package com.estore.gateway;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ShopServiceTest {

    private ProductCatalogPort productCatalogPort;
    private CartPort cartPort;
    private OrderPort orderPort;
    private DocumentPort documentPort;
    private ShopService shopService;

    @BeforeEach
    void setup() {
        this.productCatalogPort = Mockito.mock(ProductCatalogPort.class);
        this.cartPort = Mockito.mock(CartPort.class);
        this.orderPort = Mockito.mock(OrderPort.class);
        this.documentPort = Mockito.mock(DocumentPort.class);
        this.shopService = new ShopService(productCatalogPort, cartPort, orderPort, documentPort);
    }

    private static Money pln(String amount) {
        return Money.of(new BigDecimal(amount), "PLN");
    }

    @Test
    void getCheckoutOptions_CombinesPaymentAndDeliveryMethodsFromOrderService() {
        List<DeliveryOption> deliveryOptions = List.of(
                DeliveryOption.builder().method("COURIER").cost(pln("19.99")).build(),
                DeliveryOption.builder().method("PICKUP").cost(pln("0.00")).build());
        when(orderPort.getPaymentMethods()).thenReturn(List.of("CARD", "BLIK"));
        when(orderPort.getDeliveryOptions()).thenReturn(deliveryOptions);

        CheckoutOptions result = shopService.getCheckoutOptions();

        assertAll(
                () -> assertEquals(List.of("CARD", "BLIK"), result.getPaymentMethods()),
                () -> assertEquals(deliveryOptions, result.getDeliveryMethods())
        );
    }

    @Test
    void findOrderDocument_DocumentIssued_ReturnsDocument() {
        UUID orderId = UUID.randomUUID();
        OrderDocument document = OrderDocument.builder().fileName("faktura-FV-2026-10-000001.pdf").content(new byte[]{1}).build();
        when(orderPort.getOrder(orderId)).thenReturn(Order.builder().id(orderId).build());
        when(documentPort.findByOrderId(orderId)).thenReturn(Optional.of(document));

        Optional<OrderDocument> result = shopService.findOrderDocument(orderId);

        assertSame(document, result.orElseThrow());
    }

    @Test
    void findOrderDocument_DocumentNotIssuedYet_ReturnsEmpty() {
        UUID orderId = UUID.randomUUID();
        when(orderPort.getOrder(orderId)).thenReturn(Order.builder().id(orderId).build());
        when(documentPort.findByOrderId(orderId)).thenReturn(Optional.empty());

        assertTrue(shopService.findOrderDocument(orderId).isEmpty());
    }

    @Test
    void findOrderDocument_OrderNotFound_ThrowsWithoutAskingForDocument() {
        UUID orderId = UUID.randomUUID();
        when(orderPort.getOrder(orderId))
                .thenThrow(new ServiceRejectedRequestException("Order with id " + orderId + " not found", 404));

        ServiceRejectedRequestException exception = Assertions.assertThrows(ServiceRejectedRequestException.class,
                () -> shopService.findOrderDocument(orderId));

        assertEquals(404, exception.getResponseCode());
        verifyNoInteractions(documentPort);
    }

    @Test
    void addItem_PassesCommandToCart() {
        UUID cartId = UUID.randomUUID();
        AddItemCommand command = AddItemCommand.builder()
                .productId(UUID.randomUUID()).variantId(UUID.randomUUID()).quantity(1).addOns(List.of()).build();
        Cart cart = Cart.builder().id(cartId).build();
        when(cartPort.addItem(cartId, command)).thenReturn(cart);

        assertSame(cart, shopService.addItem(cartId, command));
        verify(cartPort).addItem(cartId, command);
    }
}
