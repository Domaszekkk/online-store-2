package com.estore.cart.client;

import feign.Retryer;
import feign.codec.ErrorDecoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;


public class ProductClientConfiguration {

    @Bean
    public Retryer productClientRetryer(
            @Value("${product.service.retry.period-millis:100}") long periodMillis,
            @Value("${product.service.retry.max-period-millis:1000}") long maxPeriodMillis,
            @Value("${product.service.retry.max-attempts:3}") int maxAttempts) {
        return new Retryer.Default(periodMillis, maxPeriodMillis, maxAttempts);
    }

    @Bean
    public ErrorDecoder productClientErrorDecoder() {
        return new RetryOnServerErrorDecoder();
    }
}
