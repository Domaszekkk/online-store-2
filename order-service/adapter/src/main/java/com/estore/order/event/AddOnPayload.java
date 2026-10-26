package com.estore.order.event;

import java.util.UUID;

public record AddOnPayload(UUID productId, String name, MoneyPayload price, String vatRate, int quantity) {
}
