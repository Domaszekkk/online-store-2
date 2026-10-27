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
    private BillingAddress billingAddress;
    private PaymentMethod paymentMethod;
    private DeliveryMethod deliveryMethod;

    @Builder.Default
    private List<OrderLine> lines = new ArrayList<>();

    private Money itemsTotal;
    private Money deliveryCost;
    private Money grandTotal;
    private String documentNumber;
    private Instant createdAt;

    public static Order preview(PlaceOrderCommand command, CartDetails cart) {
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new CartNotOrderableException(cart.getId(), "cart is empty");
        }
        List<OrderLine> lines = new ArrayList<>(cart.getItems().stream().map(OrderLine::fromCartItem).toList());
        Money itemsTotal = lines.stream()
                .map(OrderLine::getLineTotal)
                .reduce(Money.zero(CURRENCY), Money::add);
        Money deliveryCost = command.getDeliveryMethod().cost();

        return Order.builder()
                .cartId(cart.getId())
                .customer(command.getCustomer())
                .shippingAddress(command.getShippingAddress())
                .billingAddress(command.getBillingAddress())
                .paymentMethod(command.getPaymentMethod())
                .deliveryMethod(command.getDeliveryMethod())
                .lines(lines)
                .itemsTotal(itemsTotal)
                .deliveryCost(deliveryCost)
                .grandTotal(itemsTotal.add(deliveryCost))
                .build();
    }

    public static Order place(PlaceOrderCommand command, CartDetails cart, long sequenceNumber) {
        Order order = preview(command, cart);
        Instant now = Instant.now();
        order.id = UUID.randomUUID();
        order.orderNumber = formatOrderNumber(now, sequenceNumber);
        order.status = OrderStatus.NEW;
        order.createdAt = now;
        return order;
    }

    public void confirm(String documentNumber) {
        this.documentNumber = documentNumber;
        if (status == OrderStatus.NEW) {
            this.status = OrderStatus.CONFIRMED;
        }
    }

    public void changeStatus(OrderStatus targetStatus) {
        if (targetStatus == OrderStatus.CONFIRMED || !status.canChangeTo(targetStatus)) {
            throw new InvalidStatusChangeException(id, status, targetStatus);
        }
        this.status = targetStatus;
    }

    static String formatOrderNumber(Instant placedAt, long sequenceNumber) {
        return "ORD-%d-%06d".formatted(placedAt.atZone(STORE_ZONE).getYear(), sequenceNumber);
    }
}
