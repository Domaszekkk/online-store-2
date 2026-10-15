package com.estore.cart.client;

import java.util.UUID;

public record AddOnResponse(
        UUID productId,
        String name,
        MoneyPayload price
) {
}
