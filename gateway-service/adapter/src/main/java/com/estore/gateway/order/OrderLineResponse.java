package com.estore.gateway.order;

import com.estore.gateway.client.MoneyPayload;

import java.util.List;

public record OrderLineResponse(String productName,
                                MoneyPayload unitPrice,
                                int quantity,
                                List<OrderLineAddOnResponse> addOns,
                                MoneyPayload lineTotal) {
}
