package com.estore.order;

import java.util.UUID;

public interface CartPort {
    CartDetails checkout(UUID cartId, long expectedVersion);

    CartDetails refreshCart(UUID cartId);
}
