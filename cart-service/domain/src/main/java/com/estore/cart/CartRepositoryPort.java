package com.estore.cart;

import java.util.Optional;
import java.util.UUID;

public interface CartRepositoryPort {
    Cart save(Cart cart);

    Optional<Cart> findById(UUID id);
}
