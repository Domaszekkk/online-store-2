package com.estore.gateway.product;

import com.estore.gateway.client.MoneyPayload;

import java.util.UUID;

public record AddOnResponse(UUID productId, String name, MoneyPayload price) {
}
