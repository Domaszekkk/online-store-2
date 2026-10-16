package com.estore.order;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class OrderService {
    private final OrderRepositoryPort orderRepositoryPort;
    private final CartCheckoutPort cartCheckoutPort;

    public Order placeOrder(PlaceOrderCommand command) {
        CartDetails cart = cartCheckoutPort.checkout(command.getCartId());
        Order order = Order.place(command, cart, orderRepositoryPort.nextOrderSequenceNumber());
        return orderRepositoryPort.save(order);
    }

    public Order getOrderById(UUID id) {
        return orderRepositoryPort
                .findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    public List<Order> getOrders(String customerEmail) {
        if (customerEmail == null || customerEmail.isBlank()) {
            return orderRepositoryPort.findAll();
        }
        return orderRepositoryPort.findAllByCustomerEmail(customerEmail.trim());
    }
}
