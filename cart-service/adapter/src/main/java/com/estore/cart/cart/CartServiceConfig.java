package com.estore.cart.cart;

import com.estore.cart.CartRepositoryPort;
import com.estore.cart.CartService;
import com.estore.cart.ProductCatalogPort;
import com.estore.cart.TransactionPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;
import java.time.Duration;

@Configuration
@EnableScheduling
public class CartServiceConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public CartService cartService(CartRepositoryPort cartRepositoryPort,
                                   ProductCatalogPort productCatalogPort,
                                   TransactionPort transactionPort,
                                   Clock clock,
                                   @Value("${cart.expiration.inactivity-timeout}") Duration inactivityTimeout,
                                   @Value("${cart.expiration.retention-period}") Duration expiredCartRetention) {
        return new CartService(cartRepositoryPort, productCatalogPort, transactionPort,
                clock, inactivityTimeout, expiredCartRetention);
    }
}
