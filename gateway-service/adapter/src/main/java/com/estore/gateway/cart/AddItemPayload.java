package com.estore.gateway.cart;

import java.util.List;
import java.util.UUID;

public record AddItemPayload(UUID productId, UUID variantId, int quantity, List<AddOnSelectionPayload> addOns) {
}
