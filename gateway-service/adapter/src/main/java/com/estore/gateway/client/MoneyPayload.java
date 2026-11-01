package com.estore.gateway.client;

import java.math.BigDecimal;

public record MoneyPayload(BigDecimal amount, String currency) {
}
