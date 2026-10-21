package com.estore.order.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderJpaRepository extends JpaRepository<OrderEntity, UUID> {

    Optional<OrderEntity> findByCartId(UUID cartId);

    List<OrderEntity> findAllByOrderByCreatedAtDesc();

    List<OrderEntity> findAllByCustomerEmailIgnoreCaseOrderByCreatedAtDesc(String email);

    @Query(value = "SELECT nextval('order_number_seq')", nativeQuery = true)
    long nextOrderSequenceNumber();
}
