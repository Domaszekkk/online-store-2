package com.estore.cart;

import java.util.function.Supplier;

public interface TransactionPort {
    <T> T inTransaction(Supplier<T> work);
}
