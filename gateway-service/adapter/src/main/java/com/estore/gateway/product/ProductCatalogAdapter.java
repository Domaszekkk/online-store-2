package com.estore.gateway.product;

import com.estore.gateway.ProductCatalogPort;
import com.estore.gateway.ProductDetails;
import com.estore.gateway.ProductSummary;
import com.estore.gateway.client.RemoteServiceCalls;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProductCatalogAdapter implements ProductCatalogPort {
    private static final String SERVICE_NAME = "product-service";

    private final ProductClient productClient;
    private final ProductMapper productMapper;
    private final RemoteServiceCalls remoteServiceCalls;

    @Override
    public List<ProductSummary> findProducts(String type) {
        return productMapper.toSummaries(remoteServiceCalls.call(SERVICE_NAME, () -> productClient.getProducts(type)));
    }

    @Override
    public ProductDetails getProduct(UUID id) {
        return productMapper.toDetails(remoteServiceCalls.call(SERVICE_NAME, () -> productClient.getProduct(id)));
    }
}
