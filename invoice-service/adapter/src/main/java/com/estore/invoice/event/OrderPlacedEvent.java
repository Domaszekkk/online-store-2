package com.estore.invoice.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderPlacedEvent(
        UUID orderId,
        String orderNumber,
        Instant placedAt,
        CustomerPayload customer,
        BillingAddressPayload billingAddress,
        String paymentMethod,
        String deliveryMethod,
        MoneyPayload deliveryCost,
        String deliveryVatRate,
        List<OrderLinePayload> lines
) {
    public static final String TYPE = "OrderPlaced";
}
