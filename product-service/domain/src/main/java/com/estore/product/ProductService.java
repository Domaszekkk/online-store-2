package com.estore.product;

import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class ProductService {
    private final ProductRepositoryPort productRepositoryPort;
    private final TransactionPort transactionPort;
    private final Clock clock;
    private final Period archiveRetentionPeriod;

    public Product addProduct(AddProductCommand command) {
        Product savedProduct = transactionPort.inTransaction(() -> {
            Product product = Product.from(command);
            validateAddOns(product.getId(), product.getAddOns());
            requireStockKeepingUnitsFree(product);
            return productRepositoryPort.save(product);
        });
        return withAddOnDetails(savedProduct);
    }

    public Product getProductById(UUID id) {
        return withAddOnDetails(findActiveProduct(id));
    }

    public ResultPage<Product> getProducts(ProductType type, PageQuery pageQuery) {
        return withAddOnDetails(productRepositoryPort.findAllActive(type, pageQuery));
    }

    public Product getArchivedProductById(UUID id) {
        return withAddOnDetails(productRepositoryPort
                .findArchivedById(id)
                .orElseThrow(() -> new ProductNotFoundException(id)));
    }

    public ResultPage<Product> getArchivedProducts(PageQuery pageQuery) {
        return withAddOnDetails(productRepositoryPort.findAllArchived(pageQuery));
    }

    public List<ProductOption> getProductConfiguration(UUID id) {
        return findActiveProduct(id).getOptions();
    }

    public Product updateProduct(UUID id, UpdateProductCommand command) {
        Product savedProduct = transactionPort.inTransaction(() -> {
            Product product = getProductForUpdate(id);
            if (command.getAddOns() != null) {
                validateAddOns(id, command.getAddOns());
            }
            if (command.getType() != ProductType.ELECTRONICS && productRepositoryPort.isAddOnOfActiveProduct(id)) {
                throw InvalidProductException.usedAsAddOn(id, command.getType());
            }
            product.update(command);
            requireStockKeepingUnitsFree(product);
            return productRepositoryPort.save(product);
        });
        return withAddOnDetails(savedProduct);
    }

    public void archiveProduct(UUID id) {
        transactionPort.inTransaction(() -> {
            Product product = getProductForUpdate(id);
            product.archive(clock.instant());
            productRepositoryPort.removeAddOnsReferencing(id);
            return productRepositoryPort.save(product);
        });
    }

    public long deleteExpiredArchivedProducts() {
        Instant cutoff = OffsetDateTime.now(clock).minus(archiveRetentionPeriod).toInstant();
        return productRepositoryPort.deleteArchivedBefore(cutoff);
    }

    private Product findActiveProduct(UUID id) {
        return productRepositoryPort
                .findActiveById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    private Product getProductForUpdate(UUID id) {
        return productRepositoryPort
                .findActiveByIdForUpdate(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    private void requireStockKeepingUnitsFree(Product product) {
        Set<String> stockKeepingUnits = product.stockKeepingUnits();
        productRepositoryPort.lockStockKeepingUnits(stockKeepingUnits);
        List<String> takenStockKeepingUnits = productRepositoryPort
                .findStockKeepingUnitsUsedByOtherActiveProducts(stockKeepingUnits, product.getId());
        if (!takenStockKeepingUnits.isEmpty()) {
            throw new StockKeepingUnitTakenException(takenStockKeepingUnits);
        }
    }

    private void validateAddOns(UUID productId, List<AddOn> addOns) {
        Set<UUID> addOnProductIds = new HashSet<>();
        for (AddOn addOn : addOns) {
            UUID addOnProductId = addOn.getProductId();
            if (addOnProductId == null) {
                throw InvalidAddOnException.missingProductId();
            }
            if (addOnProductId.equals(productId)) {
                throw InvalidAddOnException.selfReference(productId);
            }
            if (!addOnProductIds.add(addOnProductId)) {
                throw InvalidAddOnException.duplicate(addOnProductId);
            }
            Product addOnProduct = productRepositoryPort.findActiveById(addOnProductId)
                    .orElseThrow(() -> InvalidAddOnException.productNotFound(addOnProductId));
            if (addOnProduct.getType() != ProductType.ELECTRONICS) {
                throw InvalidAddOnException.notAccessory(addOnProductId, addOnProduct.getType());
            }
        }
    }

    private Product withAddOnDetails(Product product) {
        withAddOnDetails(List.of(product));
        return product;
    }

    private List<Product> withAddOnDetails(List<Product> products) {
        Set<UUID> addOnProductIds = products.stream()
                .flatMap(product -> product.getAddOns().stream())
                .map(AddOn::getProductId)
                .collect(Collectors.toSet());
        if (addOnProductIds.isEmpty()) {
            return products;
        }
        Map<UUID, Product> accessories = productRepositoryPort.findAllActiveByIds(addOnProductIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        products.forEach(product -> product.setAddOns(product.getAddOns().stream()
                .map(addOn -> accessories.get(addOn.getProductId()))
                .filter(Objects::nonNull)
                .map(Product::asAddOn)
                .collect(Collectors.toCollection(ArrayList::new))));
        return products;
    }

    private ResultPage<Product> withAddOnDetails(ResultPage<Product> page) {
        withAddOnDetails(page.items());
        return page;
    }
}
