package com.estore.gateway;

import java.util.UUID;

public interface CartPort {
    Cart createCart();

    Cart getCart(UUID cartId);

    Cart addItem(UUID cartId, AddItemCommand command);

    Cart changeItemQuantity(UUID cartId, UUID itemId, int quantity);

    Cart removeItem(UUID cartId, UUID itemId);
}
