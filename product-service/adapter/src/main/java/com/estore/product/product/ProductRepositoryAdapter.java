package com.estore.product.product;

import com.estore.product.Product;
import com.estore.product.ProductRepositoryPort;
import com.estore.product.ProductType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProductRepositoryAdapter implements ProductRepositoryPort {
    private final ProductJpaRepository productJpaRepository;
    private final ProductMapper productMapper;

    @Override
    @Transactional
    public Product save(Product product) {
        return productMapper.toModel(productJpaRepository.save(productMapper.toEntity(product)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Product> findActiveById(UUID id) {
        return productJpaRepository.findByIdAndArchivedAtIsNull(id).map(productMapper::toModel);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<Product> findActiveByIdForUpdate(UUID id) {
        return productJpaRepository.findActiveByIdForUpdate(id).map(productMapper::toModel);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> findAllActive(ProductType type) {
        List<ProductEntity> entities = (type == null)
                ? productJpaRepository.findByArchivedAtIsNull()
                : productJpaRepository.findByTypeAndArchivedAtIsNull(type);
        return entities.stream().map(productMapper::toModel).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsActiveById(UUID id) {
        return productJpaRepository.existsByIdAndArchivedAtIsNull(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findStockKeepingUnitsUsedByOtherActiveProducts(Collection<String> stockKeepingUnits,
                                                                       UUID productId) {
        return productJpaRepository.findStockKeepingUnitsUsedByOtherActiveProducts(stockKeepingUnits, productId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Product> findArchivedById(UUID id) {
        return productJpaRepository.findByIdAndArchivedAtIsNotNull(id).map(productMapper::toModel);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> findAllArchived() {
        return productJpaRepository.findByArchivedAtIsNotNull().stream().map(productMapper::toModel).toList();
    }

    @Override
    @Transactional
    public void removeAddOnsReferencing(UUID productId) {
        productJpaRepository.deleteAddOnsReferencing(productId);
    }

    @Override
    @Transactional
    public long deleteArchivedBefore(Instant cutoff) {
        return productJpaRepository.deleteByArchivedAtBefore(cutoff);
    }
}
