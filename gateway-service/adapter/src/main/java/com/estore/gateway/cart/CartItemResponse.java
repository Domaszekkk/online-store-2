package com.estore.gateway.cart;

import com.estore.gateway.client.MoneyPayload;

import java.util.List;
import java.util.UUID;

public record CartItemResponse(UUID id,
                               UUID productId,
                               UUID variantId,
                               String productName,
                               MoneyPayload unitPrice,
                               int quantity,
                               List<CartItemAddOnResponse> addOns,
                               MoneyPayload lineTotal,
                               boolean available) {
}
