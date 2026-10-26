package com.estore.gateway.product;

import com.estore.gateway.client.MoneyPayload;

import java.util.Map;
import java.util.UUID;

public record VariantResponse(UUID id, Map<String, String> optionValues, MoneyPayload price, String availability) {
}
