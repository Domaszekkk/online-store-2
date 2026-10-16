package com.estore.order;

import lombok.*;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Order {
    public static final String CURRENCY = "PLN";
    private static final ZoneId STORE_ZONE = ZoneId.of("Europe/Warsaw");

    private UUID id;
    private String orderNumber;
    private UUID cartId;
    private OrderStatus status;
    private Customer customer;
    private Address shippingAddress;
    private Address billingAddress;
    private PaymentMethod paymentMethod;
    private DeliveryMethod deliveryMethod;

    @Builder.Default
    private List<OrderLine> lines = new ArrayList<>();

    private Money itemsTotal;
    private Money deliveryCost;
    private Money grandTotal;
    private Instant createdAt;

    public static Order place(PlaceOrderCommand command, CartDetails cart, long sequenceNumber) {
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new CartNotOrderableException(cart.getId(), "cart is empty");
        }
        Instant now = Instant.now();
        List<OrderLine> lines = new ArrayList<>(cart.getItems().stream().map(OrderLine::fromCartItem).toList());
        Money itemsTotal = lines.stream()
                .map(OrderLine::getLineTotal)
                .reduce(Money.zero(CURRENCY), Money::add);
        Money deliveryCost = command.getDeliveryMethod().cost();

        return Order.builder()
                .id(UUID.randomUUID())
                .orderNumber(formatOrderNumber(now, sequenceNumber))
                .cartId(cart.getId())
                .status(OrderStatus.NEW)
                .customer(command.getCustomer())
                .shippingAddress(command.getShippingAddress())
                .billingAddress(command.getBillingAddress())
                .paymentMethod(command.getPaymentMethod())
                .deliveryMethod(command.getDeliveryMethod())
                .lines(lines)
                .itemsTotal(itemsTotal)
                .deliveryCost(deliveryCost)
                .grandTotal(itemsTotal.add(deliveryCost))
                .createdAt(now)
                .build();
    }

    static String formatOrderNumber(Instant placedAt, long sequenceNumber) {
        return "ORD-%d-%06d".formatted(placedAt.atZone(STORE_ZONE).getYear(), sequenceNumber);
    }
}
