package com.estore.gateway;

import lombok.RequiredArgsConstructor;

import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
public class ShopService {
    private final ProductCatalogPort productCatalogPort;
    private final CartPort cartPort;
    private final OrderPort orderPort;
    private final DocumentPort documentPort;

    public ResultPage<ProductSummary> getProducts(String type, int page, int size) {
        return productCatalogPort.findProducts(type, page, size);
    }

    public ProductDetails getProduct(UUID productId) {
        return productCatalogPort.getProduct(productId);
    }

    public Cart createCart() {
        return cartPort.createCart();
    }

    public Cart getCart(UUID cartId) {
        return cartPort.getCart(cartId);
    }

    public Cart addItem(UUID cartId, AddItemCommand command) {
        return cartPort.addItem(cartId, command);
    }

    public Cart changeItemQuantity(UUID cartId, UUID itemId, int quantity) {
        return cartPort.changeItemQuantity(cartId, itemId, quantity);
    }

    public Cart removeItem(UUID cartId, UUID itemId) {
        return cartPort.removeItem(cartId, itemId);
    }

    public CheckoutOptions getCheckoutOptions() {
        return CheckoutOptions.builder()
                .paymentMethods(orderPort.getPaymentMethods())
                .deliveryMethods(orderPort.getDeliveryOptions())
                .build();
    }

    public OrderPreview previewOrder(PlaceOrderCommand command) {
        return orderPort.previewOrder(command);
    }

    public Order placeOrder(PlaceOrderCommand command) {
        return orderPort.placeOrder(command);
    }

    public Order getOrder(UUID orderId) {
        return orderPort.getOrder(orderId);
    }

    public Optional<OrderDocument> findOrderDocument(UUID orderId) {
        orderPort.getOrder(orderId);
        return documentPort.findByOrderId(orderId);
    }
}
