package com.estore.gateway.cart;

import java.util.UUID;

public record AddOnSelectionPayload(UUID productId, int quantity) {
}
