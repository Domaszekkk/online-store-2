package com.estore.order.client;

import java.math.BigDecimal;

public record MoneyPayload(BigDecimal amount, String currency) {
}
