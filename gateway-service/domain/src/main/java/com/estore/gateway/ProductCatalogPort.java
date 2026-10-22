package com.estore.gateway;

import java.util.List;
import java.util.UUID;

public interface ProductCatalogPort {
    List<ProductSummary> findProducts(String type);

    ProductDetails getProduct(UUID id);
}
