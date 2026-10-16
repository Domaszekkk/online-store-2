package com.estore.order.client;

import java.util.List;
import java.util.UUID;

public record CartResponse(
        UUID id,
        String status,
        List<CartItemResponse> items
) {
}
