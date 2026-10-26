package com.estore.order.event;

import java.math.BigDecimal;

public record MoneyPayload(BigDecimal amount, String currency) {
}
