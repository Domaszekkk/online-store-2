package com.estore.product;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class ProductService {
    private final ProductRepositoryPort productRepositoryPort;

    public Product addProduct(AddProductCommand command) {
        Product product = Product.from(command);
        validateAddOns(product.getId(), product.getAddOns());
        return productRepositoryPort.save(product);
    }

    public Product getProductById(UUID id) {
        return productRepositoryPort
                .findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    public List<Product> getProducts(ProductType type) {
        return productRepositoryPort.findAll(type);
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

    public void deleteProduct(UUID id) {
        if (!productRepositoryPort.existsById(id)) {
            throw new ProductNotFoundException(id);
        }
        productRepositoryPort.deleteById(id);
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
            if (!productRepositoryPort.existsById(addOnProductId)) {
                throw InvalidAddOnException.productNotFound(addOnProductId);
            }
        }
    }
}
