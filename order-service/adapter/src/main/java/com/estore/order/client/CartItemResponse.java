package com.estore.order.client;

import java.util.List;
import java.util.UUID;

public record CartItemResponse(
        UUID productId,
        UUID variantId,
        String productName,
        String stockKeepingUnit,
        MoneyPayload unitPrice,
        String vatRate,
        int quantity,
        List<CartItemAddOnResponse> addOns
) {
}
