package com.estore.gateway;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI gatewayServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Shop Gateway API")
                        .description("Backend for frontend: the single entry point of the shop frontend")
                        .version("1.0"));
    }
}
