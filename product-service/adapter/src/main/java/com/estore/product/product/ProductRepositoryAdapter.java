package com.estore.product.product;

import com.estore.product.PageQuery;
import com.estore.product.Product;
import com.estore.product.ProductRepositoryPort;
import com.estore.product.ProductType;
import com.estore.product.ResultPage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
    public ResultPage<Product> findAllActive(ProductType type, PageQuery pageQuery) {
        PageRequest pageRequest = PageRequest.of(pageQuery.page(), pageQuery.size(), Sort.by("name", "id"));
        Page<ProductEntity> entities = (type == null)
                ? productJpaRepository.findByArchivedAtIsNull(pageRequest)
                : productJpaRepository.findByTypeAndArchivedAtIsNull(type, pageRequest);
        return toResultPage(entities, pageQuery);
    }

    private ResultPage<Product> toResultPage(Page<ProductEntity> entities, PageQuery pageQuery) {
        return new ResultPage<>(entities.stream().map(productMapper::toModel).toList(),
                pageQuery.page(), pageQuery.size(), entities.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> findAllActiveByIds(Collection<UUID> ids) {
        return productJpaRepository.findByIdInAndArchivedAtIsNull(ids).stream().map(productMapper::toModel).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isAddOnOfActiveProduct(UUID productId) {
        return productJpaRepository.countActiveProductsWithAddOn(productId) > 0;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void lockStockKeepingUnits(Collection<String> stockKeepingUnits) {
        stockKeepingUnits.stream()
                .sorted()
                .forEach(productJpaRepository::lockStockKeepingUnit);
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
    public ResultPage<Product> findAllArchived(PageQuery pageQuery) {
        PageRequest pageRequest = PageRequest.of(pageQuery.page(), pageQuery.size(),
                Sort.by(Sort.Order.desc("archivedAt"), Sort.Order.asc("id")));
        return toResultPage(productJpaRepository.findByArchivedAtIsNotNull(pageRequest), pageQuery);
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
