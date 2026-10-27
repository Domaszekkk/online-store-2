package com.estore.order;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepositoryPort {
    Order save(Order order);

    Optional<Order> findById(UUID id);

    Optional<Order> findByIdForUpdate(UUID id);

    Optional<Order> findByCartId(UUID cartId);

    List<Order> findAll();

    List<Order> findAllByCustomerEmail(String email);

    long nextOrderSequenceNumber();
}
