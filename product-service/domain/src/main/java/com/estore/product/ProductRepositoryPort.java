package com.estore.product;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepositoryPort {
    Product save(Product product);

    Optional<Product> findActiveById(UUID id);

    List<Product> findAllActive(ProductType type);

    boolean existsActiveById(UUID id);

    Optional<Product> findArchivedById(UUID id);

    List<Product> findAllArchived();

    void removeAddOnsReferencing(UUID productId);

    long deleteArchivedBefore(Instant cutoff);
}
