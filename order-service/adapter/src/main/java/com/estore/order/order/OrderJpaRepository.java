package com.estore.order.order;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderJpaRepository extends JpaRepository<OrderEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT orderEntity FROM OrderEntity orderEntity WHERE orderEntity.id = :id")
    Optional<OrderEntity> findByIdForUpdate(@Param("id") UUID id);

    Optional<OrderEntity> findByCartId(UUID cartId);

    List<OrderEntity> findAllByOrderByCreatedAtDesc();

    List<OrderEntity> findAllByCustomerEmailIgnoreCaseOrderByCreatedAtDesc(String email);

    @Query(value = "SELECT nextval('order_number_seq')", nativeQuery = true)
    long nextOrderSequenceNumber();
}
