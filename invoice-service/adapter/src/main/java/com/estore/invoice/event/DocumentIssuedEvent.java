package com.estore.invoice.event;

import java.util.UUID;

public record DocumentIssuedEvent(UUID orderId, String documentNumber, String documentType) {
    public static final String TYPE = "DocumentIssued";
}
