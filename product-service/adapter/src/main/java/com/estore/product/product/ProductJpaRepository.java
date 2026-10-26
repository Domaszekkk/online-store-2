package com.estore.product.product;

import com.estore.product.ProductType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductJpaRepository extends JpaRepository<ProductEntity, UUID> {

    Optional<ProductEntity> findByIdAndArchivedAtIsNull(UUID id);

    List<ProductEntity> findByArchivedAtIsNull();

    List<ProductEntity> findByTypeAndArchivedAtIsNull(ProductType type);

    boolean existsByIdAndArchivedAtIsNull(UUID id);

    Optional<ProductEntity> findByIdAndArchivedAtIsNotNull(UUID id);

    List<ProductEntity> findByArchivedAtIsNotNull();

    long deleteByArchivedAtBefore(Instant cutoff);

    @Modifying
    @Query("DELETE FROM AddOnEntity addOn WHERE addOn.productId = :productId")
    void deleteAddOnsReferencing(@Param("productId") UUID productId);
}
