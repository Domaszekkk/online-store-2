package com.estore.gateway.cart;

import com.estore.gateway.client.MoneyPayload;

import java.util.List;
import java.util.UUID;

public record CartResponse(UUID id, String status, List<CartItemResponse> items, MoneyPayload total) {
}
