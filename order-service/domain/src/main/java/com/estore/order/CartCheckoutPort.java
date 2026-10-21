package com.estore.order;

import java.util.UUID;

public interface CartCheckoutPort {
    CartDetails checkout(UUID cartId);
}
