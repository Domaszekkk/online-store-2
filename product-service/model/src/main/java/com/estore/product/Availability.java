package com.estore.product;

public enum Availability {
    AVAILABLE,
    SOLD_OUT,
    COMING_SOON;

    public boolean isAvailable() {
        return this == AVAILABLE;
    }
}
