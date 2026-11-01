package com.estore.cart.client;

import java.util.List;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String name,
        List<VariantResponse> variants,
        List<AddOnResponse> addOns
) {
}
