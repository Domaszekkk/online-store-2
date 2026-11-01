package com.estore.cart;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public record CatalogSnapshot(Set<UUID> checkedProductIds, Map<UUID, ProductDetails> products) {

    public boolean covers(UUID productId) {
        return checkedProductIds.contains(productId);
    }

    public Optional<ProductDetails> find(UUID productId) {
        return Optional.ofNullable(products.get(productId));
    }
}
