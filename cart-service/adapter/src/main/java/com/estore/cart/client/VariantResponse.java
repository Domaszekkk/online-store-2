package com.estore.cart.client;

import java.util.UUID;

public record VariantResponse(
        UUID id,
        String stockKeepingUnit,
        MoneyPayload price,
        String availability
) {
}
