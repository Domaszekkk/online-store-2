package com.estore.order;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepositoryPort {
    Order save(Order order);

    Optional<Order> findById(UUID id);

    Optional<Order> findByIdForUpdate(UUID id);

    Optional<Order> findByCartId(UUID cartId);

    ResultPage<Order> findAll(PageQuery pageQuery);

    ResultPage<Order> findAllByCustomerEmail(String email, PageQuery pageQuery);

    long nextOrderSequenceNumber();
}
