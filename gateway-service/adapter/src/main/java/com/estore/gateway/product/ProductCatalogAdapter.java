package com.estore.gateway.product;

import com.estore.gateway.ProductCatalogPort;
import com.estore.gateway.ProductDetails;
import com.estore.gateway.ProductSummary;
import com.estore.gateway.ResultPage;
import com.estore.gateway.client.RemoteServiceCalls;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProductCatalogAdapter implements ProductCatalogPort {
    private static final String SERVICE_NAME = "product-service";

    private final ProductClient productClient;
    private final ProductMapper productMapper;
    private final RemoteServiceCalls remoteServiceCalls;

    @Override
    public ResultPage<ProductSummary> findProducts(String type, int page, int size) {
        return productMapper.toSummaryPage(remoteServiceCalls.call(SERVICE_NAME, () -> productClient.getProducts(type, page, size)));
    }

    @Override
    public ProductDetails getProduct(UUID id) {
        return productMapper.toDetails(remoteServiceCalls.call(SERVICE_NAME, () -> productClient.getProduct(id)));
    }
}
