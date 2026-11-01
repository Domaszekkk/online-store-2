package com.estore.gateway.product;

import com.estore.gateway.client.RemoteServiceClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(name = "product-service", url = "${product.service.url}", configuration = RemoteServiceClientConfiguration.class)
public interface ProductClient {

    @GetMapping("/products")
    ProductPageResponse getProducts(@RequestParam(value = "type", required = false) String type,
                                    @RequestParam("page") int page,
                                    @RequestParam("size") int size);

    @GetMapping("/products/{id}")
    ProductResponse getProduct(@PathVariable("id") UUID id);
}
