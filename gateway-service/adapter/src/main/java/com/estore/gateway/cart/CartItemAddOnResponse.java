package com.estore.gateway.cart;

import com.estore.gateway.client.MoneyPayload;

import java.util.UUID;

public record CartItemAddOnResponse(UUID productId, String name, MoneyPayload price, int quantity) {
}
