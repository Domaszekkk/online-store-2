package com.estore.product;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepositoryPort {
    Product save(Product product);

    Optional<Product> findActiveById(UUID id);

    Optional<Product> findActiveByIdForUpdate(UUID id);

    List<Product> findAllActive(ProductType type);

    boolean existsActiveById(UUID id);

    List<String> findStockKeepingUnitsUsedByOtherActiveProducts(Collection<String> stockKeepingUnits, UUID productId);

    Optional<Product> findArchivedById(UUID id);

    List<Product> findAllArchived();

    void removeAddOnsReferencing(UUID productId);

    long deleteArchivedBefore(Instant cutoff);
}
