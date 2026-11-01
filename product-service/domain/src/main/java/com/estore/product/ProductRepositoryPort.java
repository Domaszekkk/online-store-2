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

    ResultPage<Product> findAllActive(ProductType type, PageQuery pageQuery);

    List<Product> findAllActiveByIds(Collection<UUID> ids);

    boolean isAddOnOfActiveProduct(UUID productId);

    void lockStockKeepingUnits(Collection<String> stockKeepingUnits);

    List<String> findStockKeepingUnitsUsedByOtherActiveProducts(Collection<String> stockKeepingUnits, UUID productId);

    Optional<Product> findArchivedById(UUID id);

    ResultPage<Product> findAllArchived(PageQuery pageQuery);

    void removeAddOnsReferencing(UUID productId);

    long deleteArchivedBefore(Instant cutoff);
}
