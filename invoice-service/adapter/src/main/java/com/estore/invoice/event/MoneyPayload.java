package com.estore.invoice.event;

import java.math.BigDecimal;

public record MoneyPayload(BigDecimal amount, String currency) {
}
