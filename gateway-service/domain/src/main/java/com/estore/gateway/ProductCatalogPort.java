package com.estore.gateway;

import java.util.UUID;

public interface ProductCatalogPort {
    ResultPage<ProductSummary> findProducts(String type, int page, int size);

    ProductDetails getProduct(UUID id);
}
