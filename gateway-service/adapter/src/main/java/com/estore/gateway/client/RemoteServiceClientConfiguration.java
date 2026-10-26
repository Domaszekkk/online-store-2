package com.estore.gateway.client;

import feign.Retryer;
import feign.codec.ErrorDecoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public class RemoteServiceClientConfiguration {

    @Bean
    public Retryer remoteServiceRetryer(
            @Value("${gateway.client.retry.period-millis:100}") long periodMillis,
            @Value("${gateway.client.retry.max-period-millis:1000}") long maxPeriodMillis,
            @Value("${gateway.client.retry.max-attempts:3}") int maxAttempts) {
        return new GetOnlyRetryer(new Retryer.Default(periodMillis, maxPeriodMillis, maxAttempts));
    }

    @Bean
    public ErrorDecoder remoteServiceErrorDecoder() {
        return new RetryOnServerErrorDecoder();
    }
}
