package com.estore.cart.cart;

import com.estore.cart.CartRepositoryPort;
import com.estore.cart.CartService;
import com.estore.cart.ProductCatalogPort;
import com.estore.cart.TransactionPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CartServiceConfig {

    @Bean
    public CartService cartService(CartRepositoryPort cartRepositoryPort,
                                   ProductCatalogPort productCatalogPort,
                                   TransactionPort transactionPort) {
        return new CartService(cartRepositoryPort, productCatalogPort, transactionPort);
    }
}
