package com.estore.cart.cart;

import com.estore.cart.Cart;
import com.estore.cart.CartRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CartRepositoryAdapter implements CartRepositoryPort {
    private final CartJpaRepository cartJpaRepository;
    private final CartMapper cartMapper;

    @Override
    public Cart save(Cart cart) {
        return cartMapper.toModel(cartJpaRepository.save(cartMapper.toEntity(cart)));
    }

    @Override
    public Optional<Cart> findById(UUID id) {
        return cartJpaRepository.findById(id).map(cartMapper::toModel);
    }
}
