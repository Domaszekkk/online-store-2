package com.estore.gateway.order;

import com.estore.gateway.client.MoneyPayload;

public record OrderLineAddOnResponse(String name, MoneyPayload price, int quantity) {
}
