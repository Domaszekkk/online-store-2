package com.estore.order.order;

import com.estore.order.CartPort;
import com.estore.order.OrderEventPort;
import com.estore.order.OrderRepositoryPort;
import com.estore.order.OrderService;
import com.estore.order.TransactionPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OrderServiceConfig {

    @Bean
    public OrderService orderService(OrderRepositoryPort orderRepositoryPort,
                                     CartPort cartPort,
                                     OrderEventPort orderEventPort,
                                     TransactionPort transactionPort) {
        return new OrderService(orderRepositoryPort, cartPort, orderEventPort, transactionPort);
    }
}
