package com.estore.gateway.cart;

import com.estore.gateway.AddItemCommand;
import com.estore.gateway.Cart;
import com.estore.gateway.CartPort;
import com.estore.gateway.client.RemoteServiceCalls;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CartAdapter implements CartPort {
    private static final String SERVICE_NAME = "cart-service";

    private final CartClient cartClient;
    private final CartMapper cartMapper;
    private final RemoteServiceCalls remoteServiceCalls;

    @Override
    public Cart createCart() {
        return cartMapper.toModel(remoteServiceCalls.call(SERVICE_NAME, cartClient::createCart));
    }

    @Override
    public Cart getCart(UUID cartId) {
        return cartMapper.toModel(remoteServiceCalls.call(SERVICE_NAME, () -> cartClient.getCart(cartId)));
    }

    @Override
    public Cart addItem(UUID cartId, AddItemCommand command) {
        AddItemPayload payload = cartMapper.toPayload(command);
        return cartMapper.toModel(remoteServiceCalls.call(SERVICE_NAME, () -> cartClient.addItem(cartId, payload)));
    }

    @Override
    public Cart changeItemQuantity(UUID cartId, UUID itemId, int quantity) {
        ChangeItemQuantityPayload payload = new ChangeItemQuantityPayload(quantity);
        return cartMapper.toModel(remoteServiceCalls.call(SERVICE_NAME,
                () -> cartClient.changeItemQuantity(cartId, itemId, payload)));
    }

    @Override
    public Cart removeItem(UUID cartId, UUID itemId) {
        return cartMapper.toModel(remoteServiceCalls.call(SERVICE_NAME, () -> cartClient.removeItem(cartId, itemId)));
    }
}
