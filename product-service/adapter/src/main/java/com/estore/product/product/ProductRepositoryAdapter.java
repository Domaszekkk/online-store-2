package com.estore.product.product;

import com.estore.product.Product;
import com.estore.product.ProductRepositoryPort;
import com.estore.product.ProductType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProductRepositoryAdapter implements ProductRepositoryPort {
    private final ProductJpaRepository productJpaRepository;
    private final ProductMapper productMapper;

    @Override
    public Product save(Product product) {
        return productMapper.toModel(productJpaRepository.save(productMapper.toEntity(product)));
    }

    @Override
    public Optional<Product> findById(UUID id) {
        return productJpaRepository.findById(id).map(productMapper::toModel);
    }

    @Override
    public List<Product> findAll(ProductType type) {
        List<ProductEntity> entities = (type == null)
                ? productJpaRepository.findAll()
                : productJpaRepository.findByType(type);
        return entities.stream().map(productMapper::toModel).toList();
    }

    @Override
    public boolean existsById(UUID id) {
        return productJpaRepository.existsById(id);
    }

    @Override
    public void deleteById(UUID id) {
        productJpaRepository.deleteById(id);
    }
}
