package com.estore.order.order;

import com.estore.order.Order;
import com.estore.order.OrderAlreadyPlacedException;
import com.estore.order.OrderRepositoryPort;
import com.estore.order.PageQuery;
import com.estore.order.ResultPage;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<Order> findByIdForUpdate(UUID id) {
        return orderJpaRepository.findByIdForUpdate(id).map(orderMapper::toModel);
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
    public ResultPage<Order> findAll(PageQuery pageQuery) {
        return toResultPage(orderJpaRepository.findAll(newestFirst(pageQuery)), pageQuery);
    }

    @Override
    @Transactional(readOnly = true)
    public ResultPage<Order> findAllByCustomerEmail(String email, PageQuery pageQuery) {
        return toResultPage(orderJpaRepository.findAllByCustomerEmailIgnoreCase(email, newestFirst(pageQuery)), pageQuery);
    }

    private PageRequest newestFirst(PageQuery pageQuery) {
        return PageRequest.of(pageQuery.page(), pageQuery.size(),
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("id")));
    }

    private ResultPage<Order> toResultPage(Page<OrderEntity> entities, PageQuery pageQuery) {
        return new ResultPage<>(entities.stream().map(orderMapper::toModel).toList(),
                pageQuery.page(), pageQuery.size(), entities.getTotalElements());
    }

    @Override
    @Transactional
    public long nextOrderSequenceNumber() {
        return orderJpaRepository.nextOrderSequenceNumber();
    }
}
