package com.estore.order;

import java.util.UUID;

public interface CartPort {
    CartDetails checkout(UUID cartId);

    CartDetails getCart(UUID cartId);
}
