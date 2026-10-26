package com.estore.order.event;

import java.util.List;
import java.util.UUID;

public record OrderLinePayload(UUID productId,
                               UUID variantId,
                               String productName,
                               String stockKeepingUnit,
                               MoneyPayload unitPrice,
                               String vatRate,
                               int quantity,
                               List<AddOnPayload> addOns,
                               MoneyPayload lineTotal) {
}
