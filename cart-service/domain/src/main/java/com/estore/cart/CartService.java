package com.estore.cart;

import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@RequiredArgsConstructor
public class CartService {
    private final CartRepositoryPort cartRepositoryPort;
    private final ProductCatalogPort productCatalogPort;
    private final TransactionPort transactionPort;

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
        getCartById(cartId);

        ProductDetails product = productCatalogPort.getProduct(command.getProductId());
        VariantDetails variant = product.findVariant(command.getVariantId())
                .orElseThrow(() -> new VariantNotFoundException(command.getVariantId()));
        if (!variant.isAvailable()) {
            throw new VariantNotAvailableException(command.getVariantId());
        }
        List<CartItemAddOn> addOns = resolveAddOns(product, command.getAddOns());

        return modifyCart(cartId, cart -> cart.addItem(
                CartItem.create(product.getId(), product.getName(), variant, command.getQuantity(), addOns)));
    }

    public Cart changeItemQuantity(UUID cartId, UUID itemId, ChangeItemQuantityCommand command) {
        return modifyCart(cartId, cart -> cart.changeItemQuantity(itemId, command.getQuantity()));
    }

    public Cart removeItem(UUID cartId, UUID itemId) {
        return modifyCart(cartId, cart -> cart.removeItem(itemId));
    }

    public Cart checkout(UUID cartId) {
        return transactionPort.inTransaction(() -> {
            Cart cart = getCartForUpdate(cartId);
            if (cart.isCheckedOut()) {
                return cart;
            }
            cart.checkout();
            return cartRepositoryPort.save(cart);
        });
    }

    private Cart modifyCart(UUID cartId, Consumer<Cart> modification) {
        return transactionPort.inTransaction(() -> {
            Cart cart = getCartForUpdate(cartId);
            modification.accept(cart);
            return cartRepositoryPort.save(cart);
        });
    }

    private Cart getCartForUpdate(UUID cartId) {
        return cartRepositoryPort
                .findByIdForUpdate(cartId)
                .orElseThrow(() -> new CartNotFoundException(cartId));
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
            CartItem.requireValidQuantity(quantity);
            AddOnDetails addOn = product.findAddOn(addOnProductId)
                    .orElseThrow(() -> new ProductNotFoundException(addOnProductId));
            addOns.add(CartItemAddOn.builder()
                    .productId(addOn.getProductId())
                    .name(addOn.getName())
                    .price(addOn.getPrice())
                    .vatRate(addOn.getVatRate())
                    .quantity(quantity)
                    .build());
        });
        return addOns;
    }
}
