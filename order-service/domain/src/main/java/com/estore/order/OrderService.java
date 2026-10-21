package com.estore.order;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class OrderService {
    private final OrderRepositoryPort orderRepositoryPort;
    private final CartCheckoutPort cartCheckoutPort;
    private final OrderEventPort orderEventPort;
    private final TransactionPort transactionPort;

    public Order placeOrder(PlaceOrderCommand command) {
        if (command.getBillingAddress() != null) {
            command.getBillingAddress().requireValid();
        }
        orderRepositoryPort.findByCartId(command.getCartId()).ifPresent(existing -> {
            throw new OrderAlreadyPlacedException(command.getCartId(), existing.getOrderNumber());
        });
        CartDetails cart = cartCheckoutPort.checkout(command.getCartId());
        return transactionPort.inTransaction(() -> {
            Order order = Order.place(command, cart, orderRepositoryPort.nextOrderSequenceNumber());
            Order savedOrder = orderRepositoryPort.save(order);
            orderEventPort.orderPlaced(savedOrder);
            return savedOrder;
        });
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
