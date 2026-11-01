package com.estore.product.product;

import com.estore.product.ProductType;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductJpaRepository extends JpaRepository<ProductEntity, UUID> {

    Optional<ProductEntity> findByIdAndArchivedAtIsNull(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT product FROM ProductEntity product WHERE product.id = :id AND product.archivedAt IS NULL")
    Optional<ProductEntity> findActiveByIdForUpdate(@Param("id") UUID id);

    Page<ProductEntity> findByArchivedAtIsNull(Pageable pageable);

    Page<ProductEntity> findByTypeAndArchivedAtIsNull(ProductType type, Pageable pageable);

    List<ProductEntity> findByIdInAndArchivedAtIsNull(Collection<UUID> ids);

    @Query("""
            SELECT COUNT(product) FROM ProductEntity product JOIN product.addOns addOn
            WHERE addOn.productId = :productId
            AND product.archivedAt IS NULL
            """)
    long countActiveProductsWithAddOn(@Param("productId") UUID productId);

    @Query(value = "SELECT COUNT(*) FROM pg_advisory_xact_lock(hashtextextended(:stockKeepingUnit, 0))", nativeQuery = true)
    long lockStockKeepingUnit(@Param("stockKeepingUnit") String stockKeepingUnit);

    @Query("""
            SELECT variant.stockKeepingUnit FROM ProductEntity product JOIN product.variants variant
            WHERE variant.stockKeepingUnit IN :stockKeepingUnits
            AND product.archivedAt IS NULL
            AND product.id <> :productId
            """)
    List<String> findStockKeepingUnitsUsedByOtherActiveProducts(
            @Param("stockKeepingUnits") Collection<String> stockKeepingUnits,
            @Param("productId") UUID productId);

    Optional<ProductEntity> findByIdAndArchivedAtIsNotNull(UUID id);

    Page<ProductEntity> findByArchivedAtIsNotNull(Pageable pageable);

    long deleteByArchivedAtBefore(Instant cutoff);

    @Modifying
    @Query("DELETE FROM AddOnEntity addOn WHERE addOn.productId = :productId")
    void deleteAddOnsReferencing(@Param("productId") UUID productId);
}
