package com.estore.cart.cart;

import com.estore.cart.Cart;
import com.estore.cart.CartRepositoryPort;
import com.estore.cart.CartStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CartRepositoryAdapter implements CartRepositoryPort {
    private final CartJpaRepository cartJpaRepository;
    private final CartMapper cartMapper;

    @Override
    @Transactional
    public Cart save(Cart cart) {
        return cartMapper.toModel(cartJpaRepository.save(cartMapper.toEntity(cart)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Cart> findById(UUID id) {
        return cartJpaRepository.findById(id).map(cartMapper::toModel);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<Cart> findByIdForUpdate(UUID id) {
        return cartJpaRepository.findByIdForUpdate(id).map(cartMapper::toModel);
    }

    @Override
    @Transactional
    public int markExpiredCarts(Instant now) {
        return cartJpaRepository.markExpired(now, CartStatus.ACTIVE, CartStatus.EXPIRED);
    }

    @Override
    @Transactional
    public long deleteExpiredCartsBefore(Instant cutoff) {
        return cartJpaRepository.deleteByStatusAndExpiresAtBefore(CartStatus.EXPIRED, cutoff);
    }
}
