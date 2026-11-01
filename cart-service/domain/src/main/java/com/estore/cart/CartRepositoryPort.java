package com.estore.cart;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface CartRepositoryPort {
    Cart save(Cart cart);

    Optional<Cart> findById(UUID id);

    Optional<Cart> findByIdForUpdate(UUID id);

    int markExpiredCarts(Instant now);

    long deleteExpiredCartsBefore(Instant cutoff);
}
