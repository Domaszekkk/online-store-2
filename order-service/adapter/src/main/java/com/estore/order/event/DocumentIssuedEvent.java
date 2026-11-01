package com.estore.order.event;

import java.util.UUID;

public record DocumentIssuedEvent(UUID orderId, String documentNumber) {
    public static final String TYPE = "DocumentIssued";
}
