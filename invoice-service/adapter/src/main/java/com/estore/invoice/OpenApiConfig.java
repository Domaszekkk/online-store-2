package com.estore.invoice;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI invoiceServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Invoice Service API")
                        .description("REST API for invoices and purchase confirmations issued from placed orders")
                        .version("1.0"));
    }
}
