package com.estore.order.order;

import com.estore.order.CartCheckoutPort;
import com.estore.order.OrderRepositoryPort;
import com.estore.order.OrderService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OrderServiceConfig {

    @Bean
    public OrderService orderService(OrderRepositoryPort orderRepositoryPort, CartCheckoutPort cartCheckoutPort) {
        return new OrderService(orderRepositoryPort, cartCheckoutPort);
    }
}
