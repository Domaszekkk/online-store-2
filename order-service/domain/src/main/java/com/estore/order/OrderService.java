package com.estore.order;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class OrderService {
    private final OrderRepositoryPort orderRepositoryPort;
    private final CartPort cartPort;
    private final OrderEventPort orderEventPort;
    private final TransactionPort transactionPort;

    public Order previewOrder(PlaceOrderCommand command) {
        requireOrderCanBePlaced(command);
        return Order.preview(command, cartPort.refreshCart(command.getCartId()));
    }

    public Order placeOrder(PlaceOrderCommand command) {
        requireOrderCanBePlaced(command);
        CartDetails cart = cartPort.checkout(command.getCartId(), command.getCartVersion());
        return transactionPort.inTransaction(() -> {
            Order order = Order.place(command, cart, orderRepositoryPort.nextOrderSequenceNumber());
            Order savedOrder = orderRepositoryPort.save(order);
            orderEventPort.orderPlaced(savedOrder);
            return savedOrder;
        });
    }

    public Order confirmOrder(UUID orderId, String documentNumber) {
        return transactionPort.inTransaction(() -> {
            Order order = getOrderForUpdate(orderId);
            order.confirm(documentNumber);
            return orderRepositoryPort.save(order);
        });
    }

    public Order changeOrderStatus(UUID orderId, OrderStatus targetStatus) {
        return transactionPort.inTransaction(() -> {
            Order order = getOrderForUpdate(orderId);
            order.changeStatus(targetStatus);
            return orderRepositoryPort.save(order);
        });
    }

    public Order getOrderById(UUID id) {
        return orderRepositoryPort
                .findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    public ResultPage<Order> getOrders(String customerEmail, PageQuery pageQuery) {
        if (customerEmail == null || customerEmail.isBlank()) {
            return orderRepositoryPort.findAll(pageQuery);
        }
        return orderRepositoryPort.findAllByCustomerEmail(customerEmail.trim(), pageQuery);
    }

    public List<DeliveryMethod> getDeliveryMethods() {
        return List.of(DeliveryMethod.values());
    }

    public List<PaymentMethod> getPaymentMethods() {
        return List.of(PaymentMethod.values());
    }

    private void requireOrderCanBePlaced(PlaceOrderCommand command) {
        if (command.getBillingAddress() != null) {
            command.getBillingAddress().requireValid();
        }
        orderRepositoryPort.findByCartId(command.getCartId()).ifPresent(existing -> {
            throw new OrderAlreadyPlacedException(command.getCartId(), existing.getOrderNumber());
        });
    }

    private Order getOrderForUpdate(UUID orderId) {
        return orderRepositoryPort
                .findByIdForUpdate(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }
}
