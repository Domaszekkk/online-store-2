package com.estore.product;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepositoryPort {
    Product save(Product product);

    Optional<Product> findById(UUID id);

    List<Product> findAll(ProductType type);

    boolean existsById(UUID id);

    void deleteById(UUID id);
}
