package com.estore.invoice.event;

import java.util.List;

public record OrderLinePayload(String productName,
                               String stockKeepingUnit,
                               MoneyPayload unitPrice,
                               String vatRate,
                               int quantity,
                               List<AddOnPayload> addOns) {
}
