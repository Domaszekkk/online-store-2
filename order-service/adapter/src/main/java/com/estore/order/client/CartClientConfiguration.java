package com.estore.order.client;

import feign.Retryer;
import feign.codec.ErrorDecoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;


public class CartClientConfiguration {

    @Bean
    public Retryer cartClientRetryer(
            @Value("${cart.service.retry.period-millis:100}") long periodMillis,
            @Value("${cart.service.retry.max-period-millis:1000}") long maxPeriodMillis,
            @Value("${cart.service.retry.max-attempts:3}") int maxAttempts) {
        return new Retryer.Default(periodMillis, maxPeriodMillis, maxAttempts);
    }

    @Bean
    public ErrorDecoder cartClientErrorDecoder() {
        return new RetryOnServerErrorDecoder();
    }
}
