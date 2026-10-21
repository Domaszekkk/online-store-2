package com.estore.cart;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI cartServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Cart Service API")
                        .description("REST API for managing shopping carts")
                        .version("1.0"));
    }
}
