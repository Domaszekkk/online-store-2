package com.estore.gateway.product;

import com.estore.gateway.client.MoneyPayload;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ProductResponse(UUID id,
                              String name,
                              String type,
                              String description,
                              MoneyPayload basePrice,
                              Map<String, String> attributes,
                              List<ProductOptionResponse> options,
                              List<VariantResponse> variants,
                              List<AddOnResponse> addOns) {
}
