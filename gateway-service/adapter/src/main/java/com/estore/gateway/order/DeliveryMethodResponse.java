package com.estore.gateway.order;

import com.estore.gateway.client.MoneyPayload;

public record DeliveryMethodResponse(String method, MoneyPayload cost) {
}
