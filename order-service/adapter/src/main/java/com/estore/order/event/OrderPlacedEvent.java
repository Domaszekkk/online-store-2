package com.estore.order.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderPlacedEvent(
        UUID eventId,
        UUID orderId,
        String orderNumber,
        Instant placedAt,
        CustomerPayload customer,
        BillingAddressPayload billingAddress,
        String paymentMethod,
        String deliveryMethod,
        MoneyPayload deliveryCost,
        String deliveryVatRate,
        List<OrderLinePayload> lines,
        MoneyPayload itemsTotal,
        MoneyPayload grandTotal
) {
    public static final String TYPE = "OrderPlaced";
}
