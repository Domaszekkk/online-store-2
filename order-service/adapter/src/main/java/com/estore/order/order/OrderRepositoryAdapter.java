package com.estore.order.order;

import com.estore.order.Order;
import com.estore.order.OrderAlreadyPlacedException;
import com.estore.order.OrderRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OrderRepositoryAdapter implements OrderRepositoryPort {
    private static final String UNIQUE_CART_ID_CONSTRAINT = "uq_orders_cart_id";

    private final OrderJpaRepository orderJpaRepository;
    private final OrderMapper orderMapper;

    @Override
    @Transactional
    public Order save(Order order) {
        try {
            return orderMapper.toModel(orderJpaRepository.saveAndFlush(orderMapper.toEntity(order)));
        } catch (DataIntegrityViolationException exception) {
            String reason = exception.getMostSpecificCause().getMessage();
            if (reason != null && reason.contains(UNIQUE_CART_ID_CONSTRAINT)) {
                throw new OrderAlreadyPlacedException(order.getCartId(), null);
            }
            throw exception;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Order> findByCartId(UUID cartId) {
        return orderJpaRepository.findByCartId(cartId).map(orderMapper::toModel);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Order> findById(UUID id) {
        return orderJpaRepository.findById(id).map(orderMapper::toModel);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> findAll() {
        return orderJpaRepository.findAllByOrderByCreatedAtDesc().stream().map(orderMapper::toModel).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> findAllByCustomerEmail(String email) {
        return orderJpaRepository.findAllByCustomerEmailIgnoreCaseOrderByCreatedAtDesc(email).stream()
                .map(orderMapper::toModel)
                .toList();
    }

    @Override
    @Transactional
    public long nextOrderSequenceNumber() {
        return orderJpaRepository.nextOrderSequenceNumber();
    }
}
