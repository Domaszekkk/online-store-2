package com.estore.order.order;

import com.estore.order.Order;
import com.estore.order.OrderRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OrderRepositoryAdapter implements OrderRepositoryPort {
    private final OrderJpaRepository orderJpaRepository;
    private final OrderMapper orderMapper;

    @Override
    @Transactional
    public Order save(Order order) {
        return orderMapper.toModel(orderJpaRepository.save(orderMapper.toEntity(order)));
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
