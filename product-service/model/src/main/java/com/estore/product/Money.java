package com.estore.product;

import java.math.BigDecimal;

public record Money(BigDecimal amount, String currency) {

    public static Money of(BigDecimal amount, String currency) {
        return new Money(amount, currency);
    }
}
