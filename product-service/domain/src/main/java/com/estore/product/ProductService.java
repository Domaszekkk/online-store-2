package com.estore.product;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class ProductService {
    private final ProductRepositoryPort productRepositoryPort;

    public Product addProduct(AddProductCommand command) {
        Product product = Product.from(command);
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

    public Product updateProduct(UUID id, UpdateProductRequest request) {
        Product product = getProductById(id);
        product.update(request);
        return productRepositoryPort.save(product);
    }

    public void deleteProduct(UUID id) {
        if (!productRepositoryPort.existsById(id)) {
            throw new ProductNotFoundException(id);
        }
        productRepositoryPort.deleteById(id);
    }
}
