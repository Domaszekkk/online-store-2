package com.estore.order.client;

import java.util.UUID;

public record CartItemAddOnResponse(
        UUID productId,
        String name,
        MoneyPayload price,
        String vatRate,
        int quantity
) {
}
