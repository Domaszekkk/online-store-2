package com.estore.gateway;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ShopServiceConfig {

    @Bean
    public ShopService shopService(ProductCatalogPort productCatalogPort,
                                   CartPort cartPort,
                                   OrderPort orderPort,
                                   DocumentPort documentPort) {
        return new ShopService(productCatalogPort, cartPort, orderPort, documentPort);
    }
}
