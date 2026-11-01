package com.estore.cart;

import java.util.Optional;
import java.util.UUID;

public interface ProductCatalogPort {
    ProductDetails getProduct(UUID productId);

    Optional<ProductDetails> findProduct(UUID productId);
}
