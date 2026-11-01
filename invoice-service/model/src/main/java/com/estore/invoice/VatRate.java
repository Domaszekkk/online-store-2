package com.estore.invoice;

import java.math.BigDecimal;
import java.math.RoundingMode;

public enum VatRate {
    VAT_23(new BigDecimal("23")),
    VAT_8(new BigDecimal("8")),
    VAT_5(new BigDecimal("5")),
    VAT_0(BigDecimal.ZERO);

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final BigDecimal percentage;

    VatRate(BigDecimal percentage) {
        this.percentage = percentage;
    }

    public Money netFromGross(Money gross) {
        BigDecimal net = gross.amount()
                .multiply(ONE_HUNDRED)
                .divide(ONE_HUNDRED.add(percentage), 2, RoundingMode.HALF_UP);
        return Money.of(net, gross.currency());
    }
}
