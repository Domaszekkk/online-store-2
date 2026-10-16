package com.estore.order;

import java.math.BigDecimal;

public enum DeliveryMethod {
    COURIER(new BigDecimal("19.99")),
    PARCEL_LOCKER(new BigDecimal("12.99")),
    PICKUP(new BigDecimal("0.00"));

    private final BigDecimal costInPolishZloty;

    DeliveryMethod(BigDecimal costInPolishZloty) {
        this.costInPolishZloty = costInPolishZloty;
    }

    public Money cost() {
        return Money.of(costInPolishZloty, Order.CURRENCY);
    }
}
