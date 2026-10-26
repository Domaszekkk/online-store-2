package com.estore.product;

import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.Period;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class ProductService {
    private final ProductRepositoryPort productRepositoryPort;
    private final TransactionPort transactionPort;
    private final Clock clock;
    private final Period archiveRetentionPeriod;

    public Product addProduct(AddProductCommand command) {
        Product product = Product.from(command);
        validateAddOns(product.getId(), product.getAddOns());
        return productRepositoryPort.save(product);
    }

    public Product getProductById(UUID id) {
        return productRepositoryPort
                .findActiveById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    public List<Product> getProducts(ProductType type) {
        return productRepositoryPort.findAllActive(type);
    }

    public Product getArchivedProductById(UUID id) {
        return productRepositoryPort
                .findArchivedById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    public List<Product> getArchivedProducts() {
        return productRepositoryPort.findAllArchived();
    }

    public List<ProductOption> getProductConfiguration(UUID id) {
        return getProductById(id).getOptions();
    }

    public Product updateProduct(UUID id, UpdateProductCommand command) {
        Product product = getProductById(id);
        if (command.getAddOns() != null) {
            validateAddOns(id, command.getAddOns());
        }
        product.update(command);
        return productRepositoryPort.save(product);
    }

    public void archiveProduct(UUID id) {
        transactionPort.inTransaction(() -> {
            Product product = getProductById(id);
            product.archive(clock.instant());
            productRepositoryPort.removeAddOnsReferencing(id);
            return productRepositoryPort.save(product);
        });
    }

    public long deleteExpiredArchivedProducts() {
        Instant cutoff = OffsetDateTime.now(clock).minus(archiveRetentionPeriod).toInstant();
        return productRepositoryPort.deleteArchivedBefore(cutoff);
    }

    private void validateAddOns(UUID productId, List<AddOn> addOns) {
        for (AddOn addOn : addOns) {
            UUID addOnProductId = addOn.getProductId();
            if (addOnProductId == null) {
                throw InvalidAddOnException.missingProductId();
            }
            if (addOnProductId.equals(productId)) {
                throw InvalidAddOnException.selfReference(productId);
            }
            if (!productRepositoryPort.existsActiveById(addOnProductId)) {
                throw InvalidAddOnException.productNotFound(addOnProductId);
            }
        }
    }
}
