package com.estore.gateway.client;

import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.springframework.cloud.openfeign.clientconfig.HttpClient5FeignConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HttpClientConfig {

    @Bean
    public HttpClient5FeignConfiguration.HttpClientBuilderCustomizer disableHttpClientRetries() {
        return HttpClientBuilder::disableAutomaticRetries;
    }
}
