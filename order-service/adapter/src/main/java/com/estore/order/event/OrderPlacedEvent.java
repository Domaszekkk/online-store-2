package com.estore.order.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderPlacedEvent(
        UUID eventId,
        UUID orderId,
        String orderNumber,
        Instant placedAt,
        Customer customer,
        BillingAddress billingAddress,
        String paymentMethod,
        String deliveryMethod,
        Money deliveryCost,
        String deliveryVatRate,
        List<Line> lines,
        Money itemsTotal,
        Money grandTotal
) {
    public static final String TYPE = "OrderPlaced";

    public record Customer(String firstName, String lastName, String email, String phone) {
    }

    public record BillingAddress(String companyName,
                                 String taxIdentificationNumber,
                                 String street,
                                 String city,
                                 String postalCode,
                                 String country) {
    }

    public record Line(UUID productId,
                       UUID variantId,
                       String productName,
                       String stockKeepingUnit,
                       Money unitPrice,
                       String vatRate,
                       int quantity,
                       List<AddOn> addOns,
                       Money lineTotal) {
    }

    public record AddOn(UUID productId, String name, Money price, String vatRate, int quantity) {
    }

    public record Money(BigDecimal amount, String currency) {
    }
}
