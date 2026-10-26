package com.estore.cart.client;

import java.math.BigDecimal;

public record MoneyPayload(BigDecimal amount, String currency) {
}
