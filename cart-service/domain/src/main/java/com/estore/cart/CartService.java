package com.estore.cart;

import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
public class CartService {
    private final CartRepositoryPort cartRepositoryPort;
    private final ProductCatalogPort productCatalogPort;

    public Cart createCart() {
        return cartRepositoryPort.save(Cart.createNew());
    }

    public Cart getCartById(UUID id) {
        return cartRepositoryPort
                .findById(id)
                .orElseThrow(() -> new CartNotFoundException(id));
    }

    public Cart addItem(UUID cartId, AddItemCommand command) {
        CartItem.requireValidQuantity(command.getQuantity());
        Cart cart = getCartById(cartId);

        ProductDetails product = productCatalogPort.getProduct(command.getProductId());
        VariantDetails variant = product.findVariant(command.getVariantId())
                .orElseThrow(() -> new VariantNotFoundException(command.getVariantId()));
        if (!variant.isAvailable()) {
            throw new VariantNotAvailableException(command.getVariantId());
        }

        List<CartItemAddOn> addOns = resolveAddOns(product, command.getAddOns());
        CartItem item = CartItem.create(product.getId(), product.getName(), variant, command.getQuantity(), addOns);

        cart.addItem(item);
        return cartRepositoryPort.save(cart);
    }

    public Cart changeItemQuantity(UUID cartId, UUID itemId, ChangeItemQuantityCommand command) {
        Cart cart = getCartById(cartId);
        cart.changeItemQuantity(itemId, command.getQuantity());
        return cartRepositoryPort.save(cart);
    }

    public Cart removeItem(UUID cartId, UUID itemId) {
        Cart cart = getCartById(cartId);
        cart.removeItem(itemId);
        return cartRepositoryPort.save(cart);
    }

    private List<CartItemAddOn> resolveAddOns(ProductDetails product, List<AddOnSelection> selections) {
        if (selections == null || selections.isEmpty()) {
            return new ArrayList<>();
        }
        Map<UUID, Integer> quantityPerAddOn = new LinkedHashMap<>();
        for (AddOnSelection selection : selections) {
            CartItem.requireValidQuantity(selection.getQuantity());
            quantityPerAddOn.merge(selection.getProductId(), selection.getQuantity(), Integer::sum);
        }
        List<CartItemAddOn> addOns = new ArrayList<>();
        quantityPerAddOn.forEach((addOnProductId, quantity) -> {
            AddOnDetails addOn = product.findAddOn(addOnProductId)
                    .orElseThrow(() -> new ProductNotFoundException(addOnProductId));
            addOns.add(CartItemAddOn.builder()
                    .productId(addOn.getProductId())
                    .name(addOn.getName())
                    .price(addOn.getPrice())
                    .quantity(quantity)
                    .build());
        });
        return addOns;
    }
}
