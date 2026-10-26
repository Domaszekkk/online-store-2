package com.estore.product.product;

import com.estore.product.ProductRepositoryPort;
import com.estore.product.ProductService;
import com.estore.product.TransactionPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;
import java.time.Period;

@Configuration
@EnableScheduling
public class ProductServiceConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public ProductService productService(ProductRepositoryPort productRepositoryPort,
                                         TransactionPort transactionPort,
                                         Clock clock,
                                         @Value("${product.archive.retention-period}") Period archiveRetentionPeriod) {
        return new ProductService(productRepositoryPort, transactionPort, clock, archiveRetentionPeriod);
    }
}
